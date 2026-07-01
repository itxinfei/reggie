package com.reggie.module.payment.channel;

import lombok.Data;

@Data
public class RefundResponse {
    private boolean success;
    private String refundChannelTradeNo;
    private String errorMsg;
}
