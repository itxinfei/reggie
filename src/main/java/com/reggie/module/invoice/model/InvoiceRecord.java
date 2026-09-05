package com.reggie.module.invoice.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发票申请记录实体
 */
@Data
@TableName("invoice_record")
@Schema(description = "发票申请记录")
public class InvoiceRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPLIED = 1;
    public static final int STATUS_ISSUED = 2;
    public static final int STATUS_VOIDED = 3;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "申请用户ID（用户端归属列，防止跨用户越权查询）")
    private Long userId;

    @Schema(description = "订单号")
    private String orderNo;

    @Schema(description = "发票抬头ID")
    private Long titleId;

    @Schema(description = "发票抬头（冗余）")
    private String title;

    @Schema(description = "税号（冗余）")
    private String taxNumber;

    @Schema(description = "类型：1=个人，2=企业")
    private Integer type;

    @Schema(description = "开票金额")
    private BigDecimal amount;

    @Schema(description = "状态：0=待申请，1=已申请，2=已开具，3=已作废")
    private Integer status;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @Schema(description = "发票代码")
    private String invoiceCode;

    @Schema(description = "发票PDF地址")
    private String invoiceUrl;

    @Schema(description = "申请时间")
    private LocalDateTime applyTime;

    @Schema(description = "开具时间")
    private LocalDateTime issueTime;

    @Schema(description = "租户ID")
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}