package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.payment.channel.PaymentChannelFactory;
import com.reggie.module.payment.mapper.PaymentChannelConfigMapper;
import com.reggie.module.payment.model.PaymentChannelConfig;
import com.reggie.module.payment.service.PaymentChannelConfigService;
import com.reggie.module.payment.util.PaymentCredentialEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 支付渠道配置服务实现
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class PaymentChannelConfigServiceImpl
        extends ServiceImpl<PaymentChannelConfigMapper, PaymentChannelConfig>
        implements PaymentChannelConfigService {

    /** 敏感字段掩码 */
    private static final String MASK = "***已加密***";

    @Autowired
    private PaymentChannelFactory channelFactory;

    @Override
    public IPage<PaymentChannelConfig> pageMasked(IPage<PaymentChannelConfig> page,
                                                  String channel, Integer enabled) {
        LambdaQueryWrapper<PaymentChannelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentChannelConfig::getIsDeleted, 0)
                .eq(StringUtils.hasText(channel), PaymentChannelConfig::getChannel, channel)
                .eq(enabled != null, PaymentChannelConfig::getEnabled, enabled)
                .orderByDesc(PaymentChannelConfig::getUpdateTime);
        IPage<PaymentChannelConfig> result = this.page(page, wrapper);
        for (PaymentChannelConfig config : result.getRecords()) {
            maskCredentials(config);
        }
        return result;
    }

    @Override
    public PaymentChannelConfig getMaskedById(Long id) {
        PaymentChannelConfig config = this.getById(id);
        if (config != null) {
            maskCredentials(config);
        }
        return config;
    }

    @Override
    public PaymentChannelConfig getEntityById(Long id) {
        return this.getById(id);
    }

    @Override
    public PaymentChannelConfig addConfig(PaymentChannelConfig config) {
        if (config.getTenantId() == null) {
            config.setTenantId(BaseContext.getCurrentTenantId());
        }
        if (config.getEnabled() == null) {
            config.setEnabled(1);
        }
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        encryptCredentials(config);
        this.save(config);
        evictCache(config.getTenantId(), config.getChannel());
        maskCredentials(config);
        return config;
    }

    @Override
    public boolean updateConfig(PaymentChannelConfig config) {
        PaymentChannelConfig exist = this.getById(config.getId());
        if (exist == null) {
            return false;
        }
        // 三个敏感字段：仅当明文非空才重新加密，留空保留原密文
        if (StringUtils.hasText(config.getWxApiV3Key())) {
            exist.setWxApiV3Key(PaymentCredentialEncryptor.encrypt(config.getWxApiV3Key()));
        }
        if (StringUtils.hasText(config.getWxMchPrivateKey())) {
            exist.setWxMchPrivateKey(PaymentCredentialEncryptor.encrypt(config.getWxMchPrivateKey()));
        }
        if (StringUtils.hasText(config.getAliPrivateKey())) {
            exist.setAliPrivateKey(PaymentCredentialEncryptor.encrypt(config.getAliPrivateKey()));
        }
        // 渠道不可改（前端编辑时禁用），此处不覆盖 channel
        exist.setConfigName(config.getConfigName());
        exist.setWxAppId(config.getWxAppId());
        exist.setWxMchId(config.getWxMchId());
        exist.setWxMchCertSerialNo(config.getWxMchCertSerialNo());
        exist.setWxPublicKeyId(config.getWxPublicKeyId());
        exist.setWxPublicKey(config.getWxPublicKey());
        exist.setAliAppId(config.getAliAppId());
        exist.setAliPublicKey(config.getAliPublicKey());
        exist.setPayNotifyUrl(config.getPayNotifyUrl());
        exist.setRefundNotifyUrl(config.getRefundNotifyUrl());
        exist.setEnabled(config.getEnabled());
        exist.setRemark(config.getRemark());
        exist.setUpdateTime(LocalDateTime.now());
        boolean ok = this.updateById(exist);
        if (ok) {
            evictCache(exist.getTenantId(), exist.getChannel());
        }
        return ok;
    }

    @Override
    public boolean setEnabled(Long id, Integer enabled) {
        PaymentChannelConfig exist = this.getById(id);
        if (exist == null) {
            return false;
        }
        PaymentChannelConfig config = new PaymentChannelConfig();
        config.setId(id);
        config.setEnabled(enabled);
        config.setUpdateTime(LocalDateTime.now());
        boolean ok = this.updateById(config);
        if (ok) {
            evictCache(exist.getTenantId(), exist.getChannel());
        }
        return ok;
    }

    @Override
    public boolean existsActiveConfig(String channel, Long exceptId) {
        LambdaQueryWrapper<PaymentChannelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentChannelConfig::getChannel, channel)
                .eq(PaymentChannelConfig::getIsDeleted, 0);
        if (exceptId != null) {
            wrapper.ne(PaymentChannelConfig::getId, exceptId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public PaymentChannelConfig findActive(Long tenantId, String channel) {
        return this.baseMapper.selectActiveIgnoreTenant(tenantId, channel);
    }

    @Override
    public boolean removeById(Serializable id) {
        PaymentChannelConfig exist = this.getById(id);
        boolean ok = super.removeById(id, false);
        if (ok && exist != null) {
            evictCache(exist.getTenantId(), exist.getChannel());
        }
        return ok;
    }

    /** 写操作成功后驱逐工厂缓存（缓存驱逐失败仅告警，不影响配置落库） */
    private void evictCache(Long tenantId, String channel) {
        try {
            channelFactory.evict(tenantId, channel);
        } catch (Exception e) {
            log.warn("驱逐支付渠道缓存失败 tenant={}, channel={}, err={}", tenantId, channel, e.getMessage());
        }
    }

    /** 保存前加密三个敏感字段 */
    private void encryptCredentials(PaymentChannelConfig config) {
        if (StringUtils.hasText(config.getWxApiV3Key())) {
            config.setWxApiV3Key(PaymentCredentialEncryptor.encrypt(config.getWxApiV3Key()));
        }
        if (StringUtils.hasText(config.getWxMchPrivateKey())) {
            config.setWxMchPrivateKey(PaymentCredentialEncryptor.encrypt(config.getWxMchPrivateKey()));
        }
        if (StringUtils.hasText(config.getAliPrivateKey())) {
            config.setAliPrivateKey(PaymentCredentialEncryptor.encrypt(config.getAliPrivateKey()));
        }
    }

    /** 列表/详情返回时掩码（密文同样敏感，不外泄） */
    private void maskCredentials(PaymentChannelConfig config) {
        if (config.getWxApiV3Key() != null) {
            config.setWxApiV3Key(MASK);
        }
        if (config.getWxMchPrivateKey() != null) {
            config.setWxMchPrivateKey(MASK);
        }
        if (config.getAliPrivateKey() != null) {
            config.setAliPrivateKey(MASK);
        }
    }
}
