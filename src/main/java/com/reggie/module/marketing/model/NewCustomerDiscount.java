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
 * New Customer Discount Entity
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("new_customer_discount")
@Schema(description = "New Customer Discount")
public class NewCustomerDiscount implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Discount Type - Fixed Amount */
    public static final int TYPE_FIXED = 1;
    /** Discount Type - Percentage */
    public static final int TYPE_PERCENTAGE = 2;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Discount Name")
    @NotNull(message = "Discount name is required")
    private String name;

    @Schema(description = "Discount Type: 1-Fixed Amount, 2-Percentage")
    @NotNull(message = "Discount type is required")
    private Integer discountType;

    @Schema(description = "Discount Value (amount or percentage)")
    @NotNull(message = "Discount value is required")
    private BigDecimal discountValue;

    @Schema(description = "Maximum Discount Amount (for percentage type)")
    private BigDecimal maxDiscountAmount;

    @Schema(description = "Minimum Order Amount")
    private BigDecimal minOrderAmount;

    @Schema(description = "Valid Days After Registration")
    private Integer validDays;

    @Schema(description = "Status: 0-Disabled, 1-Enabled")
    private Integer status;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Tenant ID")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;

    @Schema(description = "Update Time")
    private LocalDateTime updateTime;

    @Schema(description = "Create User")
    private Long createUser;

    @Schema(description = "Update User")
    private Long updateUser;
}
