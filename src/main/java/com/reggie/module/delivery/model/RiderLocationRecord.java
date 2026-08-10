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
 * Rider Location Record Entity
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("rider_location_record")
@Schema(description = "Rider Location Record")
public class RiderLocationRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Rider ID")
    private Long riderId;

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Longitude")
    private BigDecimal longitude;

    @Schema(description = "Latitude")
    private BigDecimal latitude;

    @Schema(description = "Speed (km/h)")
    private BigDecimal speed;

    @Schema(description = "Direction (degrees)")
    private BigDecimal direction;

    @Schema(description = "Record Time")
    private LocalDateTime recordTime;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;
}
