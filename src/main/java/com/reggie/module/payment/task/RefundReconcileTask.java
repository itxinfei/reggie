package com.reggie.module.payment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.enums.RefundStatus;
import com.reggie.module.payment.mapper.RefundRecordMapper;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 退款对账兜底定时任务
 * <p>
 * 场景：渠道退款成功但本地落库失败（RefundServiceImpl 主事务回滚时 persistTrace 降级写入独立事务成功）。
 * 此类痕迹以 "[对账待办]" 前缀标识，状态为 PENDING，需人工核对渠道后台后决定：
 * <ul>
 *   <li>渠道实际已退款：手动标记该条 RefundRecord.status = SUCCESS</li>
 *   <li>渠道实际未退款：手动退款（由人工触发 refundByOrder 或渠道后台操作）</li>
 * </ul>
 *
 * <p>扫描策略：
 * <ul>
 *   <li>每 10 分钟执行一次（fixedDelay）</li>
 *   <li>仅扫描 30 分钟内创建、未被人工处理（状态仍为 PENDING）的痕迹</li>
 *   <li>多租户遍历 + Redis 分布式锁，fail-closed（锁获取失败跳过）</li>
 * </ul>
 * </p>
 *
 * @author reggie
 * @since 2026-09-03
 */
@Slf4j
@Component
public class RefundReconcileTask {

    /** 分布式锁 Key */
    private static final String LOCK_KEY = "schedule:lock:refund-reconcile";
    /** 分布式锁 TTL：10 分钟，防止任务卡死导致锁永久持有 */
    private static final long LOCK_TTL_MS = 10 * 60 * 1000L;
    /** 执行间隔：10 分钟 */
    private static final long INTERVAL_MS = 10 * 60 * 1000L;
    /** 对账痕迹超过此时间未处理则跳过（防止孤儿数据无限堆积告警） */
    private static final int MAX_AGE_HOURS = 72;
    /** 单笔任务最大告警条数（防止数据量过大打爆日志） */
    private static final int MAX_ALERMS_PER_RUN = 100;

    @Autowired
    private RefundRecordMapper refundRecordMapper;

    @Autowired
    private TenantService tenantService;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Scheduled(fixedDelay = INTERVAL_MS)
    public void scanReconcileTraces() {
        String lockValue = tryLock();
        if (lockValue == null) {
            log.debug("[退款对账] 实例锁获取失败，跳过本次执行");
            return;
        }
        try {
            List<Tenant> tenants = tenantService.listActiveTenants();
            if (tenants == null || tenants.isEmpty()) {
                log.debug("[退款对账] 无活跃租户，跳过");
                return;
            }
            int total = 0;
            for (Tenant tenant : tenants) {
                Long originalTenantId = BaseContext.getCurrentTenantId();
                BaseContext.setCurrentTenantId(tenant.getId());
                try {
                    total += scanReconcileTracesForTenant();
                } catch (Exception e) {
                    log.error("[退款对账] 租户 {} 扫描失败: {}", tenant.getId(), e.getMessage(), e);
                } finally {
                    if (originalTenantId != null) {
                        BaseContext.setCurrentTenantId(originalTenantId);
                    } else {
                        BaseContext.remove();
                    }
                }
            }
            if (total > 0) {
                log.warn("[退款对账] 租户扫描完成，共 {} 个租户，告警 {} 条", tenants.size(), total);
            }
        } finally {
            unlock(lockValue);
        }
    }

    /**
     * 扫描当前租户下 "[对账待办]" 痕迹并告警
     *
     * @return 本轮告警条数
     */
    private int scanReconcileTracesForTenant() {
        LocalDateTime maxAgeThreshold = LocalDateTime.now().minusHours(MAX_AGE_HOURS);
        List<RefundRecord> traces = refundRecordMapper.selectList(new LambdaQueryWrapper<RefundRecord>()
                .likeRight(RefundRecord::getReason, "[对账待办]")
                .eq(RefundRecord::getStatus, RefundStatus.PENDING.getCode())
                .ge(RefundRecord::getCreatedTime, maxAgeThreshold)
                .orderByDesc(RefundRecord::getCreatedTime)
                .last("limit " + MAX_ALERMS_PER_RUN));
        if (traces == null || traces.isEmpty()) {
            return 0;
        }
        int count = traces.size();
        for (RefundRecord trace : traces) {
            log.warn("[退款对账] 待人工核对: tenantId={}, traceId={}, refundNo={}, amount={}, reason={}, createdTime={}",
                    trace.getTenantId(), trace.getId(), trace.getRefundNo(),
                    trace.getAmount(), trace.getReason(), trace.getCreatedTime());
        }
        return count;
    }

    private String tryLock() {
        if (redisTemplate == null) {
            log.warn("[退款对账] Redis 不可用，跳过本次执行（分布式锁获取失败）");
            return null;
        }
        try {
            String lockValue = UUID.randomUUID().toString();
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent(LOCK_KEY, lockValue, LOCK_TTL_MS, TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(ok) ? lockValue : null;
        } catch (Exception e) {
            log.error("[退款对账] 获取分布式锁失败，跳过本次执行: {}", e.getMessage());
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
                    Collections.singletonList(LOCK_KEY),
                    lockValue);
        } catch (Exception e) {
            log.error("[退款对账] 释放分布式锁失败: {}", e.getMessage());
        }
    }
}
