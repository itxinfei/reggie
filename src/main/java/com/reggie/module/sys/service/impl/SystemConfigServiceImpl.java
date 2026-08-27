package com.reggie.module.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.module.sys.model.SystemConfig;
import com.reggie.module.sys.mapper.SystemConfigMapper;
import com.reggie.module.sys.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.reggie.common.CustomException;

/**
 * 系统配置服务实现
 * 优先取租户级配置，其次取全局配置（tenant_id IS NULL）
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

    /**
     * 新增租户级配置（租户安全）
     * <p>tenantId 从 BaseContext 强制取得，前端无法通过 DTO 字段篡改租户归属。
     * 若当前租户已存在同 key 配置，则拒绝创建（避免跨租户覆盖）。</p>
     */
    @Override
    @CacheEvict(value = "systemConfig", key = "'config:' + #configKey + ':' + T(com.reggie.common.BaseContext).getCurrentTenantId()")
    public boolean addTenantConfig(String configKey, String configValue) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法创建配置");
        }
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getTenantId, tenantId)
               .eq(SystemConfig::getConfigKey, configKey);
        Long count = Long.valueOf(this.count(wrapper));
        if (count > 0) {
            throw new CustomException("当前租户已存在配置 [" + configKey + "]，请使用更新接口");
        }
        SystemConfig config = new SystemConfig();
        config.setTenantId(tenantId);
        config.setConfigKey(configKey);
        config.setConfigValue(configValue);
        config.setConfigType(SystemConfig.TYPE_FEATURE);
        config.setCreateTime(LocalDateTime.now());
        config.setCreateUser(BaseContext.getCurrentId());
        return this.save(config);
    }

    /**
     * 更新当前租户配置值（租户安全）
     * <p>先通过 key 查询当前租户下的配置确认归属，再仅更新 configValue 字段，
     * 避免前端通过全实体覆盖 tenantId / configKey / id 等敏感字段。</p>
     */
    @Override
    @CacheEvict(value = "systemConfig", key = "'config:' + #configKey + ':' + T(com.reggie.common.BaseContext).getCurrentTenantId()")
    public boolean updateTenantConfig(Long id, String configKey, String configValue) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法更新配置");
        }
        // 先按 tenantId + configKey 查询确认归属
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getTenantId, tenantId)
               .eq(SystemConfig::getConfigKey, configKey)
               .eq(SystemConfig::getId, id);
        SystemConfig existing = this.getOne(wrapper);
        if (existing == null) {
            throw new CustomException("配置不存在或不属于当前租户（id=" + id + ", key=" + configKey + "）");
        }
        // 仅更新 configValue，不接收前端传入的 tenantId / configKey / id
        existing.setConfigValue(configValue);
        existing.setUpdateTime(LocalDateTime.now());
        existing.setUpdateUser(BaseContext.getCurrentId());
        return this.updateById(existing);
    }

    /**
     * 删除当前租户配置（租户安全）
     * <p>先查询确认该 id 的配置属于当前租户，再删除，防止通过 ID 猜测跨租户删除。</p>
     */
    @Override
    @CacheEvict(value = "systemConfig", allEntries = true)
    public boolean deleteTenantConfig(Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法删除配置");
        }
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getId, id)
               .eq(SystemConfig::getTenantId, tenantId);
        SystemConfig existing = this.getOne(wrapper);
        if (existing == null) {
            throw new CustomException("配置不存在或不属于当前租户（id=" + id + "）");
        }
        return this.removeById(id);
    }
}







