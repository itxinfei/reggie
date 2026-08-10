package com.reggie.module.finance.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reconciliation Statement Entity
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("reconciliation_statement")
@Schema(description = "Reconciliation Statement")
public class ReconciliationStatement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Status - Unreconciled */
    public static final int STATUS_UNRECONCILED = 0;
    /** Status - Reconciled */
    public static final int STATUS_RECONCILED = 1;
    /** Status - Discrepancy */
    public static final int STATUS_DISCREPANCY = 2;

    @Schema(description = "Primary Key ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "Statement Number")
    private String statementNo;

    @Schema(description = "Statement Date")
    private LocalDate statementDate;

    @Schema(description = "Platform: all, wechat, alipay, bank")
    private String platform;

    @Schema(description = "System Amount")
    private BigDecimal systemAmount;

    @Schema(description = "Platform Amount")
    private BigDecimal platformAmount;

    @Schema(description = "Difference Amount")
    private BigDecimal differenceAmount;

    @Schema(description = "Order Count")
    private Integer orderCount;

    @Schema(description = "Refund Amount")
    private BigDecimal refundAmount;

    @Schema(description = "Refund Count")
    private Integer refundCount;

    @Schema(description = "Fee Amount")
    private BigDecimal feeAmount;

    @Schema(description = "Net Amount")
    private BigDecimal netAmount;

    @Schema(description = "Status: 0-Unreconciled, 1-Reconciled, 2-Discrepancy")
    private Integer status;

    @Schema(description = "Reconcile Time")
    private LocalDateTime reconcileTime;

    @Schema(description = "Reconcile User ID")
    private Long reconcileUserId;

    @Schema(description = "Reconcile User Name")
    private String reconcileUserName;

    @Schema(description = "Remark")
    private String remark;

    @Schema(description = "Tenant ID")
    private Long tenantId;

    @Schema(description = "Create Time")
    private LocalDateTime createTime;

    @Schema(description = "Update Time")
    private LocalDateTime updateTime;
}
