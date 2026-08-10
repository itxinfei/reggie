package com.reggie.module.marketing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 营销活动使用记录实体
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("campaign_usage_record")
@Schema(description = "营销活动使用记录")
public class CampaignUsageRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "活动ID")
    private Long campaignId;

    @Schema(description = "规则ID（满减规则/折扣规则）")
    private Long ruleId;

    @Schema(description = "规则类型：1-满减，2-折扣")
    private Integer ruleType;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单号")
    private String orderNumber;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "订单金额")
    private BigDecimal orderAmount;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额")
    private BigDecimal actualAmount;

    @Schema(description = "使用时间")
    private LocalDateTime useTime;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
