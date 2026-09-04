package com.reggie.module.platform.service.impl;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.platform.adapter.PlatformAdapter;
import com.reggie.module.platform.adapter.PlatformOrder;
import com.reggie.module.platform.mapper.PlatformSyncLogMapper;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.model.PlatformSyncLog;
import com.reggie.module.platform.service.PlatformOrderPersistService;
import com.reggie.module.platform.service.PlatformSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 外卖平台同步服务实现
 * <p>
 * 通过 Spring 自动注入所有 PlatformAdapter 实现，根据平台类型分发调用。
 * 支持重试机制和异常兜底，网络异常时自动重试最多 3 次。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Service
public class PlatformSyncServiceImpl implements PlatformSyncService {

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 3;
    /** 重试基础延迟（毫秒） */
    private static final long RETRY_BASE_DELAY_MS = 1000L;

    private final List<PlatformAdapter> adapters;
    private final PlatformOrderPersistService persistService;
    private final PlatformSyncLogMapper syncLogMapper;

    @Autowired
    public PlatformSyncServiceImpl(List<PlatformAdapter> adapters,
                                  PlatformOrderPersistService persistService,
                                  PlatformSyncLogMapper syncLogMapper) {
        this.adapters = adapters;
        this.persistService = persistService;
        this.syncLogMapper = syncLogMapper;
    }

    private PlatformAdapter getAdapter(String platformType) {
        return adapters.stream()
                .filter(a -> a.platformType().equals(platformType))
                .findFirst()
                .orElse(null);
    }

    /**
     * 带重试的执行逻辑
     *
     * @param platformType 平台类型
     * @param action       动作描述
     * @param executable   执行逻辑
     * @param <T>          返回值类型
     * @return 执行结果
     */
    private <T> T executeWithRetry(String platformType, String action, Retryable<T> executable) {
        int retryCount = 0;
        long delayMs = RETRY_BASE_DELAY_MS;
        while (retryCount <= MAX_RETRY_COUNT) {
            try {
                return executable.execute();
            } catch (Exception e) {
                retryCount++;
                if (retryCount > MAX_RETRY_COUNT) {
                    log.error("[平台重试] 已达最大重试次数，放弃: platformType={}, action={}, error={}",
                            platformType, action, e.getMessage());
                    throw new CustomException("平台同步执行失败: " + e.getMessage());
                }
                log.warn("[平台重试] 第{}次重试: platformType={}, action={}", retryCount, platformType, action, e);
                sleep(delayMs);
                delayMs *= 2; // 指数退避
            }
        }
        throw new CustomException("平台同步执行失败");
    }

    private void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    private interface Retryable<T> {
        T execute() throws Exception;
    }

    @Override
    public List<PlatformOrder> pullOrders(PlatformConfig config, String beginTime, String endTime) {
        return executeWithRetry(config.getPlatformType(), "PULL", () -> {
            PlatformAdapter adapter = getAdapter(config.getPlatformType());
            if (adapter == null) {
                log.warn("[平台同步] 未找到适配器，跳过: platformType={}", config.getPlatformType());
                return java.util.Collections.emptyList();
            }
            log.info("[平台同步] 开始拉单: platformType={}, shopId={}", config.getPlatformType(), config.getShopId());
            List<PlatformOrder> orders = adapter.pullOrders(config, beginTime, endTime);
            log.info("[平台同步] 拉单完成: platformType={}, count={}", config.getPlatformType(), orders.size());

            // 拉单入库（幂等去重），tenant 以当前上下文为准，失败时不影响返回拉取结果
            Long tenantId = BaseContext.getCurrentTenantId();
            try {
                int inserted = persistService.persistOrders(
                        config.getPlatformType(), config.getShopId(), tenantId, orders);
                log.info("[平台同步] 拉单落库完成: platformType={}, 拉取={}, 新增={}",
                        config.getPlatformType(), orders.size(), inserted);
                // 记录成功日志，便于对账与排查
                saveSyncLog(config, tenantId, "PULL", "IN",
                        "count=" + orders.size() + ",inserted=" + inserted, null);
            } catch (Exception e) {
                log.error("[平台同步] 拉单落库失败(已捕获，不影响拉取): platformType={}", config.getPlatformType(), e);
                // 落库失败必须记 status=1 失败日志：否则对账表"全部成功"掩盖丢单，
                // PlatformRetryTask 也无法按失败日志重试补拉
                saveSyncLog(config, tenantId, "PULL", "IN",
                        "count=" + orders.size(), "落库失败: " + e.getMessage());
            }
            return orders;
        });
    }

    @Override
    public int persistOrders(PlatformConfig config, List<PlatformOrder> orders) {
        Long tenantId = BaseContext.getCurrentTenantId();
        return persistService.persistOrders(
                config.getPlatformType(), config.getShopId(), tenantId, orders);
    }

