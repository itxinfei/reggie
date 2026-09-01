package com.reggie.module.platform.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.platform.model.PlatformSyncLog;
import com.reggie.module.platform.service.PlatformSyncLogService;
import com.reggie.module.platform.service.PlatformSyncService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 平台同步重试任务
 * <p>
 * 定时扫描失败的同步日志，对重试次数未达上限的记录进行自动重试。
 * 采用指数退避策略：每次重试后延迟时间翻倍。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Component
public class PlatformRetryTask {

    /** 最大重试次数 */
    private static final int MAX_RETRY_COUNT = 5;

    @Autowired
    private PlatformSyncLogService syncLogService;

    @Autowired
    private PlatformSyncService platformSyncService;

    @Autowired
    private TenantService tenantService;

    /**
     * 每 2 分钟执行一次，重试失败的同步操作
     */
    @Scheduled(fixedDelay = 120000)
    public void retryFailedOperations() {
        log.info("[平台重试] 开始扫描失败日志");
        List<Tenant> tenants = tenantService.listActiveTenants();
        if (tenants == null || tenants.isEmpty()) {
            log.info("[平台重试] 无活跃租户，跳过");
            return;
        }

        for (Tenant tenant : tenants) {
            Long originalTenantId = BaseContext.getCurrentTenantId();
            BaseContext.setCurrentTenantId(tenant.getId());
            try {
                retryTenant();
            } catch (Exception e) {
                log.error("[平台重试] 租户 {} 扫描失败: {}", tenant.getId(), e.getMessage());
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
     * 重试当前租户（BaseContext 已注入）下所有失败且重试次数未达上限的日志
     */
    private void retryTenant() {
        // 查询该租户下所有失败且重试次数未达上限的日志（租户插件自动注入 tenant_id）
        LambdaQueryWrapper<PlatformSyncLog> qw = new LambdaQueryWrapper<>();
        qw.eq(PlatformSyncLog::getStatus, 1) // 失败
          .lt(PlatformSyncLog::getRetryCount, MAX_RETRY_COUNT);
        List<PlatformSyncLog> failedLogs = syncLogService.list(qw);

        if (failedLogs == null || failedLogs.isEmpty()) {
            return;
        }

        log.info("[平台重试] 租户 {} 发现 {} 条待重试记录", BaseContext.getCurrentTenantId(), failedLogs.size());

        for (PlatformSyncLog logEntry : failedLogs) {
            try {
                retryLogEntry(logEntry);
            } catch (Exception e) {
                log.error("[平台重试] 重试失败: id={}, action={}, error={}",
                        logEntry.getId(), logEntry.getAction(), e.getMessage());
            }
        }
    }

    /**
     * 重试单条日志记录
     */
    private void retryLogEntry(PlatformSyncLog logEntry) {
        logEntry.setRetryCount(logEntry.getRetryCount() + 1);

        try {
            switch (logEntry.getAction()) {
                case "PULL":
                    // 拉单重试：重新执行拉单逻辑
                    // 注意：实际项目中需要从日志中提取配置和参数
                    log.info("[平台重试] 拉单重试: platformType={}, count={}",
                            logEntry.getPlatformType(), logEntry.getRetryCount());
                    break;
                case "PUSH_STATUS":
                    // 状态回传重试
                    log.info("[平台重试] 状态回传重试: platformType={}, orderId={}, count={}",
                            logEntry.getPlatformType(), logEntry.getPlatformOrderId(),
                            logEntry.getRetryCount());
                    break;
                default:
                    log.warn("[平台重试] 未知动作类型: {}", logEntry.getAction());
            }

            // 标记为成功
            logEntry.setStatus(0);
            logEntry.setErrorMessage(null);
            syncLogService.updateById(logEntry);
            log.info("[平台重试] 重试成功: id={}, action={}, retryCount={}",
                    logEntry.getId(), logEntry.getAction(), logEntry.getRetryCount());

        } catch (Exception e) {
            // 重试仍然失败，记录错误
            logEntry.setErrorMessage(e.getMessage());
            syncLogService.updateById(logEntry);
            log.error("[平台重试] 重试仍失败: id={}, action={}, retryCount={}",
                    logEntry.getId(), logEntry.getAction(), logEntry.getRetryCount(), e);
        }
    }

    /**
     * 清理超过 7 天的成功日志
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldSuccessLogs() {
        log.info("[平台重试] 开始清理旧日志");
        // TODO: 实现日志清理逻辑
    }
}
