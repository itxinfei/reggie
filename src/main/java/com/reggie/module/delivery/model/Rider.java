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
 * Rider Entity
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("rider")
@Schema(description = "Rider")
public class Rider implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Status - Offline */
    public static final int STATUS_OFFLINE = 0;
    /** Status - Online */
    public static final int STATUS_ONLINE = 1;
    /** Status - Busy */
    public static final int STATUS_BUSY = 2;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Rider Name")
    private String name;

    @Schema(description = "Phone Number")
    private String phone;

    @Schema(description = "Avatar URL")
    private String avatar;

    @Schema(description = "Current Longitude")
    private BigDecimal currentLongitude;

    @Schema(description = "Current Latitude")
    private BigDecimal currentLatitude;

    @Schema(description = "Status: 0-Offline, 1-Online, 2-Busy")
    private Integer status;

    @Schema(description = "Current Order Count")
    private Integer currentOrderCount;

    @Schema(description = "Total Order Count")
    private Integer totalOrderCount;

    @Schema(description = "Rating")
    private BigDecimal rating;

    @Schema(description = "Last Location Update Time")
    private LocalDateTime lastLocationTime;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;

    @Schema(description = "Update Time")
    private LocalDateTime updateTime;
}
