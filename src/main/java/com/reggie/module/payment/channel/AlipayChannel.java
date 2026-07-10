package com.reggie.module.payment.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

/**
 * 支付宝支付渠道适配器
 * 实现与支付宝支付平台的交互逻辑
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Component
public class AlipayChannel implements PaymentChannel {

    /** 交易号前缀 */
    private static final String TRADE_NO_PREFIX = "ALIPAY_";
    /** 退款号前缀 */
    private static final String REFUND_PREFIX = "ALIPAY_REFUND_";
    /** 支付URL前缀 */
    private static final String PAY_URL_PREFIX = "https://pay.alipay.com/pay/";
    /** 二维码URL前缀 */
    private static final String QR_CODE_URL_PREFIX = "https://qr.alipay.com/";

    /**
     * 创建支付订单
     *
     * @param request 支付请求参数
     * @return 支付响应
     */
    @Override
    public PayResponse createOrder(PayRequest request) {
        log.info("Alipay createOrder: tradeNo={}, amount={}, subject={}", request.getTradeNo(), request.getAmount(), request.getSubject());
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo(TRADE_NO_PREFIX + UUID.randomUUID().toString().replace("-", ""));
        response.setPayUrl(PAY_URL_PREFIX + response.getChannelTradeNo());
        response.setQrCodeUrl(QR_CODE_URL_PREFIX + response.getChannelTradeNo());
        return response;
    }

    /**
     * 查询订单状态
     *
     * @param tradeNo 交易号
     * @return 支付响应
     */
    @Override
    public PayResponse queryOrder(String tradeNo) {
        log.info("Alipay queryOrder: tradeNo={}", tradeNo);
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo(TRADE_NO_PREFIX + tradeNo);
        return response;
    }

    /**
     * 退款
     *
     * @param request 退款请求参数
     * @return 退款响应
     */
    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("Alipay refund: channelTradeNo={}, amount={}, reason={}", request.getChannelTradeNo(), request.getAmount(), request.getReason());
        RefundResponse response = new RefundResponse();
        response.setSuccess(true);
        response.setRefundChannelTradeNo(REFUND_PREFIX + UUID.randomUUID().toString().replace("-", ""));
        return response;
    }

    /**
     * 处理支付回调通知
     *
     * @param params 回调参数
     * @return 支付响应
     */
    @Override
    public PayResponse handleNotify(Map<String, String> params) {
        log.info("Alipay handleNotify: params={}", params);
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo(params.get("trade_no"));
        return response;
    }
}
