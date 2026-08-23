package com.reggie.module.payment;

import com.reggie.module.payment.channel.AlipayChannel;
import com.reggie.module.payment.channel.WechatPayChannel;
import com.reggie.module.payment.config.PaymentConfigProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 支付渠道回调签名校验单元测试。
 * <p>
 * 验证 verifyNotifySign 从 STUB 恒真实现升级为真实签名校验后：
 * <ul>
 *   <li>mock-mode=true（开发）时跳过验签；</li>
 *   <li>mock-mode=false（生产）+ 正确签名时校验通过；</li>
 *   <li>mock-mode=false + 篡改参数/缺失密钥时校验失败（fail-closed，杜绝恒真）。</li>
 * </ul>
 * </p>
 *
 * @author reggie
 * @since 2026-08-15
 */
class PaymentChannelSignTest {

    private PaymentConfigProperties config;
    private AlipayChannel alipayChannel;
    private WechatPayChannel wechatPayChannel;

    @BeforeEach
    void setUp() {
        config = new PaymentConfigProperties();
        alipayChannel = new AlipayChannel();
        wechatPayChannel = new WechatPayChannel();
        // 反射注入配置（避免依赖 Spring 容器）
        inject(alipayChannel, config);
        inject(wechatPayChannel, config);
    }

    private void inject(Object target, Object fieldValue) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField("paymentConfig");
            f.setAccessible(true);
            f.set(target, fieldValue);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 支付宝回调参数样例（key 故意乱序，验签应不依赖入参顺序） */
    private Map<String, String> alipayParams(PrivateKey privateKey) throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", "20260815001");
        params.put("trade_no", "2026081500001");
        params.put("total_amount", "99.99");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("app_id", "2021000000000001");
        params.put("seller_id", "2088000000000001");
        params.put("sign_type", "RSA2");

        // 待签串：剔除 sign/sign_type，其余按 key 字典序拼接 k1=v1&k2=v2
        StringBuilder content = new StringBuilder();
        new TreeMap<>(params).entrySet().stream()
                .filter(e -> !"sign".equals(e.getKey()) && !"sign_type".equals(e.getKey()))
                .forEach(e -> {
                    if (content.length() > 0) {
                        content.append("&");
                    }
                    content.append(e.getKey()).append("=").append(e.getValue());
                });
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(content.toString().getBytes(StandardCharsets.UTF_8));
        params.put("sign", Base64.getEncoder().encodeToString(sig.sign()));
        return params;
    }

    @Test
    void alipayMockModeSkipsVerification() {
        config.setMockMode(true);
        config.setAlipayPublicKey("");
        assertTrue(alipayChannel.verifyNotifySign(new LinkedHashMap<String, String>() {{
            put("out_trade_no", "20260815001");
            put("sign", "whatever");
        }}));
    }

    @Test
    void alipayValidSignaturePasses() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        config.setMockMode(false);
        config.setAlipayPublicKey(Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
        assertTrue(alipayChannel.verifyNotifySign(alipayParams(kp.getPrivate())));
    }

    @Test
    void alipayTamperedParamsFail() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();
        config.setMockMode(false);
        config.setAlipayPublicKey(Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
        Map<String, String> params = alipayParams(kp.getPrivate());
        params.put("total_amount", "1.00"); // 篡改金额
        assertFalse(alipayChannel.verifyNotifySign(params));
    }

    @Test
    void alipayMissingKeyFailClosed() {
        config.setMockMode(false);
        config.setAlipayPublicKey("");
        assertFalse(alipayChannel.verifyNotifySign(new LinkedHashMap<String, String>() {{
            put("out_trade_no", "20260815001");
            put("sign", "abc");
        }}));
    }

    /** 按微信 APIv2 规则计算 MD5 签名：剔除 sign，其余按 key 字典序拼接 + &key= */
    private String wechatSign(Map<String, String> params, String apiKey, String algo) throws Exception {
        StringBuilder content = new StringBuilder();
        new TreeMap<>(params).entrySet().stream()
                .filter(e -> !"sign".equals(e.getKey()))
                .forEach(e -> {
                    if (content.length() > 0) {
                        content.append("&");
                    }
                    content.append(e.getKey()).append("=").append(e.getValue());
                });
        content.append("&key=").append(apiKey);
        byte[] digest;
        if ("HMAC-SHA256".equalsIgnoreCase(algo)) {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            digest = mac.doFinal(content.toString().getBytes(StandardCharsets.UTF_8));
        } else {
            digest = java.security.MessageDigest.getInstance("MD5")
                    .digest(content.toString().getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString().toUpperCase();
    }

    @Test
    void wechatValidMd5SignaturePasses() throws Exception {
        config.setMockMode(false);
        config.setWechatApiKey("test_api_key_32_chars_123456789");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", "20260815002");
        params.put("transaction_id", "4200000000000000");
        params.put("total_fee", "9999");
        params.put("result_code", "SUCCESS");
        params.put("appid", "wx8888888888888888");
        params.put("mch_id", "1900000109");
        params.put("sign", wechatSign(params, config.getWechatApiKey(), "MD5"));
        assertTrue(wechatPayChannel.verifyNotifySign(params));
    }

    @Test
    void wechatTamperedParamsFail() throws Exception {
        config.setMockMode(false);
        config.setWechatApiKey("test_api_key_32_chars_123456789");
        Map<String, String> params = new LinkedHashMap<>();
        params.put("out_trade_no", "20260815003");
        params.put("total_fee", "9999");
        params.put("result_code", "SUCCESS");
        params.put("sign", wechatSign(params, config.getWechatApiKey(), "MD5"));
        params.put("total_fee", "1"); // 篡改金额
        assertFalse(wechatPayChannel.verifyNotifySign(params));
    }

    @Test
    void wechatMissingKeyFailClosed() {
        config.setMockMode(false);
        config.setWechatApiKey("");
        assertFalse(wechatPayChannel.verifyNotifySign(new LinkedHashMap<String, String>() {{
            put("out_trade_no", "20260815003");
            put("sign", "abc");
        }}));
    }
}
