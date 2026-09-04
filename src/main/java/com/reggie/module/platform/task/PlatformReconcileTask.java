package com.reggie.module.platform.task;

import com.reggie.common.BaseContext;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformReconcileTaskService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 平台对账定时任务
 * <p>
 * 每天凌晨自动对前一天的平台订单进行对账。
 * </p>
 * <p>
 * 多实例部署时通过 Redis 分布式锁（SETNX + Lua 释放）保证同一时刻仅一个节点执行，
 * 与 PlatformReconcileTaskServiceImpl 内的 ConcurrentHashMap 单 JVM 锁 + DB UNIQUE 索引形成三层防护。
 * Redis 不可用时 fail-closed 跳过，避免多实例重复对账。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Component
public class PlatformReconcileTask {

    /** 分布式锁过期时间（毫秒），对账为批量任务，TTL 设大于普通任务 */
    private static final long LOCK_TTL_MS = 10 * 60 * 1000L; // 10分钟

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private PlatformReconcileTaskService reconcileTaskService;

    @Autowired
    private TenantService tenantService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每天凌晨 2 点执行对账
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeDailyReconcile() {
        // 分布式锁防止多实例重复执行（fail-closed：Redis 不可用则跳过）
        String lockValue = tryLock("platform:lock:reconcile", LOCK_TTL_MS);
        if (lockValue == null) {
            log.debug("对账任务正在执行中，跳过本次");
            return;
        }

        try {
            log.info("开始执行平台对账任务");
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants == null || tenants.isEmpty()) {
                log.info("无活跃租户，跳过对账");
                return;
            }

            for (Tenant tenant : tenants) {
                Long originalTenantId = BaseContext.getCurrentTenantId();
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    reconcileTenant(yesterday);
                } catch (Exception e) {
                    log.error("租户 {} 对账失败: {}", tenant.getId(), e.getMessage());
                } finally {
                    if (originalTenantId != null) {
                        BaseContext.setCurrentTenantId(originalTenantId);
                    } else {
                        BaseContext.remove();
                    }
                }
            }
        } finally {
            unlock("platform:lock:reconcile", lockValue);
        }
    }

    /**
     * 对账当前租户（BaseContext 已注入）下的所有启用平台配置
     */
    private void reconcileTenant(LocalDate yesterday) {
        List<PlatformConfig> configs = platformConfigService.listEnabledConfigs();
        if (configs == null || configs.isEmpty()) {
            log.info("没有启用的平台配置，跳过对账");
            return;
        }

        for (PlatformConfig config : configs) {
            try {
                log.info("开始对账: platformType={}, date={}", config.getPlatformType(), yesterday);
                reconcileTaskService.reconcile(config.getPlatformType(), yesterday);
                log.info("对账完成: platformType={}", config.getPlatformType());
            } catch (Exception e) {
                log.error("对账失败: platformType={}", config.getPlatformType(), e);
            }
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
            // fail-closed：Redis 不可用时跳过本次执行，避免多实例重复对账
            log.warn("[平台对账] Redis不可用，跳过本次执行（分布式锁获取失败）: {}", lockKey);
            return null;
        }
        try {
            String lockValue = UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, ttlMs, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(success) ? lockValue : null;
        } catch (Exception e) {
            log.error("[平台对账] 获取分布式锁失败，跳过本次执行: {}", lockKey, e);
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
            log.error("[平台对账] 释放分布式锁失败: {}", lockKey, e);
        }
    }
}
