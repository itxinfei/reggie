package com.reggie.module.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.payment.model.PaymentChannelConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 支付渠道配置 Mapper
 *
 * @author reggie
 * @since 2026-09-21
 */
@Mapper
public interface PaymentChannelConfigMapper extends BaseMapper<PaymentChannelConfig> {

    /**
     * 按租户 + 渠道查询启用配置（跨租户，跳过租户拦截器）。
     * <p>供支付工厂构建 SDK 与回调租户路由使用：用支付单上的 tenantId 显式查询，不依赖登录态。</p>
     *
     * @param tenantId 租户 ID
     * @param channel  渠道 WECHAT/ALIPAY
     * @return 启用且未删除的配置（含密文）；不存在返回 null
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM payment_channel_config "
            + "WHERE tenant_id = #{tenantId} AND channel = #{channel} "
            + "AND enabled = 1 AND is_deleted = 0 LIMIT 1")
    PaymentChannelConfig selectActiveIgnoreTenant(@Param("tenantId") Long tenantId,
                                                  @Param("channel") String channel);
}
