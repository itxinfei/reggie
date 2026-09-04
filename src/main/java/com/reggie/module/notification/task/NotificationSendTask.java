package com.reggie.module.notification.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.module.notification.mapper.NotificationRecordMapper;
import com.reggie.module.notification.model.NotificationRecord;
import com.reggie.module.notification.service.NotificationService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知定时发送调度任务
 * <p>
 * 修复 P0 缺陷：batchSend 传入未来 sendTime 时只建 status=0 待发送记录并"交由调度任务处理"，
 * 但 notification 模块此前不存在任何 @Scheduled 任务，导致定时通知永久滞留、消息静默丢失。
 * 本任务每 30 秒扫描一次"已到 sendTime 且 status=0"的记录，调用
 * {@link NotificationService#sendScheduledRecord} 补发（CAS 抢占 status 0->1 防多实例重复发送）。
 * 多租户遍历模式沿用 {@code PlatformPullTask}：逐租户设置 BaseContext，并 try/finally 恢复/清理，
 * 保证通知模板查询与租户上下文一致。
 * </p>
 *
 * @author reggie
 * @since 2026-09-04
 */
@Slf4j
@Component
public class NotificationSendTask {

    /** 每次扫描的单批记录上限：单实例执行发送，避免一次积压过多拉长单次扫描 */
    private static final int BATCH_SIZE = 100;

    @Autowired
    private NotificationRecordMapper recordMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TenantService tenantService;

    /**
     * 每 30 秒扫描一次待发送且已到点的通知记录并执行发送。
     */
    @Scheduled(fixedDelay = 30000)
    public void scanDueScheduledRecords() {
        List<Tenant> tenants = tenantService.listActiveTenants();
        if (tenants == null || tenants.isEmpty()) {
            return;
        }
        for (Tenant tenant : tenants) {
            try {
                processTenant(tenant.getId());
            } catch (Exception e) {
                log.error("[通知定时发送] 租户处理异常: tenantId={}", tenant.getId(), e);
            }
        }
    }

    private void processTenant(Long tenantId) {
        Long originalTenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        com.reggie.common.BaseContext.setCurrentTenantId(tenantId);
        try {
            LambdaQueryWrapper<NotificationRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NotificationRecord::getTenantId, tenantId)
                   .eq(NotificationRecord::getStatus, 0)
                   .isNotNull(NotificationRecord::getSendTime)
                   .le(NotificationRecord::getSendTime, LocalDateTime.now())
                   .orderByAsc(NotificationRecord::getSendTime)
                   .last("LIMIT " + BATCH_SIZE);
            List<NotificationRecord> dueRecords = recordMapper.selectList(wrapper);
            if (dueRecords == null || dueRecords.isEmpty()) {
                return;
            }
            for (NotificationRecord record : dueRecords) {
                try {
                    notificationService.sendScheduledRecord(record);
                } catch (Exception e) {
                    // sendScheduledRecord 内部已捕获异常；此处兜底避免单条记录异常中断本租户后续记录
                    log.error("[通知定时发送] 记录处理异常: recordId={}, tenantId={}", record.getId(), tenantId, e);
                }
            }
        } finally {
            if (originalTenantId != null) {
                com.reggie.common.BaseContext.setCurrentTenantId(originalTenantId);
            } else {
                com.reggie.common.BaseContext.remove();
            }
        }
    }
}
