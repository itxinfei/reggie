package com.reggie.module.kds.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.kds.model.KitchenTicket;
import com.reggie.module.kds.service.KitchenTicketService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * KDS 后厨超时自动升级任务。
 * <p>
 * 每 2 分钟扫描一次，将制作中（STATUS_COOKING）且已超过阈值时间的工单自动标记为加急。
 * 阈值默认 15 分钟，可通过 kitchen.cooking-timeout-minutes 配置。
 * </p>
 *
 * @author reggie
 * @since 2026-09-15
 */
@Slf4j
@Component
public class KdsTimeoutTask {

    /** 默认制作超时阈值（分钟） */
    private static final int DEFAULT_COOKING_TIMEOUT_MINUTES = 15;

    @Autowired
    private KitchenTicketService kitchenTicketService;

    @Autowired
    private TenantService tenantService;

    /**
     * 每 2 分钟扫描制作中超时工单，自动标记加急。
     */
    @Scheduled(fixedRate = 120000, initialDelay = 60000)
    public void autoUpgradeOverdueCooking() {
        List<Tenant> tenants = tenantService.list();
        if (tenants == null || tenants.isEmpty()) {
            return;
        }
        int timeoutMinutes = DEFAULT_COOKING_TIMEOUT_MINUTES;
        for (Tenant tenant : tenants) {
            try {
                BaseContext.setCurrentTenantId(tenant.getId());
                List<KitchenTicket> overdue = kitchenTicketService.list(
                        new LambdaQueryWrapper<KitchenTicket>()
                                .eq(KitchenTicket::getStatus, KitchenTicket.STATUS_COOKING)
                                .eq(KitchenTicket::getUrgent, KitchenTicket.URGENT_NO)
                                .le(KitchenTicket::getCookStartTime,
                                        LocalDateTime.now().minusMinutes(timeoutMinutes))
                );
                if (overdue.isEmpty()) {
                    continue;
                }
                for (KitchenTicket ticket : overdue) {
                    ticket.setUrgent(KitchenTicket.URGENT_YES);
                    ticket.setUpdateTime(LocalDateTime.now());
                }
                kitchenTicketService.updateBatchById(overdue);
                log.warn("[KDS超时升级] 租户{}：{}张制作中超时工单自动标记加急（阈值{}分钟）",
                        tenant.getId(), overdue.size(), timeoutMinutes);
            } catch (Exception e) {
                log.error("[KDS超时升级] 租户{}处理异常：{}", tenant.getId(), e.getMessage(), e);
            } finally {
                BaseContext.remove();
            }
        }
    }
}