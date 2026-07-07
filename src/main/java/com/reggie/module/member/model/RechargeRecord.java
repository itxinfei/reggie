package com.reggie.module.member.model;

import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RechargeRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long tenantId;
    private Long memberId;
    private BigDecimal amount;
    private BigDecimal giftAmount;
    private String paymentMethod;
    private LocalDateTime createdTime;
}
