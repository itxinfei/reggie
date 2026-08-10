package com.reggie.module.payment.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * 微信支付渠道适配器，实现与微信支付平台的交互逻辑。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Component
public class WechatPayChannel implements PaymentChannel {

    /**
     * 创建支付订单
     *
     * @param request 支付请求参数
     * @return 支付响应
     */
    @Override
    public PayResponse createOrder(PayRequest request) {
        log.info("WechatPay createOrder: tradeNo={}, amount={}, subject={}", request.getTradeNo(), request.getAmount(), request.getSubject());
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo("WECHAT_" + UUID.randomUUID().toString().replace("-", ""));
        response.setPayUrl("https://pay.weixin.qq.com/pay/" + response.getChannelTradeNo());
        response.setQrCodeUrl("https://qr.weixin.qq.com/" + response.getChannelTradeNo());
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
        log.info("WechatPay queryOrder: tradeNo={}", tradeNo);
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo("WECHAT_" + tradeNo);
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
        log.info("WechatPay refund: channelTradeNo={}, amount={}, reason={}", request.getChannelTradeNo(), request.getAmount(), request.getReason());
        RefundResponse response = new RefundResponse();
        response.setSuccess(true);
        response.setRefundChannelTradeNo("WECHAT_REFUND_" + UUID.randomUUID().toString().replace("-", ""));
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
        log.info("WechatPay handleNotify: params={}", params);
        PayResponse response = new PayResponse();
        response.setSuccess(true);
        response.setChannelTradeNo(params.get("trade_no"));
        return response;
    }

    /**
     * 校验微信支付回调签名。
     * <p>
     * STUB 实现：仅校验必要参数（out_trade_no、sign）存在性。生产环境必须替换为
     * {@code WxPayUtil.verifyNotifySign} 配合微信支付 API 密钥进行真实签名校验，
     * 严禁保留此恒真逻辑上线。
     * </p>
     *
     * @param params 回调参数
     * @return true=必要参数齐全（待替换为真实签名校验）；false=参数缺失
     */
    @Override
    public boolean verifyNotifySign(Map<String, String> params) {
        if (params == null) {
            return false;
        }
        String outTradeNo = params.get("out_trade_no");
        String sign = params.get("sign");
        if (outTradeNo == null || outTradeNo.trim().isEmpty() || sign == null || sign.trim().isEmpty()) {
            log.warn("WechatPay 回调签名校验失败：缺少 out_trade_no 或 sign 参数，params={}", params);
            return false;
        }
        // TODO(生产环境必须替换): 使用 WxPayUtil.verifyNotifySign 进行真实签名校验
        log.warn("WechatPay 回调签名校验为 STUB 实现，仅校验参数存在性，生产环境必须替换为真实签名校验");
        return true;
    }
}