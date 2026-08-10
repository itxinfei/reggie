package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Delivery Time Record Entity
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("delivery_time_record")
@Schema(description = "Delivery Time Record")
public class DeliveryTimeRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Order Number")
    private String orderNumber;

    @Schema(description = "Rider ID")
    private Long riderId;

    @Schema(description = "Rider Name")
    private String riderName;

    @Schema(description = "Order Time")
    private LocalDateTime orderTime;

    @Schema(description = "Accept Time")
    private LocalDateTime acceptTime;

    @Schema(description = "Pickup Time")
    private LocalDateTime pickupTime;

    @Schema(description = "Deliver Time")
    private LocalDateTime deliverTime;

    @Schema(description = "Estimated Delivery Time (minutes)")
    private Integer estimatedMinutes;

    @Schema(description = "Actual Delivery Time (minutes)")
    private Integer actualMinutes;

    @Schema(description = "Distance (meters)")
    private BigDecimal distance;

    @Schema(description = "Delivery Status: 0-Pending, 1-Accepted, 2-Picked up, 3-Delivering, 4-Delivered, 5-Cancelled")
    private Integer status;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;

    @Schema(description = "Update Time")
    private LocalDateTime updateTime;
}
