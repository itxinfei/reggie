package com.reggie.module.payment.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long orderId;
    private Long tenantId;
    private String tradeNo;
    private String channelTradeNo;
    private String channel;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidTime;
    private LocalDateTime notifyTime;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
