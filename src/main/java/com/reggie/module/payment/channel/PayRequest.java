package com.reggie.module.payment.channel;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PayRequest {
    private String tradeNo;
    private BigDecimal amount;
    private String subject;
    private String description;
    private Integer timeoutMinutes;
}
