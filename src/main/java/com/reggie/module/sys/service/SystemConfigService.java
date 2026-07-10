package com.reggie.module.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.sys.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 根据配置键获取值（优先租户级，其次全局）
     */
    String getConfig(String configKey);

    /**
     * 根据配置键获取值（指定租户）
     */
    String getConfig(String configKey, Long tenantId);

    /**
     * 获取配置值，不存在返回默认值
     */
    String getConfigOrDefault(String configKey, String defaultValue);

    /**
     * 设置配置值（租户级）
     */
    boolean setConfig(Long tenantId, String configKey, String configValue);

    /**
     * 设置全局配置值
     */
    boolean setGlobalConfig(String configKey, String configValue);

    /**
     * 查询租户下的所有配置
     */
    List<SystemConfig> listByTenantId(Long tenantId);

    /**
     * 批量获取配置（一次性查询多个key）
     */
    Map<String, String> getConfigs(List<String> keys, Long tenantId);
}
