package com.reggie.module.marketing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 折扣活动规则实体
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("discount_rule")
@Schema(description = "折扣活动规则")
public class DiscountRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 折扣范围 - 全场 */
    public static final int SCOPE_ALL = 1;
    /** 折扣范围 - 指定分类 */
    public static final int SCOPE_CATEGORY = 2;
    /** 折扣范围 - 指定菜品 */
    public static final int SCOPE_DISH = 3;
    /** 折扣范围 - 指定套餐 */
    public static final int SCOPE_SETMEAL = 4;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "活动ID")
    @NotNull(message = "活动ID不能为空")
    private Long campaignId;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "折扣范围：1-全场，2-指定分类，3-指定菜品，4-指定套餐")
    @NotNull(message = "折扣范围不能为空")
    private Integer scope;

    @Schema(description = "折扣率（如8折传0.8）")
    @NotNull(message = "折扣率不能为空")
    private BigDecimal discountRate;

    @Schema(description = "最大优惠金额")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "最低消费金额")
    private BigDecimal minConsumption;

    @Schema(description = "适用分类ID（scope=2时使用）")
    private Long categoryId;

    @Schema(description = "适用菜品ID（scope=3时使用）")
    private Long dishId;

    @Schema(description = "适用套餐ID（scope=4时使用）")
    private Long setmealId;

    @Schema(description = "每日限用次数")
    private Integer dailyLimit;

    @Schema(description = "每人限用次数")
    private Integer perUserLimit;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "租户ID")
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
