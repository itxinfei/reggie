package com.reggie.module.billing.vo;

import com.reggie.module.billing.model.TenantSubscription;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 租户当前订阅权益视图（用于前端展示与功能准入判断）
 *
 * @author reggie
 * @since 2026-09-12
 */
@Data
@Schema(description = "租户当前订阅权益")
public class SubscriptionEntitlementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "是否拥有有效订阅")
    private boolean active;

    @Schema(description = "订阅记录ID")
    private Long subscriptionId;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "套餐编码")
    private String planCode;

    @Schema(description = "套餐名称")
    private String planName;

    @Schema(description = "订阅状态：0=待支付，1=生效中，2=已过期，3=已取消")
    private Integer status;

    @Schema(description = "生效开始时间")
    private LocalDateTime startTime;

    @Schema(description = "到期时间")
    private LocalDateTime endTime;

    @Schema(description = "剩余天数（不足一天按0计；已过期为负数或0）")
    private Long daysRemaining;

    @Schema(description = "门店数上限（-1 不限）")
    private Integer maxStores;

    @Schema(description = "员工数上限（-1 不限）")
    private Integer maxEmployees;

    /**
     * 由订阅记录构造权益视图。
     *
     * @param sub 订阅记录，可为空（无订阅时返回 active=false）
     * @param now 当前时间
     * @return 权益视图
     */
    public static SubscriptionEntitlementVO from(TenantSubscription sub, LocalDateTime now) {
        SubscriptionEntitlementVO vo = new SubscriptionEntitlementVO();
        if (sub == null) {
            vo.setActive(false);
            vo.setDaysRemaining(0L);
            return vo;
        }
        vo.setSubscriptionId(sub.getId());
        vo.setTenantId(sub.getTenantId());
        vo.setPlanCode(sub.getPlanCode());
        vo.setPlanName(sub.getPlanName());
        vo.setStatus(sub.getStatus());
        vo.setStartTime(sub.getStartTime());
        vo.setEndTime(sub.getEndTime());
        vo.setMaxStores(sub.getMaxStores());
        vo.setMaxEmployees(sub.getMaxEmployees());
        boolean isActive = Integer.valueOf(TenantSubscription.STATUS_ACTIVE).equals(sub.getStatus())
                && sub.getStartTime() != null && sub.getEndTime() != null
                && !now.isBefore(sub.getStartTime()) && now.isBefore(sub.getEndTime());
        vo.setActive(isActive);
        if (sub.getEndTime() != null) {
            vo.setDaysRemaining(ChronoUnit.DAYS.between(now.toLocalDate(), sub.getEndTime().toLocalDate()));
        } else {
            vo.setDaysRemaining(0L);
        }
        return vo;
    }
}
