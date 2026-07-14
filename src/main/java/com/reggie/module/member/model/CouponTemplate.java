package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("coupon_template")
@Schema(description = "优惠券模板")
public class CouponTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "模板ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "模板名称", example = "新人满减券")
    private String name;

    @Schema(description = "优惠券类型：1=满减券，2=折扣券，3=代金券", example = "1")
    private String type;

    @Schema(description = "满减条件金额（元），满减券必填", example = "50.00")
    private BigDecimal conditionAmount;

    @Schema(description = "优惠金额（元），满减券/代金券必填", example = "10.00")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率（如8.5折=0.85），折扣券必填", example = "0.85")
    private BigDecimal discountRate;

    @Schema(description = "发放总数", example = "1000")
    private Integer totalCount;

    @Schema(description = "剩余可领数量", example = "500")
    private Integer remainCount;

    @Schema(description = "有效天数（领取后N天内有效）", example = "30")
    private Integer validDays;

    @Schema(description = "状态：0=禁用，1=启用", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    private LocalDateTime updatedTime;
}
