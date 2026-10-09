package com.reggie.module.schedule.task;

import com.reggie.common.BaseContext;
import com.reggie.common.RedisLockUtil;
import com.reggie.module.member.service.PointsRecordService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

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
    /** 公共 Redis 分布式锁工具（SET NX EX + Lua 释放，fail-closed） */
    private final RedisLockUtil redisLockUtil;

    /**
     * 每天凌晨 2:00 执行积分过期扫描
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void expirePoints() {
        log.info("[积分过期] 开始执行积分过期定时任务");

        String lockValue = RedisLockUtil.newLockValue();
        if (!tryAcquire(lockValue)) {
            // fail-closed：拿不到锁（其他实例正在执行或 Redis 异常）跳过本次，
            // 绝不无锁裸跑，避免多实例重复扣积分/发通知
            log.info("[积分过期] 获取分布式锁失败（其他实例正在执行或 Redis 不可用），跳过本次");
            return;
        }

        try {
            processAllTenants();
            log.info("[积分过期] 定时任务执行完成");
        } catch (Exception e) {
            log.error("[积分过期] 处理异常", e);
        } finally {
            redisLockUtil.unlockIfOwned(LOCK_KEY, lockValue);
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

    /**
     * 自旋获取分布式锁：最长等待 {@code LOCK_WAIT_MILLIS}，每 {@code LOCK_RETRY_INTERVAL} 重试一次。
     *
     * <p>修复(P0)：原实现在 Redis 不可用时 {@code return true} 无锁裸跑，多实例下会重复扣积分/发通知；
     * 现统一为 fail-closed——Redis 异常由 {@link RedisLockUtil#tryLock} 按未拿到锁处理并记 log.error，
     * 本方法返回 false，任务跳过本轮。</p>
     */
    private boolean tryAcquire(String lockValue) {
        long startTime = System.currentTimeMillis();
        try {
            while (System.currentTimeMillis() - startTime < LOCK_WAIT_MILLIS) {
                if (redisLockUtil.tryLock(LOCK_KEY, lockValue, LOCK_EXPIRE_SECONDS)) {
                    return true;
                }
                Thread.sleep(LOCK_RETRY_INTERVAL);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[积分过期] 获取锁被中断");
        }
        return false;
    }
}
