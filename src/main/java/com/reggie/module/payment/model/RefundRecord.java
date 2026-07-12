package com.reggie.module.payment.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款记录实体类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class RefundRecord implements Serializable {
    /** 序列化版本UID */
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;
    /** 支付订单ID */
    private Long paymentOrderId;
    /** 租户ID */
    private Long tenantId;
    /** 退款流水号 */
    private String refundNo;
    /** 退款金额 */
    private BigDecimal amount;
    /** 退款原因 */
    private String reason;
    /** 退款状态 */
    private String status;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
