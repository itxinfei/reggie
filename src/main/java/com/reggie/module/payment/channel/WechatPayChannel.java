package com.reggie.module.payment.channel;

import com.reggie.module.payment.config.PaymentConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    /** 支付配置（回调验签所需 API 密钥、签名类型、mock-mode 开关） */
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
     * 校验微信支付回调签名（APIv2）。
     * <p>
     * 真实校验：剔除 sign 后，其余参数按 key 字典序拼接
     * {@code k1=v1&k2=v2...&key=API密钥}，计算 MD5（或 HMAC-SHA256）摘要转大写与 sign 比对。
     * </p>
     * <p>
     * 安全开关：mock-mode=true（开发/演示）时跳过验签仅告警；
     * mock-mode=false（生产）时必须配置微信 API 密钥，否则<b>拒绝回调</b>（fail-closed），
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
            log.warn("WechatPay 回调签名校验失败：缺少 sign 参数，params={}", params);
            return false;
        }
        if (paymentConfig.isMockMode()) {
            log.warn("WechatPay 回调签名校验已跳过（mock-mode=true，仅限开发/演示），生产环境必须关闭 mock-mode 并配置微信 API 密钥");
            return true;
        }
        String apiKey = paymentConfig.getWechatApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.error("WechatPay 生产模式未配置微信 API 密钥（reggie.payment.wechat-api-key），拒绝回调（fail-closed）");
            return false;
        }
        try {
            // 1. 构建待签名串：剔除 sign，其余参数按 key 字典序拼接 k1=v1&k2=v2，末尾追加 &key=API密钥
            StringBuilder content = new StringBuilder();
            params.entrySet().stream()
                    .filter(e -> !"sign".equals(e.getKey()))
                    .filter(e -> e.getValue() != null && !e.getValue().trim().isEmpty())
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(e -> {
                        if (content.length() > 0) {
                            content.append("&");
                        }
                        content.append(e.getKey()).append("=").append(e.getValue());
                    });
            content.append("&key=").append(apiKey);

            // 2. 计算签名摘要
            byte[] digest;
            if ("HMAC-SHA256".equalsIgnoreCase(paymentConfig.getWechatSignType())) {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                digest = mac.doFinal(content.toString().getBytes(StandardCharsets.UTF_8));
            } else {
                MessageDigest md5 = MessageDigest.getInstance("MD5");
                digest = md5.digest(content.toString().getBytes(StandardCharsets.UTF_8));
            }
            String computed = bytesToHex(digest).toUpperCase();

            boolean ok = computed.equals(sign.trim().toUpperCase());
            if (!ok) {
                log.warn("WechatPay 回调签名校验失败（MD5/HMAC-SHA256 不通过），out_trade_no={}", params.get("out_trade_no"));
            }
            return ok;
        } catch (Exception e) {
            log.error("WechatPay 回调签名校验异常: {}", e.getMessage(), e);
            return false;
        }
    }

    /** 字节数组转十六进制字符串 */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}