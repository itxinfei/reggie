package com.reggie.module.payment.channel;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 退款请求参数
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class RefundRequest {
    /** 渠道交易号 */
    private String channelTradeNo;
    /** 退款金额 */
    private BigDecimal amount;
    /** 退款原因 */
    private String reason;
}
