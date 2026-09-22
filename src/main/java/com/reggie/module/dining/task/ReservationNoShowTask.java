package com.reggie.module.dining.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.service.ReservationService;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import com.reggie.enums.ReservationStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预订 no-show（客人未到店）自动过期定时任务。
 * 每 10 分钟扫描一次，对预订时间已过宽限期仍为待确认/已确认的预订自动取消；
 * 已确认预订占用的桌台随之释放，避免店员忘记取消导致桌台长期停在预留态影响翻台。
 *
 * @author reggie
 * @since 2026-09-22
 */
@Slf4j
@Component
public class ReservationNoShowTask {

    /** 宽限期（分钟）：预订时间过后再容忍 15 分钟，迟到顾客不被立即取消 */
    private static final int GRACE_MINUTES = 15;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private TenantService tenantService;

    /**
     * 每 10 分钟扫描一次过期未到店的预订
     */
    @Scheduled(fixedDelay = 600000) // 10分钟
    public void autoExpireNoShow() {
        try {
            List<Tenant> tenants = tenantService.list();
            if (tenants == null || tenants.isEmpty()) {
                return;
            }
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(GRACE_MINUTES);
            int totalExpired = 0;

            for (Tenant tenant : tenants) {
                try {
                    BaseContext.setCurrentTenantId(tenant.getId());
                    // 预订时间早于截止时间且仍未到店/未取消
                    List<Reservation> expired = reservationService.list(
                            new LambdaQueryWrapper<Reservation>()
                                    .in(Reservation::getStatus,
                                            ReservationStatus.PENDING.getValue(),
                                            ReservationStatus.CONFIRMED.getValue())
                                    .lt(Reservation::getReservedTime, cutoff));
                    if (expired == null || expired.isEmpty()) {
                        continue;
                    }
                    for (Reservation r : expired) {
                        // 复用取消逻辑：CONFIRMED 预订会在同一事务内把桌台还原为空闲
                        reservationService.cancelReservation(r.getId());
                        totalExpired++;
                    }
                    log.info("[预订过期] 租户 {} 自动取消 {} 条 no-show 预订",
                            tenant.getId(), expired.size());
                } catch (Exception e) {
                    log.error("[预订过期] 租户 {} 处理异常: {}", tenant.getId(), e.getMessage(), e);
                } finally {
                    BaseContext.remove();
                }
            }
            if (totalExpired > 0) {
                log.info("[预订过期] 本轮共自动取消 {} 条 no-show 预订（宽限 {} 分钟）",
                        totalExpired, GRACE_MINUTES);
            }
        } catch (Exception e) {
            log.error("[预订过期] 定时任务执行异常: {}", e.getMessage(), e);
        }
    }
}
