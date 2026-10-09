package com.reggie.module.schedule.task;

import com.reggie.common.BaseContext;
import com.reggie.common.RedisLockUtil;
import com.reggie.module.groupbuy.service.GroupBuyService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

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

    /** 公共 Redis 分布式锁工具（SET NX EX + Lua 释放，fail-closed） */
    private final RedisLockUtil redisLockUtil;

    /**
     * 每小时第 5 分钟检查一次过期拼团活动（错开优惠券任务的第 0 分钟，降低峰值）
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void closeExpiredCampaigns() {
        log.info("开始执行拼团过期关闭定时任务");

        String lockValue = RedisLockUtil.newLockValue();
        if (!tryAcquire(lockValue)) {
            // fail-closed：拿不到锁（其他实例正在执行或 Redis 异常），跳过本次
            log.info("[拼团过期] 获取分布式锁失败（其他实例正在执行或 Redis 不可用），跳过本次");
            return;
        }

        try {
            processAllTenants();
            log.info("拼团过期关闭完成");
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("[拼团过期] 处理异常", e);
        } finally {
            redisLockUtil.unlockIfOwned(LOCK_KEY, lockValue);
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
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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
     * 自旋获取分布式锁：最长等待 {@code LOCK_WAIT_MILLIS}，每 {@code LOCK_RETRY_INTERVAL} 重试一次。
     * fail-closed：Redis 异常由 {@link RedisLockUtil#tryLock} 内部按未拿到锁处理并记日志，不向外抛出。
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
            log.warn("[拼团过期] 获取锁被中断");
        }
        return false;
    }
}
