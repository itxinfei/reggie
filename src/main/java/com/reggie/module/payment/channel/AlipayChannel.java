package com.reggie.module.payment.channel;

import com.reggie.module.payment.config.PaymentConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * 支付宝支付渠道适配器，实现与支付宝支付平台的交互逻辑。
 * </p>
 *
 * @author 心飞为你飞
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

    /** 支付配置（回调验签所需支付宝公钥、mock-mode 开关） */
    @Autowired
    private PaymentConfigProperties paymentConfig;

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

    /**
     * 校验支付宝回调签名（RSA2）。
     * <p>
     * 真实校验：剔除 sign、sign_type 后，其余参数按 key 字典序拼接
     * {@code k1=v1&k2=v2...}，用支付宝公钥做 SHA256withRSA 验签。
     * </p>
     * <p>
     * 安全开关：mock-mode=true（开发/演示）时跳过验签仅告警；
     * mock-mode=false（生产）时必须配置支付宝公钥，否则<b>拒绝回调</b>（fail-closed），
     * 严禁恒真放行。
     * </p>
     *
     * @param params 回调参数
     * @return true=签名校验通过；false=校验失败
     */
    @Override
    public boolean verifyNotifySign(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return false;
        }
        String sign = params.get("sign");
        if (sign == null || sign.trim().isEmpty()) {
            log.warn("Alipay 回调签名校验失败：缺少 sign 参数，params={}", params);
            return false;
        }
        if (paymentConfig.isMockMode()) {
            log.warn("Alipay 回调签名校验已跳过（mock-mode=true，仅限开发/演示），生产环境必须关闭 mock-mode 并配置支付宝公钥");
            return true;
        }
        String publicKey = paymentConfig.getAlipayPublicKey();
        if (publicKey == null || publicKey.trim().isEmpty()) {
            log.error("Alipay 生产模式未配置支付宝公钥（reggie.payment.alipay-public-key），拒绝回调（fail-closed）");
            return false;
        }
        try {
            // 1. 构建待验签串：剔除 sign、sign_type，其余参数按 key 字典序拼接 k1=v1&k2=v2
            StringBuilder content = new StringBuilder();
            params.entrySet().stream()
                    .filter(e -> !"sign".equals(e.getKey()) && !"sign_type".equals(e.getKey()))
                    .filter(e -> e.getValue() != null && !e.getValue().trim().isEmpty())
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(e -> {
                        if (content.length() > 0) {
                            content.append("&");
                        }
                        content.append(e.getKey()).append("=").append(e.getValue());
                    });
            // 2. 解码支付宝公钥（Base64 X.509）与签名
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(keySpec);
            byte[] signBytes = Base64.getDecoder().decode(sign);
            // 3. SHA256withRSA 验签
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(pubKey);
            signature.update(content.toString().getBytes(StandardCharsets.UTF_8));
            boolean ok = signature.verify(signBytes);
            if (!ok) {
                log.warn("Alipay 回调签名校验失败（RSA2 验签不通过），out_trade_no={}", params.get("out_trade_no"));
            }
            return ok;
        } catch (Exception e) {
            log.error("Alipay 回调签名校验异常: {}", e.getMessage(), e);
            return false;
        }
    }
}