package com.reggie.module.payment.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RefundRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long paymentOrderId;
    private Long tenantId;
    private String refundNo;
    private BigDecimal amount;
    private String reason;
    private String status;
    private LocalDateTime createdTime;
}
