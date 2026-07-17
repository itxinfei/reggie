package com.reggie.module.member.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
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
    @NotBlank(message = "优惠券名称不能为空")
    private String name;

    @Schema(description = "优惠券类型：FULL_REDUCTION=满减券，DISCOUNT=折扣券，NEW_MEMBER=新客券", example = "FULL_REDUCTION")
    @NotBlank(message = "优惠券类型不能为空")
    private String type;

    @Schema(description = "满减条件金额（元），满减券必填", example = "50.00")
    private BigDecimal conditionAmount;

    @Schema(description = "优惠金额（元），满减券/代金券必填", example = "10.00")
    private BigDecimal discountAmount;

    @Schema(description = "折扣率（如8.5折=0.85），折扣券必填", example = "0.85")
    private BigDecimal discountRate;

    @Schema(description = "发放总数", example = "1000")
    @NotNull(message = "发放总数不能为空")
    @Min(value = 1, message = "发放总数必须大于0")
    private Integer totalCount;

    @Schema(description = "剩余可领数量", example = "500")
    private Integer remainCount;

    @Schema(description = "有效天数（领取后N天内有效）", example = "30")
    @NotNull(message = "有效天数不能为空")
    @Min(value = 1, message = "有效天数必须大于0")
    private Integer validDays;

    @Schema(description = "状态：0=禁用，1=启用", example = "1")
    private Integer status;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    @Schema(description = "是否删除：0=未删除，1=已删除", example = "0")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
