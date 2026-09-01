package com.reggie.module.schedule.task;

import com.reggie.common.BaseContext;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import com.reggie.module.urgency.service.UrgencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 未接单订单定时扫描任务（漏单预警核心调度）
 * </p>
 * <p>
 * 每 30 秒扫描各活跃租户的"待接单"订单：超黄金时长（默认 3 分钟）未接单即主动通知店长，
 * 超漏单阈值（默认 3 倍黄金时长）升级为漏单告警。与接单大屏（pending-monitor.html）形成
 * "大屏可见 + 主动告警"双通道，确保商家在顾客催单之前就感知到漏单风险。
 * </p>
 * <p>
 * 与 {@link OrderTimeoutTask} 相同模式：Redis 分布式锁防多实例重复执行，
 * 无 HTTP 请求上下文，需通过遍历活跃租户列表设置 ThreadLocal。
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
@Slf4j
@Component
public class UnacceptedOrderScanTask {

    /** 扫描间隔（毫秒）：30 秒 */
    private static final long SCAN_INTERVAL_MS = 30 * 1000L;

    /** 分布式锁过期时间（毫秒） */
    private static final long LOCK_TTL_MS = 20 * 1000L;

    @Autowired
    private UrgencyService urgencyService;

    @Autowired
    private TenantService tenantService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每 30 秒扫描未接单订单并主动告警
     */
    @Scheduled(fixedRate = SCAN_INTERVAL_MS)
    public void scanUnacceptedOrders() {
        String lockValue = tryLock("schedule:lock:unaccepted-scan", LOCK_TTL_MS);
        if (lockValue == null) {
            log.debug("[漏单预警] 未接单扫描任务正在执行中，跳过本次");
            return;
        }
        try {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants.isEmpty()) {
                return;
            }
            int totalAlerted = 0;
            for (Tenant tenant : tenants) {
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    totalAlerted += urgencyService.scanUnacceptedAndAlert(tenant.getId());
                } catch (Exception e) {
                    log.error("[漏单预警] 租户 {} 扫描失败: error={}", tenant.getId(), e.getMessage());
                } finally {
                    BaseContext.remove();
                }
            }
            if (totalAlerted > 0) {
                log.info("[漏单预警] 未接单扫描完成，共处理 {} 个租户，本轮新增通知 {} 条", tenants.size(), totalAlerted);
            }
        } finally {
            unlock("schedule:lock:unaccepted-scan", lockValue);
        }
    }

    /**
     * 尝试获取分布式锁（SET NX EX 原子操作）
     */
    private String tryLock(String lockKey, long ttlMs) {
        if (redisTemplate == null) {
            // fail-closed：Redis 不可用时跳过本轮，避免多实例重复告警
            log.warn("[漏单预警] Redis不可用，跳过本次执行（分布式锁获取失败）: {}", lockKey);
            return null;
        }
        try {
            String lockValue = java.util.UUID.randomUUID().toString();
            Boolean success = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, lockValue, ttlMs, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(success) ? lockValue : null;
        } catch (Exception e) {
            log.error("[漏单预警] 获取分布式锁失败，跳过本次执行: {}", lockKey, e);
            return null;
        }
    }

    /**
     * 释放分布式锁（Lua 脚本原子操作：比对锁值后才删除）
     */
    private void unlock(String lockKey, String lockValue) {
        if (redisTemplate == null || lockValue == null) {
            return;
        }
        try {
            String luaScript = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(luaScript, Long.class),
                    java.util.Collections.singletonList(lockKey),
                    lockValue);
        } catch (Exception e) {
            log.error("[漏单预警] 释放分布式锁失败: {}", lockKey, e);
        }
    }
}
