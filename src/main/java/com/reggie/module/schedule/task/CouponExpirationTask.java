package com.reggie.module.schedule.task;

import com.reggie.common.BaseContext;
import com.reggie.module.member.service.CouponTemplateService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务：自动将过期未使用的优惠券标记为已过期
 *
 * 安全加固（2026-08-23）：
 * 1. 添加 Redis 分布式锁，防止多实例并发处理同一批优惠券
 * 2. 遍历所有活跃租户，为每个租户设置 BaseContext 后单独处理，
 *    确保 MyBatis-Plus 租户拦截器正确过滤数据
 *
 * @author 心飞为你飞
 * @since 2026-08-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpirationTask {

    /** Redis 分布式锁 Key */
    private static final String LOCK_KEY = "lock:coupon:expiration";

    /** 分布式锁过期时间（秒） */
    private static final int LOCK_EXPIRE_SECONDS = 60;

    /** 获取锁最大等待时间（毫秒） */
    private static final int LOCK_WAIT_MILLIS = 3000;

    /** 获取锁重试间隔（毫秒） */
    private static final int LOCK_RETRY_INTERVAL = 100;

    private final CouponTemplateService couponTemplateService;

    private final TenantService tenantService;

    /** Redis 模板（可选，不可用时跳过锁降级） */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每小时检查一次过期优惠券
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireUnusedCoupons() {
        log.info("开始执行优惠券过期检查定时任务");

        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = false;
        try {
            acquired = tryAcquire(lockKey(), lockValue);
        } catch (Exception e) {
            log.warn("[优惠券过期] 获取分布式锁异常，跳过本次执行: {}", e.getMessage(), e);
            return;
        }

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("[优惠券过期] 获取分布式锁失败，其他实例正在执行，跳过本次");
            return;
        }

        try {
            processAllTenants();
            log.info("优惠券过期检查完成");
        } catch (Exception e) {
            log.error("[优惠券过期] 处理异常", e);
        } finally {
            tryReleaseLock(lockKey(), lockValue);
        }
    }

    /**
     * 遍历所有活跃租户，逐个设置 BaseContext 后处理
     */
    private void processAllTenants() {
        List<Tenant> tenants = tenantService.listActiveTenants();
        if (tenants == null || tenants.isEmpty()) {
            log.warn("[优惠券过期] 无活跃租户，跳过");
            return;
        }
        for (Tenant tenant : tenants) {
            Long originalTenantId = BaseContext.getCurrentTenantId();
            BaseContext.setCurrentTenantId(tenant.getId());
            try {
                couponTemplateService.expireCoupons();
                log.info("[优惠券过期] 租户{}处理完成", tenant.getId());
            } catch (Exception e) {
                log.error("[优惠券过期] 租户{}处理异常: {}", tenant.getId(), e.getMessage(), e);
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
            log.warn("[优惠券过期] 获取锁被中断");
        } catch (Exception e) {
            log.error("[优惠券过期] 获取锁异常", e);
        }
        return false;
    }

    /**
     * 释放分布式锁：校验 UUID ownership 后删除
     */
    private void tryReleaseLock(String lockKey, String lockValue) {
        if (redisTemplate == null) {
            return;
        }
        try {
            Object current = redisTemplate.opsForValue().get(lockKey);
            if (lockValue.equals(current)) {
                redisTemplate.delete(lockKey);
            } else {
                log.warn("[优惠券过期] 锁已不属于当前实例，跳过释放");
            }
        } catch (Exception e) {
            log.warn("[优惠券过期] 释放锁失败: {}", e.getMessage(), e);
        }
    }

    private String lockKey() {
        return LOCK_KEY;
    }
}