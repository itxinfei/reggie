package com.reggie.module.payment.channel.sdk;

import com.reggie.common.CustomException;
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.reggie.module.payment.util.PaymentCredentialEncryptor;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.service.payments.h5.H5Service;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.refund.RefundService;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信支付 APIv3 SDK 构建器。
 * <p>
 * 读取数据库里的租户配置，解密 APIv3 密钥与商户私钥，按「平台证书自动更新」或「微信支付公钥」
 * 两种模式之一初始化 SDK，并组装下单（NATIVE/H5）与退款服务。
 * {@code RSAAutoCertificateConfig} 构建时会联网下载并校验微信平台证书，因此必须缓存复用，
 * 由 {@code PaymentChannelFactory} 按「租户 + 渠道」保证只构建一次。
 * </p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
public final class WechatSdkBuilder {

    private WechatSdkBuilder() {
    }

    /**
     * 依据配置构建微信 SDK 客户端集合。
     *
     * @param config 数据库中的微信渠道配置
     * @return SDK 客户端集合
     * @throws CustomException 配置缺失、解密失败或证书/网络初始化失败时抛出（工厂不缓存负值）
     */
    public static WechatSdkClients build(PaymentChannelConfig config) {
        if (config == null) {
            throw new CustomException("微信支付渠道配置为空，无法初始化");
        }
        String appId = safeTrim(config.getWxAppId());
        String mchId = safeTrim(config.getWxMchId());
        if (appId.isEmpty() || mchId.isEmpty()) {
            throw new CustomException("微信支付缺少 AppID 或商户号，无法初始化");
        }
        String apiV3Key = PaymentCredentialEncryptor.decrypt(config.getWxApiV3Key());
        String privateKey = PaymentCredentialEncryptor.decrypt(config.getWxMchPrivateKey());
        if (isBlank(apiV3Key) || isBlank(privateKey)) {
            throw new CustomException("微信支付 APIv3 密钥或商户私钥缺失（解密失败），无法初始化");
        }

        Config wxConfig = buildConfig(config, mchId, safeTrim(privateKey), safeTrim(apiV3Key));

        NativePayService nativePayService = new NativePayService.Builder().config(wxConfig).build();
        H5Service h5Service = new H5Service.Builder().config(wxConfig).build();
        RefundService refundService = new RefundService.Builder().config(wxConfig).build();

        log.info("[微信支付] SDK 初始化成功 tenantId={}, mchId={}, appId={}, mode={}",
                config.getTenantId(), mchId, appId,
                isBlank(config.getWxPublicKeyId()) ? "平台证书自动更新" : "微信支付公钥");
        return new WechatSdkClients(wxConfig, nativePayService, h5Service, refundService);
    }

    /**
     * 按是否配置微信支付公钥 ID 选择证书模式。
     */
    private static Config buildConfig(PaymentChannelConfig config, String mchId,
                                      String privateKey, String apiV3Key) {
        String publicKeyId = safeTrim(config.getWxPublicKeyId());
        if (!publicKeyId.isEmpty()) {
            // 微信支付公钥模式：以 publicKeyId + 公钥验签，无需商户证书序列号；不依赖平台证书下载
            String publicKey = safeTrim(config.getWxPublicKey());
            if (publicKey.isEmpty()) {
                throw new CustomException("已配置微信支付公钥 ID，但缺少公钥内容，无法初始化");
            }
            return new RSAPublicKeyConfig.Builder()
                    .merchantId(mchId)
                    .privateKey(privateKey)
                    .apiV3Key(apiV3Key)
                    .publicKeyId(publicKeyId)
                    .publicKey(publicKey)
                    .build();
        }
        // 平台证书自动更新模式：需要商户证书序列号，SDK 自动下载并定期刷新微信平台证书
        String serialNo = safeTrim(config.getWxMchCertSerialNo());
        if (serialNo.isEmpty()) {
            throw new CustomException("微信支付缺少商户证书序列号，无法初始化（平台证书模式）");
        }
        return new RSAAutoCertificateConfig.Builder()
                .merchantId(mchId)
                .privateKey(privateKey)
                .merchantSerialNumber(serialNo)
                .apiV3Key(apiV3Key)
                .build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String safeTrim(String s) {
        return s == null ? "" : s.trim();
    }
}
