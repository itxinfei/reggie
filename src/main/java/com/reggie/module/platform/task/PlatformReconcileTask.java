package com.reggie.module.platform.task;

import com.reggie.common.BaseContext;
import com.reggie.common.RedisLockUtil;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformReconcileTaskService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

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

    /** 分布式锁过期时间（秒），对账为批量任务，TTL 设大于普通任务 */
    private static final long LOCK_TTL_SECONDS = 10 * 60L; // 10分钟

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private PlatformReconcileTaskService reconcileTaskService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RedisLockUtil redisLockUtil;

    /** 修改点(2026-09-15)：平台同步总开关（默认 false）。适配器为占位协议时对账无真实数据可对，
     * 关闭开关可避免无效对账外呼 */
    @Value("${reggie.platform.sync-enabled:false}")
    private boolean syncEnabled;

    /**
     * 每天凌晨 2 点执行对账
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeDailyReconcile() {
        // 修改点(2026-09-15)：总开关守卫——未开启真实平台对接前不执行对账
        if (!syncEnabled) {
            log.debug("[平台对账] reggie.platform.sync-enabled=false，跳过本次对账");
            return;
        }
        // 分布式锁防止多实例重复执行（fail-closed：拿不到锁=另一实例在执行或 Redis 不可用，跳过本轮）
        redisLockUtil.executeWithLock("platform:lock:reconcile", LOCK_TTL_SECONDS, () -> {
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
                    // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                    log.error("租户 {} 对账失败: {}", tenant.getId(), e.getMessage());
                } finally {
                    if (originalTenantId != null) {
                        BaseContext.setCurrentTenantId(originalTenantId);
                    } else {
                        BaseContext.remove();
                    }
                }
            }
        });
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
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("对账失败: platformType={}", config.getPlatformType(), e);
            }
        }
    }
}
