package com.reggie.module.platform.task;

import com.reggie.common.BaseContext;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformSyncService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 平台网络异常恢复任务
 * <p>
 * 当网络异常、限流或 Token 过期时，自动重试失败的同步操作。
 * </p>
 * <p>
 * 多实例部署时通过 Redis 分布式锁（SETNX + Lua 释放）保证同一时刻仅一个节点执行，
 * Redis 不可用时 fail-closed 跳过，避免多实例重复健康检查触发平台压流。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Component
public class PlatformRecoveryTask {

    /** 分布式锁过期时间（毫秒），应大于任务最大执行时间 */
    private static final long LOCK_TTL_MS = 5 * 60 * 1000L; // 5分钟

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private PlatformSyncService platformSyncService;

    @Autowired
    private TenantService tenantService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每 5 分钟检查一次平台健康状态，异常时自动重试
     */
    @Scheduled(fixedDelay = 300000)
    public void healthCheckAndRecover() {
        // 分布式锁防止多实例重复执行（fail-closed：Redis 不可用则跳过）
        String lockValue = tryLock("platform:lock:recovery", LOCK_TTL_MS);
        if (lockValue == null) {
            log.debug("[平台恢复] 健康检查任务正在执行中，跳过本次");
            return;
        }

        try {
            log.info("[平台恢复] 开始健康检查");
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants == null || tenants.isEmpty()) {
                return;
            }

            for (Tenant tenant : tenants) {
                Long originalTenantId = BaseContext.getCurrentTenantId();
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    checkAndRecoverTenant();
                } catch (Exception e) {
                    log.error("[平台恢复] 租户 {} 健康检查失败: {}", tenant.getId(), e.getMessage());
                } finally {
                    if (originalTenantId != null) {
                        BaseContext.setCurrentTenantId(originalTenantId);
                    } else {
                        BaseContext.remove();
                    }
                }
            }
        } finally {
            unlock("platform:lock:recovery", lockValue);
        }
    }

    /**
     * 检查并恢复当前租户（BaseContext 已注入）下的所有启用平台配置
     */
    private void checkAndRecoverTenant() {
        List<PlatformConfig> configs = platformConfigService.listEnabledConfigs();
        if (configs == null || configs.isEmpty()) {
            return;
        }

        for (PlatformConfig config : configs) {
            try {
                boolean healthy = platformSyncService.checkHealth(config);
                if (!healthy) {
                    log.warn("[平台恢复] 平台异常，尝试恢复: platformType={}", config.getPlatformType());
                    recoverPlatform(config);
                }
            } catch (Exception e) {
                log.error("[平台恢复] 健康检查失败: platformType={}", config.getPlatformType(), e);
            }
        }
    }

    /**
     * 恢复单个平台：重新拉取订单、同步状态
     */
    private void recoverPlatform(PlatformConfig config) {
        try {
            log.info("[平台恢复] 开始恢复: platformType={}", config.getPlatformType());
            // TODO: 重新拉取最近 5 分钟的订单
            String beginTime = java.time.LocalDateTime.now().minusMinutes(5).toString();
            String endTime = java.time.LocalDateTime.now().toString();
            platformSyncService.pullOrders(config, beginTime, endTime);
            log.info("[平台恢复] 恢复成功: platformType={}", config.getPlatformType());
        } catch (Exception e) {
            log.error("[平台恢复] 恢复失败: platformType={}", config.getPlatformType(), e);
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
            // fail-closed：Redis 不可用时跳过本次执行，避免多实例重复健康检查
            log.warn("[平台恢复] Redis不可用，跳过本次执行（分布式锁获取失败）: {}", lockKey);
            return null;
        }
        try {
            String lockValue = UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, ttlMs, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(success) ? lockValue : null;
        } catch (Exception e) {
            log.error("[平台恢复] 获取分布式锁失败，跳过本次执行: {}", lockKey, e);
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
            log.error("[平台恢复] 释放分布式锁失败: {}", lockKey, e);
        }
    }
}
