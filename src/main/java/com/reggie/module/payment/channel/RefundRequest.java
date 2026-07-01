package com.reggie.module.payment.channel;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RefundRequest {
    private String channelTradeNo;
    private BigDecimal amount;
    private String reason;
}
