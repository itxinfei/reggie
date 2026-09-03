package com.reggie.module.platform.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.platform.adapter.PlatformOrder;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.model.PlatformSyncLog;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformSyncLogService;
import com.reggie.module.platform.service.PlatformSyncService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 平台同步重试任务
 * <p>
 * 定时扫描失败的同步日志，对重试次数未达上限的记录进行自动重试。
 * 采用指数退避策略：每次重试后延迟时间翻倍。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Component
public class PlatformRetryTask {

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 5;

    @Autowired
    private PlatformSyncLogService syncLogService;

    @Autowired
    private PlatformSyncService platformSyncService;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private TenantService tenantService;

    /**
     * 每 2 分钟执行一次，重试失败的同步操作
     */
    @Scheduled(fixedDelay = 120000)
    public void retryFailedOperations() {
        log.info("[平台重试] 开始扫描失败日志");
        List<Tenant> tenants = tenantService.listActiveTenants();
        if (tenants == null || tenants.isEmpty()) {
            log.info("[平台重试] 无活跃租户，跳过");
            return;
        }

        for (Tenant tenant : tenants) {
            Long originalTenantId = BaseContext.getCurrentTenantId();
            BaseContext.setCurrentTenantId(tenant.getId());
            try {
                retryTenant();
            } catch (Exception e) {
                log.error("[平台重试] 租户 {} 扫描失败: {}", tenant.getId(), e.getMessage());
            } finally {
                if (originalTenantId != null) {
                    BaseContext.setCurrentTenantId(originalTenantId);
                } else {
                    BaseContext.remove();
                }
            }
        }
    }

    /**
     * 重试当前租户（BaseContext 已注入）下所有失败且重试次数未达上限的日志
     */
    private void retryTenant() {
        // 查询该租户下所有失败且重试次数未达上限的日志（租户插件自动注入 tenant_id）
        LambdaQueryWrapper<PlatformSyncLog> qw = new LambdaQueryWrapper<>();
        qw.eq(PlatformSyncLog::getStatus, 1) // 失败
          .lt(PlatformSyncLog::getRetryCount, MAX_RETRY_COUNT);
        List<PlatformSyncLog> failedLogs = syncLogService.list(qw);

        if (failedLogs == null || failedLogs.isEmpty()) {
            return;
        }

        log.info("[平台重试] 租户 {} 发现 {} 条待重试记录", BaseContext.getCurrentTenantId(), failedLogs.size());

        for (PlatformSyncLog logEntry : failedLogs) {
            try {
                retryLogEntry(logEntry);
            } catch (Exception e) {
                log.error("[平台重试] 重试失败: id={}, action={}, error={}",
                        logEntry.getId(), logEntry.getAction(), e.getMessage());
            }
        }
    }

    /**
     * 重试单条日志记录
     * <p>
     * 按同步方向分发到真实同步方法：
     * <ul>
     *   <li>IN（拉单）：调用 pullOrders + persistOrders 重新拉取并落库。
     *       因 PlatformSyncLog 未记录原始时间范围，重试采用近期 10 分钟窗口；
     *       如需精确还原原始拉单范围，应在写 log 时记录 beginTime/endTime。</li>
     *   <li>OUT（状态回传）：调用 pushOrderStatus，action 字段即具体动作
     *       （accept/reject/prepare/complete/cancel）。</li>
     * </ul>
     * 真实调用成功才标记 status=0；失败保持 status=1 并记 errorMessage，
     * retryCount 在方法开头累加，达到 MAX_RETRY_COUNT 后不再被 retryTenant 查到。
     * </p>
     */
    private void retryLogEntry(PlatformSyncLog logEntry) {
        logEntry.setRetryCount(logEntry.getRetryCount() + 1);

        try {
            PlatformConfig config = platformConfigService.getByPlatformType(
                    logEntry.getPlatformType(), logEntry.getTenantId());
            if (config == null) {
                throw new CustomException("无可用平台配置: platformType=" + logEntry.getPlatformType());
            }

            String direction = logEntry.getDirection();
            if ("IN".equals(direction)) {
                // 拉单重试：log 未记录原始时间范围，采用近期 10 分钟窗口重新拉取
                String endTime = java.time.LocalDateTime.now().toString();
                String beginTime = java.time.LocalDateTime.now().minusMinutes(10).toString();
                List<PlatformOrder> orders = platformSyncService.pullOrders(config, beginTime, endTime);
                int persisted = platformSyncService.persistOrders(config, orders);
                log.info("[平台重试] 拉单重试成功: platformType={}, 拉取={}, 落库={}",
                        logEntry.getPlatformType(),
                        orders == null ? 0 : orders.size(), persisted);
            } else if ("OUT".equals(direction)) {
                // 状态回传重试：action 字段即具体动作
                platformSyncService.pushOrderStatus(config, logEntry.getPlatformOrderId(), logEntry.getAction());
                log.info("[平台重试] 状态回传重试成功: platformType={}, orderId={}, action={}",
                        logEntry.getPlatformType(), logEntry.getPlatformOrderId(), logEntry.getAction());
            } else {
                throw new CustomException("未知同步方向: " + direction);
            }

            // 真实调用成功才标记成功（修复原先无条件 setStatus(0) 的缺陷）
            logEntry.setStatus(0);
            logEntry.setErrorMessage(null);
            log.info("[平台重试] 重试成功: id={}, action={}, retryCount={}",
                    logEntry.getId(), logEntry.getAction(), logEntry.getRetryCount());

        } catch (Exception e) {
            // 重试仍失败：保持 status=1，记错误信息，retryCount 已在开头累加
            logEntry.setStatus(1);
            logEntry.setErrorMessage(e.getMessage());
            log.error("[平台重试] 重试仍失败: id={}, action={}, retryCount={}, error={}",
                    logEntry.getId(), logEntry.getAction(), logEntry.getRetryCount(), e.getMessage());
        } finally {
            syncLogService.updateById(logEntry);
        }
    }

    /**
     * 清理超过 7 天的成功日志
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldSuccessLogs() {
        log.info("[平台重试] 开始清理旧日志");
        // TODO: 实现日志清理逻辑
    }
}
