package com.reggie.module.schedule.task;

import com.reggie.common.BaseContext;
import com.reggie.module.member.service.PointsRecordService;
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
 * 定时任务：每天凌晨扫描过期积分并自动扣减
 *
 * <p>积分有效期为获取之日起 1 年（由 {@code MemberServiceImpl.addPoints} 设置 expireTime）。
 * 本任务每天凌晨 2:00 扫描 expire_time <= NOW() 的 IN 类型积分记录，
 * 按会员汇总后调用扣减逻辑（含等级降级检查），最后逻辑删除已处理记录。</p>
 *
 * <p>安全加固：Redis 分布式锁防多实例并发，逐租户处理确保租户隔离。</p>
 *
 * @author 心飞为你飞
 * @since 2026-09-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointsExpireTask {

    private static final String LOCK_KEY = "lock:points:expire";
    private static final int LOCK_EXPIRE_SECONDS = 300;
    private static final int LOCK_WAIT_MILLIS = 3000;
    private static final int LOCK_RETRY_INTERVAL = 100;

    private final PointsRecordService pointsRecordService;
    private final TenantService tenantService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 每天凌晨 2:00 执行积分过期扫描
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void expirePoints() {
        log.info("[积分过期] 开始执行积分过期定时任务");

        String lockValue = UUID.randomUUID().toString();
        Boolean acquired = false;
        try {
            acquired = tryAcquire(lockValue);
        } catch (Exception e) {
            log.warn("[积分过期] 获取分布式锁异常，跳过本次执行: {}", e.getMessage(), e);
            return;
        }

        if (!Boolean.TRUE.equals(acquired)) {
            log.warn("[积分过期] 获取分布式锁失败，其他实例正在执行，跳过本次");
            return;
        }

        try {
            processAllTenants();
            log.info("[积分过期] 定时任务执行完成");
        } catch (Exception e) {
            log.error("[积分过期] 处理异常", e);
        } finally {
            tryReleaseLock(lockValue);
        }
    }

    /**
     * 遍历所有活跃租户，逐个设置 BaseContext 后处理过期积分
     */
    private void processAllTenants() {
        List<Tenant> tenants = tenantService.listActiveTenants();
        if (tenants == null || tenants.isEmpty()) {
            log.warn("[积分过期] 无活跃租户，跳过");
            return;
        }
        for (Tenant tenant : tenants) {
            Long originalTenantId = BaseContext.getCurrentTenantId();
            BaseContext.setCurrentTenantId(tenant.getId());
            try {
                pointsRecordService.expirePointsBatch();
                log.info("[积分过期] 租户{}处理完成", tenant.getId());
            } catch (Exception e) {
                log.error("[积分过期] 租户{}处理异常: {}", tenant.getId(), e.getMessage(), e);
            } finally {
                if (originalTenantId != null) {
                    BaseContext.setCurrentTenantId(originalTenantId);
                } else {
                    BaseContext.remove();
                }
            }
        }
    }

    private Boolean tryAcquire(String lockValue) {
        if (redisTemplate == null) {
            return true; // Redis 不可用时降级直接执行（单实例部署场景）
        }
        long startTime = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - startTime < LOCK_WAIT_MILLIS) {
                Boolean success = redisTemplate.opsForValue()
                        .setIfAbsent(LOCK_KEY, lockValue, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(success)) {
                    return true;
                }
                Thread.sleep(LOCK_RETRY_INTERVAL);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[积分过期] 获取锁被中断");
        } catch (Exception e) {
            log.error("[积分过期] 获取锁异常", e);
        }
        return false;
    }

    private void tryReleaseLock(String lockValue) {
        if (redisTemplate == null || lockValue == null) {
            return;
        }
        try {
            String luaScript =
                    "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                    java.util.Collections.singletonList(LOCK_KEY),
                    lockValue
            );
        } catch (Exception e) {
            log.warn("[积分过期] 释放锁失败: {}", e.getMessage(), e);
        }
    }
}
