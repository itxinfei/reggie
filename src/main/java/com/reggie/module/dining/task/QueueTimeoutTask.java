package com.reggie.module.dining.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.module.dining.service.QueueService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 排队超时自动取消定时任务
 * 每 5 分钟扫描一次，对等待超过 30 分钟的排队记录自动标记为已取消。
 * 避免顾客离场不取消导致队列永远卡住，影响排队管理和统计准确性。
 *
 * @author reggie
 * @since 2026-09-18
 */
@Slf4j
@Component
public class QueueTimeoutTask {

    /** 等待超时阈值（分钟） */
    private static final int TIMEOUT_MINUTES = 30;

    @Autowired
    private QueueService queueService;

    @Autowired
    private TenantService tenantService;

    /**
     * 每 5 分钟扫描一次超时排队记录
     */
    @Scheduled(fixedDelay = 300000) // 5分钟
    public void autoCancelTimeout() {
        try {
            List<Tenant> tenants = tenantService.list();
            if (tenants == null || tenants.isEmpty()) return;

            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
            int totalCancelled = 0;

            for (Tenant tenant : tenants) {
                try {
                    BaseContext.setCurrentTenantId(tenant.getId());
                    // 查询等待超时的记录
                    List<QueueRecord> timeoutRecords = queueService.list(
                        new LambdaQueryWrapper<QueueRecord>()
                            .eq(QueueRecord::getStatus, "WAITING")
                            .lt(QueueRecord::getCreatedTime, cutoff)
                    );
                    if (timeoutRecords == null || timeoutRecords.isEmpty()) continue;

                    for (QueueRecord record : timeoutRecords) {
                        record.setStatus("CANCELLED");
                        record.setUpdateTime(LocalDateTime.now());
                    }
                    boolean success = queueService.updateBatchById(timeoutRecords);
                    if (success) {
                        totalCancelled += timeoutRecords.size();
                        if (!timeoutRecords.isEmpty()) {
                            log.info("[排队超时] 租户 {} 自动取消 {} 条超时排队记录",
                                tenant.getId(), timeoutRecords.size());
                        }
                    }
                } catch (Exception e) {
                    log.error("[排队超时] 租户 {} 处理异常: {}", tenant.getId(), e.getMessage(), e);
                } finally {
                    // 修改点(2026-09-18)：BaseContext 无 removeCurrentTenantId() 方法，
                    // 统一用 remove() 清理线程上下文（原调用导致该类无法编译，阻断整个工程构建）
                    BaseContext.remove();
                }
            }
            if (totalCancelled > 0) {
                log.info("[排队超时] 本轮共自动取消 {} 条超时排队记录（阈值 {} 分钟）",
                    totalCancelled, TIMEOUT_MINUTES);
            }
        } catch (Exception e) {
            log.error("[排队超时] 定时任务执行异常: {}", e.getMessage(), e);
        }
    }
}
