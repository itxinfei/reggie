package com.reggie.module.payment.channel;

import com.alipay.api.AlipayClient;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.payment.channel.real.RealAlipayChannel;
import com.reggie.module.payment.channel.real.WechatV3PayChannel;
import com.reggie.module.payment.channel.sdk.AlipaySdkBuilder;
import com.reggie.module.payment.channel.sdk.WechatSdkBuilder;
import com.reggie.module.payment.channel.sdk.WechatSdkClients;
import com.reggie.module.payment.config.PaymentConfigProperties;
import com.reggie.module.payment.mapper.PaymentChannelConfigMapper;
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.reggie.utils.QRCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * 支付渠道工厂。mock 模式直接返回存量 mock 渠道（不触数据库/网络）；
 * 真实模式按「租户 + 渠道」加载数据库配置、解密并构建官方 SDK 客户端，结果缓存复用，
 * 配置变更时由 {@link #evict} / {@link #evictTenant} 驱逐后重建。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Component
public class PaymentChannelFactory {

    private static final Logger log = LoggerFactory.getLogger(PaymentChannelFactory.class);

    /** 支付宝 mock 渠道 */
    @Autowired
    private AlipayChannel alipayChannel;

    /** 微信 mock 渠道 */
    @Autowired
    private WechatPayChannel wechatPayChannel;

    @Autowired
    private PaymentConfigProperties paymentConfigProperties;

    /** 支付渠道配置 Mapper：按租户查启用配置（忽略租户插件，显式传 tenantId） */
    @Autowired
    private PaymentChannelConfigMapper paymentChannelConfigMapper;

    /** 二维码工具：真实渠道渲染扫码二维码用（线程安全单例） */
    @Autowired
    private QRCodeUtil qrCodeUtil;

    /** 真实渠道缓存，key = tenantId:CHANNEL */
    private final ConcurrentHashMap<String, PaymentChannel> realCache = new ConcurrentHashMap<>();

    /** per-key 构建锁，避免并发重复下载证书/构建客户端 */
    private final ConcurrentHashMap<String, Object> lockMap = new ConcurrentHashMap<>();

    /**
     * 根据渠道类型获取对应的支付适配器
     *
     * @param channel 渠道类型（ALIPAY/WECHAT）
     * @return 支付适配器实例
     * @throws CustomException 当渠道类型不支持或缺少租户上下文时抛出
     */
    public PaymentChannel getChannel(String channel) {
        if (channel == null) {
            throw new CustomException("支付通道不能为空");
        }
        String ch = channel.toUpperCase();
        if (!isSupported(ch)) {
            throw new CustomException("不支持的支付通道: " + channel);
        }
        if (paymentConfigProperties.isMockMode()) {
            return mockOf(ch);
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("当前无租户上下文，无法获取支付渠道");
        }
        return getRealChannel(tenantId, ch);
    }

    /**
     * 根据渠道类型获取对应的支付适配器（回调场景专用）
     * <p>
     * 未知/空渠道、缺少租户上下文或构建异常时返回 null 而非抛异常，
     * 由调用方返回 200 + 明确失败码主动停止平台重试。
     * </p>
     *
     * @param channel 渠道类型（ALIPAY/WECHAT）
     * @return 支付适配器实例，无法获取时返回 null
     */
    public PaymentChannel getChannelNullable(String channel) {
        if (channel == null) {
            return null;
        }
        String ch = channel.toUpperCase();
        if (!isSupported(ch)) {
            return null;
        }
        if (paymentConfigProperties.isMockMode()) {
            return mockOf(ch);
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return null;
        }
        try {
            return getRealChannel(tenantId, ch);
        } catch (Exception e) {
            log.warn("获取真实支付渠道失败 tenant={}, channel={}, err={}", tenantId, ch, e.getMessage());
            return null;
        }
    }

    /**
     * 回调场景专用：按 URL 路由出的租户获取渠道，不依赖 BaseContext（回调无登录态/无 ThreadLocal）。
     *
     * @param tenantId 从 notify_url 的 tenantId 参数解析出的租户
     * @param channel  渠道（ALIPAY/WECHAT）
     * @return 支付渠道；租户/渠道非法或构建失败时返回 null
     */
    public PaymentChannel getChannelForTenantNullable(Long tenantId, String channel) {
        if (tenantId == null || channel == null) {
            return null;
        }
        String ch = channel.toUpperCase();
        if (!isSupported(ch)) {
            return null;
        }
        if (paymentConfigProperties.isMockMode()) {
            return mockOf(ch);
        }
        try {
            return getRealChannel(tenantId, ch);
        } catch (Exception e) {
            log.warn("回调按租户获取真实渠道失败 tenant={}, channel={}, err={}",
                    tenantId, ch, e.getMessage());
            return null;
        }
    }

    /**
     * 按「租户 + 渠道」获取真实渠道，双重检查锁定 + 缓存。
     */
    private PaymentChannel getRealChannel(Long tenantId, String ch) {
        String key = cacheKey(tenantId, ch);
        PaymentChannel channel = realCache.get(key);
        if (channel != null) {
            return channel;
        }
        Object lock = lockMap.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            channel = realCache.get(key);
            if (channel == null) {
                channel = buildRealChannel(tenantId, ch);
                realCache.put(key, channel);
            }
            return channel;
        }
    }

    /**
     * 构建真实渠道：查数据库启用配置 → 解密 → 官方 SDK 客户端 → 真实渠道。
     * <p>
     * 在 per-key 锁内调用，构建失败抛异常且不写入缓存（不缓存负值），下次请求可重新构建。
     * </p>
     *
     * @param tenantId 租户 ID
     * @param ch       渠道（ALIPAY/WECHAT）
     * @return 真实支付渠道
     * @throws CustomException 未配置启用渠道、凭据缺失或 SDK 初始化失败时抛出
     */
    private PaymentChannel buildRealChannel(Long tenantId, String ch) {
        PaymentChannelConfig config =
                paymentChannelConfigMapper.selectActiveIgnoreTenant(tenantId, ch);
        if (config == null) {
            throw new CustomException("未找到该租户已启用的支付渠道配置，请先在「支付渠道配置」中添加并启用: "
                    + "tenant=" + tenantId + ", channel=" + ch);
        }
        if ("WECHAT".equals(ch)) {
            WechatSdkClients clients = WechatSdkBuilder.build(config);
            return new WechatV3PayChannel(clients, config, qrCodeUtil);
        }
        AlipayClient alipayClient =
                AlipaySdkBuilder.build(config, paymentConfigProperties.getAlipayGateway());
        return new RealAlipayChannel(alipayClient, config, qrCodeUtil);
    }

    /**
     * 驱逐指定「租户 + 渠道」的缓存，下次请求重建。配置增改/启停后调用。
     *
     * @param tenantId 租户 ID
     * @param channel  渠道（ALIPAY/WECHAT，大小写不敏感）
     */
    public void evict(Long tenantId, String channel) {
        if (tenantId == null || channel == null) {
            return;
        }
        realCache.remove(cacheKey(tenantId, channel.toUpperCase()));
    }

    /**
     * 驱逐某租户的全部渠道缓存。
     *
     * @param tenantId 租户 ID
     */
    public void evictTenant(Long tenantId) {
        if (tenantId == null) {
            return;
        }
        String prefix = tenantId + ":";
        realCache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private boolean isSupported(String ch) {
        return "ALIPAY".equals(ch) || "WECHAT".equals(ch);
    }

    private PaymentChannel mockOf(String ch) {
        return "ALIPAY".equals(ch) ? alipayChannel : wechatPayChannel;
    }

    private String cacheKey(Long tenantId, String ch) {
        return tenantId + ":" + ch;
    }
}
