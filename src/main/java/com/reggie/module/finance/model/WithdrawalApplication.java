package com.reggie.module.finance.model;

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
 * Withdrawal Application Entity
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("withdrawal_application")
@Schema(description = "Withdrawal Application")
public class WithdrawalApplication implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Status - Pending Review */
    public static final int STATUS_PENDING = 0;
    /** Status - Approved */
    public static final int STATUS_APPROVED = 1;
    /** Status - Paid */
    public static final int STATUS_PAID = 2;
    /** Status - Rejected */
    public static final int STATUS_REJECTED = 3;
    /** Status - Cancelled */
    public static final int STATUS_CANCELLED = 4;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Application Number")
    private String applicationNo;

    @Schema(description = "Applicant ID")
    private Long applicantId;

    @Schema(description = "Applicant Name")
    private String applicantName;

    @Schema(description = "Withdrawal Amount")
    @NotNull(message = "Withdrawal amount is required")
    private BigDecimal amount;

    @Schema(description = "Withdrawal Method: 1-Bank Card, 2-Alipay, 3-WeChat")
    private Integer withdrawMethod;

    @Schema(description = "Receive Account")
    private String receiveAccount;

    @Schema(description = "Receive Name")
    private String receiveName;

    @Schema(description = "Status: 0-Pending, 1-Approved, 2-Paid, 3-Rejected, 4-Cancelled")
    private Integer status;

    @Schema(description = "Reviewer ID")
    private Long reviewerId;

    @Schema(description = "Reviewer Name")
    private String reviewerName;

    @Schema(description = "Review Time")
    private LocalDateTime reviewTime;

    @Schema(description = "Review Remark")
    private String reviewRemark;

    @Schema(description = "Payment Time")
    private LocalDateTime paymentTime;

    @Schema(description = "Payment Number")
    private String paymentNo;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;

    @Schema(description = "Update Time")
    private LocalDateTime updateTime;
}
