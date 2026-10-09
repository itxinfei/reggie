package com.reggie.module.payment.channel;

import lombok.Data;
import java.math.BigDecimal;

/**
 * <p>
 * 支付请求参数封装类。
 * </p>
 *
 * @author 心飞为你飞
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
    /** 支付方式：NATIVE扫码 / H5跳转 / PC网页；不传由渠道按场景默认 */
    private String payType;
    /** 客户端真实 IP（H5 支付风控需要，由控制器从请求头/远端地址解析，不采信前端入参） */
    private String clientIp;
}
