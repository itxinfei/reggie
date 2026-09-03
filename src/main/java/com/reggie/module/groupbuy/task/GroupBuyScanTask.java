package com.reggie.module.groupbuy.task;

import com.reggie.common.BaseContext;
import com.reggie.module.groupbuy.service.GroupBuyService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 拼团成团判定定时任务
 * <p>
 * 每 5 分钟扫描每个租户下所有 OPEN 状态且已过 end_time 的 campaign：
 * <ul>
 *   <li>已付款参与人数 >= min_members → 成团，标记 CLOSED（可履约）</li>
 *   <li>未成团 → 标记 ENDED，对已支付参与者的订单发起全额退款</li>
 * </ul>
 * 多实例部署下通过 Redis SETNX 分布式锁保证同一时刻只有一个实例执行。
 * 若 Redis 不可用，fail-closed 跳过本次执行，避免多实例重复发起退款。
 * </p>
 *
 * @author reggie
 * @since 2026-09-03
 */
@Slf4j
@Component
public class GroupBuyScanTask {

    private static final String LOCK_KEY = "schedule:lock:group-buy-scan";
    /** 分布式锁 TTL：5 分钟，大于单次任务执行预算，避免主进程卡死时锁永久持有 */
    private static final long LOCK_TTL_MS = 5 * 60 * 1000L;
    /** 执行间隔 5 分钟 */
    private static final long INTERVAL_MS = 5 * 60 * 1000L;

    @Autowired
    private GroupBuyService groupBuyService;

    @Autowired
    private TenantService tenantService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedDelay = INTERVAL_MS)
    public void scanGroupFormedAndNotFormed() {
        String lockValue = tryLock();
        if (lockValue == null) {
            log.debug("[拼团扫描] 实例锁获取失败，跳过本次执行");
            return;
        }
        try {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants == null || tenants.isEmpty()) {
                log.debug("[拼团扫描] 无活跃租户，跳过");
                return;
            }
            int total = 0;
            for (Tenant tenant : tenants) {
                Long originalTenantId = BaseContext.getCurrentTenantId();
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    total += groupBuyService.scanGroupFormedAndNotFormed();
                } catch (Exception e) {
                    log.error("[拼团扫描] 租户 {} 扫描失败: {}", tenant.getId(), e.getMessage(), e);
                } finally {
                    if (originalTenantId != null) {
                        BaseContext.setCurrentTenantId(originalTenantId);
                    } else {
                        BaseContext.remove();
                    }
                }
            }
            if (total > 0) {
                log.info("[拼团扫描] 租户扫描完成，共 {} 个租户处理 {} 条 campaign",
                        tenants.size(), total);
            }
        } finally {
            unlock(lockValue);
        }
    }

    private String tryLock() {
        if (redisTemplate == null) {
            log.warn("[拼团扫描] Redis 不可用，跳过本次执行（分布式锁获取失败）");
            return null;
        }
        try {
            String lockValue = java.util.UUID.randomUUID().toString();
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(LOCK_KEY, lockValue, LOCK_TTL_MS, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(ok) ? lockValue : null;
        } catch (Exception e) {
            log.error("[拼团扫描] 获取分布式锁失败，跳过本次执行: {}", e.getMessage());
            return null;
        }
    }

    private void unlock(String lockValue) {
        if (redisTemplate == null || lockValue == null) {
            return;
        }
        try {
            String lua = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            redisTemplate.execute(
                    new org.springframework.data.redis.core.script.DefaultRedisScript<>(lua, Long.class),
                    java.util.Collections.singletonList(LOCK_KEY),
                    lockValue);
        } catch (Exception e) {
            log.error("[拼团扫描] 释放分布式锁失败: {}", e.getMessage());
        }
    }
}
