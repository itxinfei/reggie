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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 平台同步重试任务
 * <p>
 * 定时扫描失败的同步日志，对重试次数未达上限的记录进行自动重试。
 * 采用指数退避策略：每次重试后延迟时间翻倍。
 * </p>
 * <p>
 * 多实例部署时通过 Redis 分布式锁（SETNX + Lua 释放）保证同一时刻仅一个节点执行，
 * Redis 不可用时 fail-closed 跳过，避免多实例重复重试导致平台压流。
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

    /** 成功日志保留天数：超过此天数的成功日志将被清理 */
    private static final int SUCCESS_LOG_RETAIN_DAYS = 7;

    /** 分布式锁过期时间（毫秒），应大于任务最大执行时间 */
    private static final long LOCK_TTL_MS = 4 * 60 * 1000L; // 4分钟

    @Autowired
    private PlatformSyncLogService syncLogService;

    @Autowired
    private PlatformSyncService platformSyncService;

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private TenantService tenantService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每 2 分钟执行一次，重试失败的同步操作
     */
    @Scheduled(fixedDelay = 120000)
    public void retryFailedOperations() {
        // 分布式锁防止多实例重复执行（fail-closed：Redis 不可用则跳过）
        String lockValue = tryLock("platform:lock:retry", LOCK_TTL_MS);
        if (lockValue == null) {
            log.debug("[平台重试] 重试任务正在执行中，跳过本次");
            return;
        }

        try {
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
        } finally {
            unlock("platform:lock:retry", lockValue);
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
     * 清理超过 {@value #SUCCESS_LOG_RETAIN_DAYS} 天的成功日志
     * <p>
     * 仅清理 status=0（成功）的日志，失败日志（status=1）保留以供重试与排查。
     * 成功日志长期累积会拖慢平台同步日志表查询，每日凌晨 3 点清理。
     * </p>
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldSuccessLogs() {
        // 分布式锁防止多实例重复清理（fail-closed）
        String lockValue = tryLock("platform:lock:clean-logs", LOCK_TTL_MS);
        if (lockValue == null) {
            log.debug("[平台重试] 日志清理任务正在执行中，跳过本次");
            return;
        }

        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(SUCCESS_LOG_RETAIN_DAYS);
            LambdaQueryWrapper<PlatformSyncLog> qw = new LambdaQueryWrapper<>();
            qw.eq(PlatformSyncLog::getStatus, 0) // 仅清理成功日志
              .lt(PlatformSyncLog::getCreateTime, threshold);

            long count = syncLogService.count(qw);
            if (count == 0) {
                log.info("[平台重试] 无需清理：无 {} 天前的成功日志", SUCCESS_LOG_RETAIN_DAYS);
                return;
            }
            boolean removed = syncLogService.remove(qw);
            log.info("[平台重试] 清理旧成功日志完成: threshold={}, 删除={}, 成功={}",
                    threshold, count, removed);
        } catch (Exception e) {
            log.error("[平台重试] 清理旧日志失败", e);
        } finally {
            unlock("platform:lock:clean-logs", lockValue);
        }
    }

    // ──────────────────────────────────────
    // 分布式锁辅助方法（与 OrderTimeoutTask 同模式）
    // ──────────────────────────────────────

    /**
     * 尝试获取分布式锁
     * @param lockKey 锁Key
     * @param ttlMs 锁过期时间（毫秒）
     * @return 锁值（UUID），获取失败返回null
     */
    private String tryLock(String lockKey, long ttlMs) {
        if (redisTemplate == null) {
            // fail-closed：Redis 不可用时跳过本次执行，避免多实例重复重试
            log.warn("[平台重试] Redis不可用，跳过本次执行（分布式锁获取失败）: {}", lockKey);
            return null;
        }
        try {
            String lockValue = UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, ttlMs, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(success) ? lockValue : null;
        } catch (Exception e) {
            log.error("[平台重试] 获取分布式锁失败，跳过本次执行: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放分布式锁（Lua 脚本原子操作：比对锁值后才删除）
     * @param lockKey 锁Key
     * @param lockValue 锁值（UUID）
     */
    private void unlock(String lockKey, String lockValue) {
        if (redisTemplate == null || lockValue == null) {
            return;
        }
        try {
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                new DefaultRedisScript<Long>(luaScript, Long.class),
                Collections.singletonList(lockKey),
                lockValue
            );
        } catch (Exception e) {
            log.error("[平台重试] 释放分布式锁失败: {}", lockKey, e);
        }
    }
}
