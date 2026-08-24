package com.reggie.module.marketing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 满减活动规则实体
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("full_reduction_rule")
@Schema(description = "满减活动规则")
public class FullReductionRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 优惠类型 - 减固定金额 */
    public static final int TYPE_REDUCE_AMOUNT = 1;
    /** 优惠类型 - 打折 */
    public static final int TYPE_DISCOUNT = 2;
    /** 优惠类型 - 赠品 */
    public static final int TYPE_GIFT = 3;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "活动ID")
    @NotNull(message = "活动ID不能为空")
    private Long campaignId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "优惠类型：1-减固定金额，2-打折，3-赠品")
    @NotNull(message = "优惠类型不能为空")
    private Integer discountType;

    @Schema(description = "满多少金额")
    @NotNull(message = "满减金额不能为空")
    private BigDecimal minAmount;

    @Schema(description = "优惠值（减金额/折扣率）")
    @NotNull(message = "优惠值不能为空")
    private BigDecimal discountValue;

    @Schema(description = "最大优惠金额（折扣类型使用）")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "赠品菜品ID（赠品类型使用）")
    private Long giftDishId;

    @Schema(description = "赠品数量")
    private Integer giftQuantity;

    @Schema(description = "是否可叠加使用：0-否，1-是")
    private Integer stackable;

    @Schema(description = "每日限用次数")
    private Integer dailyLimit;

    @Schema(description = "每人限用次数")
    private Integer perUserLimit;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "租户ID")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private Long createUser;

    @Schema(description = "更新人")
    private Long updateUser;
}
