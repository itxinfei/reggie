package com.reggie.module.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.sys.entity.SystemConfig;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 系统配置管理服务接口
 * </p>
 * <p>提供键值对配置的多租户支持（租户级/全局级）、批量查询等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface SystemConfigService extends IService<SystemConfig> {

    /**
     * 根据配置键获取值（优先租户级，其次全局）
     *
     * @param configKey 配置键
     * @return 配置值，不存在返回null
     */
    String getConfig(String configKey);

    /**
     * 根据配置键获取值（指定租户）
     *
     * @param configKey 配置键
     * @param tenantId  租户ID
     * @return 配置值，不存在返回null
     */
    String getConfig(String configKey, Long tenantId);

    /**
     * 获取配置值，不存在返回默认值
     *
     * @param configKey    配置键
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    String getConfigOrDefault(String configKey, String defaultValue);

    /**
     * 设置配置值（租户级）
     *
     * @param tenantId     租户ID
     * @param configKey    配置键
     * @param configValue  配置值
     * @return 是否设置成功
     */
    boolean setConfig(Long tenantId, String configKey, String configValue);

    /**
     * 设置全局配置值
     *
     * @param configKey    配置键
     * @param configValue  配置值
     * @return 是否设置成功
     */
    boolean setGlobalConfig(String configKey, String configValue);

    /**
     * 查询租户下的所有配置
     *
     * @param tenantId 租户ID
     * @return 配置列表
     */
    List<SystemConfig> listByTenantId(Long tenantId);

    /**
     * 批量获取配置（一次性查询多个key）
     *
     * @param keys     配置键列表
     * @param tenantId 租户ID
     * @return 配置键值对Map
     */
    Map<String, String> getConfigs(List<String> keys, Long tenantId);
}
