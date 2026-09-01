package com.reggie.module.schedule.task;

import com.reggie.common.BaseContext;
import com.reggie.module.groupbuy.service.GroupBuyService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务：自动关闭已过期的拼团活动
 *
 * 关闭条件：状态为 OPEN 且 endTime <= 当前时间。
 * 拼团活动到期后需置为已结束，避免用户继续参与已过期的活动，并触发已成团/未成团的收尾。
 *
 * 与 CouponExpirationTask 保持一致的分布式锁 + 遍历租户模式：
 * 1. Redis 分布式锁防多实例并发处理同一批活动
 * 2. 遍历活跃租户设置 BaseContext，确保 MyBatis-Plus 租户拦截器正确过滤
 *
 * @author 心飞为你飞
 * @since 2026-09-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupBuyExpireTask {

    /** Redis 分布式锁 Key */
    private static final String LOCK_KEY = "lock:groupbuy:expire";

    /** 分布式锁过期时间（秒） */
    private static final int LOCK_EXPIRE_SECONDS = 60;

    /** 获取锁最大等待时间（毫秒） */
    private static final int LOCK_WAIT_MILLIS = 3000;

    /** 获取锁重试间隔（毫秒） */
    private static final int LOCK_RETRY_INTERVAL = 100;

    private final GroupBuyService groupBuyService;

    private final TenantService tenantService;

    /** Redis 模板（可选，不可用时跳过锁降级） */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每小时第 5 分钟检查一次过期拼团活动（错开优惠券任务的第 0 分钟，降低峰值）
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void closeExpiredCampaigns() {
        log.info("开始执行拼团过期关闭定时任务");

        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = false;
        try {
            acquired = tryAcquire(LOCK_KEY, lockValue);
        } catch (Exception e) {
            log.warn("[拼团过期] 获取分布式锁异常，跳过本次执行: {}", e.getMessage(), e);
            return;
        }

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("[拼团过期] 获取分布式锁失败，其他实例正在执行，跳过本次");
            return;
        }

        try {
            processAllTenants();
            log.info("拼团过期关闭完成");
        } catch (Exception e) {
            log.error("[拼团过期] 处理异常", e);
        } finally {
            tryReleaseLock(LOCK_KEY, lockValue);
        }
    }

    /**
     * 遍历所有活跃租户，逐个设置 BaseContext 后处理
     */
    private void processAllTenants() {
        List<Tenant> tenants = tenantService.listActiveTenants();
        if (tenants == null || tenants.isEmpty()) {
            log.warn("[拼团过期] 无活跃租户，跳过");
            return;
        }
        for (Tenant tenant : tenants) {
            Long originalTenantId = BaseContext.getCurrentTenantId();
            BaseContext.setCurrentTenantId(tenant.getId());
            try {
                int closed = groupBuyService.autoCloseExpiredCampaigns();
                log.info("[拼团过期] 租户{}关闭{}个活动", tenant.getId(), closed);
            } catch (Exception e) {
                log.error("[拼团过期] 租户{}处理异常: {}", tenant.getId(), e.getMessage(), e);
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
     * 获取分布式锁：SETNX + EXPIRE
     */
    private Boolean tryAcquire(String lockKey, String lockValue) {
        if (redisTemplate == null) {
            return false;
        }
        long startTime = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - startTime < LOCK_WAIT_MILLIS) {
                Boolean success = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(success)) {
                    return true;
                }
                Thread.sleep(LOCK_RETRY_INTERVAL);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[拼团过期] 获取锁被中断");
        } catch (Exception e) {
            log.error("[拼团过期] 获取锁异常", e);
        }
        return false;
    }

    /**
     * 释放分布式锁：Lua 脚本原子校验 + 删除，防止 TTL 过期瞬间 get+delete 的 TOCTOU 竞态。
     * 与 CouponExpirationTask / OrderTimeoutTask 保持一致的 Lua 脚本模式。
     */
    private void tryReleaseLock(String lockKey, String lockValue) {
        if (redisTemplate == null || lockValue == null) {
            return;
        }
        try {
            // Lua 脚本：比对锁值后才删除，防止误删他人的锁；同时消除 get+delete 之间的竞态窗口
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                    new DefaultRedisScript<Long>(luaScript, Long.class),
                    Collections.singletonList(lockKey),
                    lockValue
            );
        } catch (Exception e) {
            log.warn("[拼团过期] 释放锁失败: {}", e.getMessage(), e);
        }
    }
}
