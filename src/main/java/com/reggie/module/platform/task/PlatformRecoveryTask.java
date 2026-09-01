package com.reggie.module.platform.task;

import com.reggie.common.BaseContext;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.service.PlatformSyncService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 平台网络异常恢复任务
 * <p>
 * 当网络异常、限流或 Token 过期时，自动重试失败的同步操作。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Component
public class PlatformRecoveryTask {

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private PlatformSyncService platformSyncService;

    @Autowired
    private TenantService tenantService;

    /**
     * 每 5 分钟检查一次平台健康状态，异常时自动重试
     */
    @Scheduled(fixedDelay = 300000)
    public void healthCheckAndRecover() {
        log.info("[平台恢复] 开始健康检查");
        List<Tenant> tenants = tenantService.listActiveTenants();
        if (tenants == null || tenants.isEmpty()) {
            return;
        }

        for (Tenant tenant : tenants) {
            Long originalTenantId = BaseContext.getCurrentTenantId();
            BaseContext.setCurrentTenantId(tenant.getId());
            try {
                checkAndRecoverTenant();
            } catch (Exception e) {
                log.error("[平台恢复] 租户 {} 健康检查失败: {}", tenant.getId(), e.getMessage());
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
     * 检查并恢复当前租户（BaseContext 已注入）下的所有启用平台配置
     */
    private void checkAndRecoverTenant() {
        List<PlatformConfig> configs = platformConfigService.listEnabledConfigs();
        if (configs == null || configs.isEmpty()) {
            return;
        }

        for (PlatformConfig config : configs) {
            try {
                boolean healthy = platformSyncService.checkHealth(config);
                if (!healthy) {
                    log.warn("[平台恢复] 平台异常，尝试恢复: platformType={}", config.getPlatformType());
                    recoverPlatform(config);
                }
            } catch (Exception e) {
                log.error("[平台恢复] 健康检查失败: platformType={}", config.getPlatformType(), e);
            }
        }
    }

    /**
     * 恢复单个平台：重新拉取订单、同步状态
     */
    private void recoverPlatform(PlatformConfig config) {
        try {
            log.info("[平台恢复] 开始恢复: platformType={}", config.getPlatformType());
            // TODO: 重新拉取最近 5 分钟的订单
            String beginTime = java.time.LocalDateTime.now().minusMinutes(5).toString();
            String endTime = java.time.LocalDateTime.now().toString();
            platformSyncService.pullOrders(config, beginTime, endTime);
            log.info("[平台恢复] 恢复成功: platformType={}", config.getPlatformType());
        } catch (Exception e) {
            log.error("[平台恢复] 恢复失败: platformType={}", config.getPlatformType(), e);
        }
    }
}
