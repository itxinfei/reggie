package com.reggie.module.customer_service.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Customer Service Session Entity
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("cs_session")
@Schema(description = "Customer Service Session")
public class CsSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Status - Waiting */
    public static final int STATUS_WAITING = 0;
    /** Status - In Progress */
    public static final int STATUS_IN_PROGRESS = 1;
    /** Status - Closed */
    public static final int STATUS_CLOSED = 2;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Session Number")
    private String sessionNo;

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "User Name")
    private String userName;

    @Schema(description = "Agent ID")
    private Long agentId;

    @Schema(description = "Agent Name")
    private String agentName;

    @Schema(description = "Session Type: 1-General, 2-Order, 3-Complaint")
    private Integer sessionType;

    @Schema(description = "Related Order ID")
    private Long orderId;

    @Schema(description = "Status: 0-Waiting, 1-In Progress, 2-Closed")
    private Integer status;

    @Schema(description = "First Response Time")
    private LocalDateTime firstResponseTime;

    @Schema(description = "Close Time")
    private LocalDateTime closeTime;

    @Schema(description = "Satisfaction Rating (1-5)")
    private Integer satisfactionRating;

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

