package com.reggie.module.payment.channel;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 支付请求参数
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class PayRequest {
    /** 交易号 */
    private String tradeNo;
    /** 支付金额 */
    private BigDecimal amount;
    /** 商品标题 */
    private String subject;
    /** 商品描述 */
    private String description;
    /** 支付超时时间（分钟） */
    private Integer timeoutMinutes;
}
