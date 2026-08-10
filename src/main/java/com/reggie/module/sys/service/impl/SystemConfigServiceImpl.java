package com.reggie.module.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.sys.entity.SystemConfig;
import com.reggie.module.sys.mapper.SystemConfigMapper;
import com.reggie.module.sys.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 * 优先取租户级配置，其次取全局配置（tenant_id IS NULL）
 */
@Slf4j
/**
 * SystemConfig service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SystemConfigServiceImpl extends com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<SystemConfigMapper, SystemConfig>
        implements SystemConfigService {

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    @Cacheable(value = "systemConfig", key = "'config:' + #configKey + ':' + (#tenantId != null ? #tenantId : 'global')")
    public String getConfig(String configKey, Long tenantId) {
        SystemConfig config = systemConfigMapper.findByConfigKey(configKey, tenantId);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String getConfig(String configKey) {
        Long tenantId = BaseContext.getCurrentTenantId();
        return getConfig(configKey, tenantId);
    }

    @Override
    public String getConfigOrDefault(String configKey, String defaultValue) {
        String value = getConfig(configKey);
        return value != null ? value : defaultValue;
    }

    @Override
    @CacheEvict(value = "systemConfig", key = "'config:' + #configKey + ':' + (#tenantId != null ? #tenantId : 'global')")
    public boolean setConfig(Long tenantId, String configKey, String configValue) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, configKey)
               .eq(SystemConfig::getTenantId, tenantId);
        SystemConfig existing = this.getOne(wrapper);

        if (existing != null) {
            existing.setConfigValue(configValue);
            existing.setUpdateTime(LocalDateTime.now());
            existing.setUpdateUser(BaseContext.getCurrentId());
            return this.updateById(existing);
        } else {
            SystemConfig config = new SystemConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType(SystemConfig.TYPE_FEATURE);
            config.setCreateTime(LocalDateTime.now());
            config.setCreateUser(BaseContext.getCurrentId());
            return this.save(config);
        }
    }

    @Override
    @CacheEvict(value = "systemConfig", key = "'config:' + #configKey + ':global'")
    public boolean setGlobalConfig(String configKey, String configValue) {
        return setConfig(null, configKey, configValue);
    }

    @Override
    public List<SystemConfig> listByTenantId(Long tenantId) {
        return systemConfigMapper.listByTenantId(tenantId);
    }

    @Override
    public Map<String, String> getConfigs(List<String> keys, Long tenantId) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SystemConfig> configs = systemConfigMapper.listByTenantId(tenantId);
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            if (keys.contains(config.getConfigKey())) {
                result.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        return result;
    }
}



