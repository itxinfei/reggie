package com.reggie.module.payment.channel.sdk;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.reggie.common.CustomException;
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.reggie.module.payment.util.PaymentCredentialEncryptor;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝开放平台 SDK 构建器。
 * <p>
 * 读取数据库租户配置，解密应用私钥，使用「公钥模式」（APPID + 应用私钥 + 支付宝公钥）
 * 初始化 {@link AlipayClient}，签名算法固定 RSA2。客户端线程安全，由工厂按「租户 + 渠道」缓存复用。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
public final class AlipaySdkBuilder {

    private static final String FORMAT_JSON = "json";
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String SIGN_TYPE_RSA2 = "RSA2";
    private static final String DEFAULT_GATEWAY = "https://openapi.alipay.com/gateway.do";

    private AlipaySdkBuilder() {
    }

    /**
     * 依据配置构建支付宝客户端。
     *
     * @param config  数据库中的支付宝渠道配置
     * @param gateway 网关地址（为空用正式网关，沙箱联调可传入沙箱网关）
     * @return 支付宝客户端
     * @throws CustomException 配置缺失、解密失败时抛出（工厂不缓存负值）
     */
    public static AlipayClient build(PaymentChannelConfig config, String gateway) {
        if (config == null) {
            throw new CustomException("支付宝渠道配置为空，无法初始化");
        }
        String appId = safeTrim(config.getAliAppId());
        if (appId.isEmpty()) {
            throw new CustomException("支付宝缺少应用 APPID，无法初始化");
        }
        String privateKey = PaymentCredentialEncryptor.decrypt(config.getAliPrivateKey());
        if (isBlank(privateKey)) {
            throw new CustomException("支付宝应用私钥缺失（解密失败），无法初始化");
        }
        String publicKey = safeTrim(config.getAliPublicKey());
        if (publicKey.isEmpty()) {
            throw new CustomException("缺少支付宝公钥，无法初始化");
        }

        String serverUrl = isBlank(gateway) ? DEFAULT_GATEWAY : gateway.trim();

        log.info("[支付宝] SDK 初始化成功 tenantId={}, appId={}, gateway={}",
                config.getTenantId(), appId, serverUrl);
        return new DefaultAlipayClient(serverUrl, appId, stripKeyPem(privateKey),
                FORMAT_JSON, CHARSET_UTF8, stripKeyPem(publicKey), SIGN_TYPE_RSA2);
    }

    /**
     * 去掉 PEM 头尾标记与所有空白，得到纯 Base64。
     * 支付宝密钥通常为一行 PKCS8 Base64；兼容用户误粘贴含 BEGIN/END 与换行的 PEM。
     */
    private static String stripKeyPem(String key) {
        if (key == null) {
            return "";
        }
        String content = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "");
        return content.replaceAll("\\s+", "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
