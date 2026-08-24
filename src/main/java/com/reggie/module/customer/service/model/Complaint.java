package com.reggie.module.customer.service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Complaint Entity
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("complaint")
@Schema(description = "Complaint")
public class Complaint implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Status - Pending */
    public static final int STATUS_PENDING = 0;
    /** Status - Processing */
    public static final int STATUS_PROCESSING = 1;
    /** Status - Resolved */
    public static final int STATUS_RESOLVED = 2;
    /** Status - Closed */
    public static final int STATUS_CLOSED = 3;

    /** Type - Food Quality */
    public static final int TYPE_FOOD_QUALITY = 1;
    /** Type - Delivery Service */
    public static final int TYPE_DELIVERY_SERVICE = 2;
    /** Type - Service Attitude */
    public static final int TYPE_SERVICE_ATTITUDE = 3;
    /** Type - Price Issue */
    public static final int TYPE_PRICE_ISSUE = 4;
    /** Type - Other */
    public static final int TYPE_OTHER = 5;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Complaint Number")
    private String complaintNo;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "User Name")
    private String userName;

    @Schema(description = "User Phone")
    private String userPhone;

    @Schema(description = "Order ID")
    private Long orderId;

    @Schema(description = "Order Number")
    private String orderNumber;

    @Schema(description = "Complaint Type: 1-Food Quality, 2-Delivery, 3-Service, 4-Price, 5-Other")
    private Integer complaintType;

    @Schema(description = "Complaint Title")
    private String title;

    @Schema(description = "Complaint Content")
    private String content;

    @Schema(description = "Image URLs (comma separated)")
    private String imageUrls;

    @Schema(description = "Status: 0-Pending, 1-Processing, 2-Resolved, 3-Closed")
    private Integer status;

    @Schema(description = "Handler ID")
    private Long handlerId;

    @Schema(description = "Handler Name")
    private String handlerName;

    @Schema(description = "Handle Result")
    private String handleResult;

    @Schema(description = "Compensation Amount")
    private BigDecimal compensationAmount;

    @Schema(description = "Handle Time")
    private LocalDateTime handleTime;

    @Schema(description = "User Satisfaction: 1-Very Dissatisfied, 2-Dissatisfied, 3-Neutral, 4-Satisfied, 5-Very Satisfied")
    private Integer satisfaction;

    @Schema(description = "User Feedback")
    private String userFeedback;

    @Schema(description = "Tenant ID")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;

    @Schema(description = "Update Time")
    private LocalDateTime updateTime;
}
