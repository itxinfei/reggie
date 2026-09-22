package com.reggie.module.payment.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.payment.model.PaymentChannelConfig;

/**
 * 支付渠道配置服务
 *
 * @author reggie
 * @since 2026-09-21
 */
public interface PaymentChannelConfigService extends IService<PaymentChannelConfig> {

    /**
     * 分页查询（敏感字段掩码）。
     * @param page    分页参数
     * @param channel 渠道筛选（可空）
     * @param enabled 启用状态筛选（可空）
     * @return 分页结果
     */
    IPage<PaymentChannelConfig> pageMasked(IPage<PaymentChannelConfig> page, String channel, Integer enabled);

    /**
     * 按主键查询详情（敏感字段掩码）。
     * @param id 主键
     * @return 掩码后的配置；不存在返回 null
     */
    PaymentChannelConfig getMaskedById(Long id);

    /**
     * 按主键查询原始记录（含密文，仅供后端内部使用，禁止直接返回前端）。
     * @param id 主键
     * @return 原始配置；不存在返回 null
     */
    PaymentChannelConfig getEntityById(Long id);

    /**
     * 新增配置（敏感字段加密落库）。
     * @param config 配置
     * @return 掩码后的配置
     */
    PaymentChannelConfig addConfig(PaymentChannelConfig config);

    /**
     * 更新配置（敏感字段留空则保留原密文；渠道不可改）。
     * @param config 配置（含 ID）
     * @return 是否成功
     */
    boolean updateConfig(PaymentChannelConfig config);

    /**
     * 启用 / 停用。
     * @param id      主键
     * @param enabled 目标状态
     * @return 是否成功
     */
    boolean setEnabled(Long id, Integer enabled);

    /**
     * 同租户同渠道是否已存在启用配置（应用层查重）。
     * @param channel  渠道
     * @param exceptId 更新时排除自身（新增传 null）
     * @return 是否存在
     */
    boolean existsActiveConfig(String channel, Long exceptId);

    /**
     * 按租户 + 渠道查询启用配置（含密文，供支付工厂构建 SDK）。
     * @param tenantId 租户 ID
     * @param channel  渠道
     * @return 启用配置；不存在返回 null
     */
    PaymentChannelConfig findActive(Long tenantId, String channel);
}