    /**
     * 记录平台同步日志（拉单/回传统一入口）
     *
     * @param action      同步动作（PULL / PUSH_STATUS:accept 等）
     * @param direction   同步方向：IN=拉单，OUT=状态回传/上架/库存回传
     * @param requestBody 请求摘要（如 count=10,inserted=8 或 orderId=xxx,action=accept）
     * @param error       失败原因，null 表示成功（status=0），非 null 记 status=1
     */
    private void saveSyncLog(PlatformConfig config, Long tenantId, String action, String direction,
                             String requestBody, String error) {
        try {
            PlatformSyncLog logEntity = new PlatformSyncLog();
            logEntity.setTenantId(tenantId);
            logEntity.setPlatformType(config.getPlatformType());
            logEntity.setAction(action);
            logEntity.setDirection(direction);
            logEntity.setStatus(error == null ? 0 : 1);
            logEntity.setErrorMessage(error);
            logEntity.setRetryCount(0);
            logEntity.setRequestBody(requestBody);
            syncLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("[平台同步] 写入同步日志失败(已忽略): {}", e.getMessage());
        }
    }

    @Override
    public void pushOrderStatus(PlatformConfig config, String platformOrderId, String action) {
        Long tenantId = BaseContext.getCurrentTenantId();
        try {
            executeWithRetry(config.getPlatformType(), "PUSH_STATUS:" + action, () -> {
                PlatformAdapter adapter = getAdapter(config.getPlatformType());
                if (adapter == null) {
                    log.warn("[平台同步] 未找到适配器，跳过状态回传: platformType={}", config.getPlatformType());
                    return null;
                }
                log.info("[平台同步] 状态回传: platformType={}, orderId={}, action={}",
                        config.getPlatformType(), platformOrderId, action);
                switch (action.toLowerCase()) {
                    case "accept":
                        adapter.acceptOrder(config, platformOrderId);
                        break;
                    case "reject":
                        adapter.rejectOrder(config, platformOrderId);
                        break;
                    case "prepare":
                        adapter.prepareOrder(config, platformOrderId);
                        break;
                    case "complete":
                        adapter.completeOrder(config, platformOrderId);
                        break;
                    case "cancel":
                        adapter.cancelOrder(config, platformOrderId);
                        break;
                    default:
                        log.warn("[平台同步] 未知的订单动作: {}", action);
                }
                return null;
            });
            // 成功后记录 OUT 方向审计日志（此前缺失，导致平台侧状态回传无任何痕迹可查）。
            // action 存原始动作（accept/reject/...），PlatformRetryTask 重试时直接作为入参复用。
            saveSyncLog(config, tenantId, action, "OUT",
                    "orderId=" + platformOrderId, null);
        } catch (Exception e) {
            // 重试耗尽仍失败：记 OUT 失败日志，供 PlatformRetryTask 重试补偿
            saveSyncLog(config, tenantId, action, "OUT",
                    "orderId=" + platformOrderId, "回传失败: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void syncDish(PlatformConfig config, Long dishId, String platformDishId, String action) {
        executeWithRetry(config.getPlatformType(), "SYNC_DISH:" + action, () -> {
            PlatformAdapter adapter = getAdapter(config.getPlatformType());
            if (adapter == null) {
                log.warn("[平台同步] 未找到适配器，跳过商品同步: platformType={}", config.getPlatformType());
                return null;
            }
            log.info("[平台同步] 商品同步: platformType={}, dishId={}, action={}",
                    config.getPlatformType(), dishId, action);
            if ("on_shelf".equals(action)) {
                adapter.syncDishOnShelf(config, dishId, platformDishId);
            } else if ("off_shelf".equals(action)) {
                adapter.syncDishOffShelf(config, dishId, platformDishId);
            } else {
                log.warn("[平台同步] 未知的商品动作: {}", action);
            }
            return null;
        });
    }

    @Override
    public void syncStock(PlatformConfig config, String platformDishId, int remainQty) {
        executeWithRetry(config.getPlatformType(), "SYNC_STOCK", () -> {
            PlatformAdapter adapter = getAdapter(config.getPlatformType());
            if (adapter == null) {
                log.warn("[平台同步] 未找到适配器，跳过库存同步: platformType={}", config.getPlatformType());
                return null;
            }
            log.info("[平台同步] 库存同步: platformType={}, platformDishId={}, remainQty={}",
                    config.getPlatformType(), platformDishId, remainQty);
            adapter.syncStock(config, platformDishId, remainQty);
            return null;
        });
    }

    @Override
    public void syncBusinessStatus(PlatformConfig config, boolean open) {
        executeWithRetry(config.getPlatformType(), "SYNC_BUSINESS_STATUS", () -> {
            PlatformAdapter adapter = getAdapter(config.getPlatformType());
            if (adapter == null) {
                log.warn("[平台同步] 未找到适配器，跳过营业状态同步: platformType={}", config.getPlatformType());
                return null;
            }
            log.info("[平台同步] 营业状态同步: platformType={}, open={}", config.getPlatformType(), open);
            adapter.syncBusinessStatus(config, open);
            return null;
        });
    }

    @Override
    public boolean checkHealth(PlatformConfig config) {
        try {
            PlatformAdapter adapter = getAdapter(config.getPlatformType());
            if (adapter == null) {
                log.warn("[平台同步] 未找到适配器，健康检查跳过: platformType={}", config.getPlatformType());
                return false;
            }
            return adapter.healthCheck(config);
        } catch (Exception e) {
            log.error("[平台同步] 健康检查失败: platformType={}", config.getPlatformType(), e);
            return false;
        }
    }
}

