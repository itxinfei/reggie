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
 * Buy Get Free Entity
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("buy_get_free")
@Schema(description = "Buy Get Free Activity")
public class BuyGetFree implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Activity Name")
    @NotNull(message = "Activity name is required")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Buy Quantity")
    @NotNull(message = "Buy quantity is required")
    private Integer buyQuantity;

    @Schema(description = "Get Quantity")
    @NotNull(message = "Get quantity is required")
    private Integer getQuantity;

    @Schema(description = "Applicable Dish ID (null for all dishes)")
    private Long dishId;

    @Schema(description = "Applicable Setmeal ID (null for all setmeals)")
    private Long setmealId;

    @Schema(description = "Gift Dish ID")
    @NotNull(message = "Gift dish is required")
    private Long giftDishId;

    @Schema(description = "Gift Dish Name")
    private String giftDishName;

    @Schema(description = "Minimum Order Amount")
    private BigDecimal minOrderAmount;

    @Schema(description = "Max Times Per Order")
    private Integer maxTimesPerOrder;

    @Schema(description = "Start Time")
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @Schema(description = "End Time")
    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @Schema(description = "Status: 0-Draft, 1-Active, 2-Paused, 3-Ended")
    private Integer status;

    @Schema(description = "Current Usage Count")
    private Integer usageCount;

    @Schema(description = "Tenant ID")
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
