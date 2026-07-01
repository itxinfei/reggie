package com.reggie.module.payment.channel;

import lombok.Data;

@Data
public class PayResponse {
    private boolean success;
    private String channelTradeNo;
    private String payUrl;
    private String qrCodeUrl;
    private String rawResponse;
    private String errorMsg;
}
