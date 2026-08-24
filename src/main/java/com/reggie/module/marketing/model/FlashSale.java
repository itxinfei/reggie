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
 * Flash Sale Entity
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("flash_sale")
@Schema(description = "Flash Sale")
public class FlashSale implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Activity Name")
    @NotNull(message = "Activity name is required")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Dish ID")
    @NotNull(message = "Dish is required")
    private Long dishId;

    @Schema(description = "Dish Name")
    private String dishName;

    @Schema(description = "Original Price")
    private BigDecimal originalPrice;

    @Schema(description = "Flash Sale Price")
    @NotNull(message = "Flash sale price is required")
    private BigDecimal flashPrice;

    @Schema(description = "Total Quantity")
    @NotNull(message = "Total quantity is required")
    private Integer totalQuantity;

    @Schema(description = "Sold Quantity")
    private Integer soldQuantity;

    @Schema(description = "Max Per User")
    private Integer maxPerUser;

    @Schema(description = "Start Time")
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @Schema(description = "End Time")
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @Schema(description = "Status: 0-Draft, 1-Active, 2-Paused, 3-Ended")
    private Integer status;

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
