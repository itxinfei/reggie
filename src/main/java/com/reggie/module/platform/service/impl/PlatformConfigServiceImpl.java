package com.reggie.module.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.platform.mapper.PlatformConfigMapper;
import com.reggie.module.platform.model.PlatformConfig;
import com.reggie.module.platform.service.PlatformConfigService;
import com.reggie.module.platform.util.PlatformCredentialEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 外卖平台接入配置服务实现
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Service
public class PlatformConfigServiceImpl extends ServiceImpl<PlatformConfigMapper, PlatformConfig> implements PlatformConfigService {

    /** 凭据脱敏展示前缀 */
    private static final String MASK = "***已加密***";

    @Override
    public IPage<PlatformConfig> pageMasked(IPage<PlatformConfig> page) {
        LambdaQueryWrapper<PlatformConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformConfig::getIsDeleted, 0)
                .orderByDesc(PlatformConfig::getUpdateTime);
        IPage<PlatformConfig> result = this.page(page, wrapper);
        for (PlatformConfig config : result.getRecords()) {
            maskCredentials(config);
        }
        return result;
    }

    @Override
    public PlatformConfig getMaskedById(Long id) {
        PlatformConfig config = this.getById(id);
        if (config != null) {
            maskCredentials(config);
        }
        return config;
    }

    @Override
    public PlatformConfig addConfig(PlatformConfig config) {
        if (config.getTenantId() == null) {
            config.setTenantId(BaseContext.getCurrentTenantId());
        }
        if (config.getEnabled() == null) {
            config.setEnabled(1);
        }
        if (config.getSyncScope() == null) {
            config.setSyncScope(1); // 默认同步订单
        }
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        encryptCredentials(config);
        this.save(config);
        maskCredentials(config);
        return config;
    }

    @Override
    public boolean updateConfig(PlatformConfig config) {
        PlatformConfig exist = this.getById(config.getId());
        if (exist == null) {
            return false;
        }
        // 仅当明文非空时才重新加密，否则保留原密文
        if (StringUtils.hasText(config.getAppKey())) {
            exist.setAppKey(PlatformCredentialEncryptor.encrypt(config.getAppKey()));
        }
        if (StringUtils.hasText(config.getAppSecret())) {
            exist.setAppSecret(PlatformCredentialEncryptor.encrypt(config.getAppSecret()));
        }
        if (StringUtils.hasText(config.getAccessToken())) {
            exist.setAccessToken(PlatformCredentialEncryptor.encrypt(config.getAccessToken()));
        }
        exist.setPlatformType(config.getPlatformType());
        exist.setPlatformName(config.getPlatformName());
        exist.setShopId(config.getShopId());
        exist.setSyncScope(config.getSyncScope());
        exist.setRemark(config.getRemark());
        exist.setEnabled(config.getEnabled());
        exist.setUpdateTime(LocalDateTime.now());
        return this.updateById(exist);
    }

    @Override
    public boolean setEnabled(Long id, Integer enabled) {
        PlatformConfig config = new PlatformConfig();
        config.setId(id);
        config.setEnabled(enabled);
        config.setUpdateTime(LocalDateTime.now());
        return this.updateById(config);
    }

    @Override
    public boolean existsByTypeAndShop(String platformType, String shopId, Long exceptId) {
        LambdaQueryWrapper<PlatformConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformConfig::getPlatformType, platformType)
                .eq(PlatformConfig::getShopId, shopId)
                .eq(PlatformConfig::getIsDeleted, 0);
        if (exceptId != null) {
            wrapper.ne(PlatformConfig::getId, exceptId);
        }
        return this.count(wrapper) > 0;
    }

    @Override
    public List<PlatformConfig> listEnabledConfigs() {
        LambdaQueryWrapper<PlatformConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformConfig::getEnabled, 1)
                .eq(PlatformConfig::getIsDeleted, 0)
                .orderByDesc(PlatformConfig::getUpdateTime);
        return this.list(wrapper);
    }

    @Override
    public PlatformConfig getByPlatformType(String platformType, Long tenantId) {
        LambdaQueryWrapper<PlatformConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PlatformConfig::getPlatformType, platformType)
               .eq(PlatformConfig::getTenantId, tenantId)
               .eq(PlatformConfig::getIsDeleted, 0)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    /** 保存前加密三个凭据字段 */
    private void encryptCredentials(PlatformConfig config) {
        if (StringUtils.hasText(config.getAppKey())) {
            config.setAppKey(PlatformCredentialEncryptor.encrypt(config.getAppKey()));
        }
        if (StringUtils.hasText(config.getAppSecret())) {
            config.setAppSecret(PlatformCredentialEncryptor.encrypt(config.getAppSecret()));
        }
        if (StringUtils.hasText(config.getAccessToken())) {
            config.setAccessToken(PlatformCredentialEncryptor.encrypt(config.getAccessToken()));
        }
    }

    /** 列表/详情返回时脱敏，避免泄露密文（密文也属敏感） */
    private void maskCredentials(PlatformConfig config) {
        config.setAppKey(config.getAppKey() != null ? MASK : null);
        config.setAppSecret(config.getAppSecret() != null ? MASK : null);
        config.setAccessToken(config.getAccessToken() != null ? MASK : null);
    }
}
