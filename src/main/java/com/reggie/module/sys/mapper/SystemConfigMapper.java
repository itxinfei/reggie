package com.reggie.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.sys.model.SystemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 系统配置 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfig> {

    /**
     * 根据配置键查询（优先取租户级，其次全局）
     *
     * @param configKey 配置键
     * @param tenantId 租户ID
     * @return 系统配置
     */
    @Select("SELECT * FROM system_config " +
            "WHERE config_key = #{configKey} AND (tenant_id = #{tenantId} OR tenant_id IS NULL) " +
            "ORDER BY tenant_id DESC LIMIT 1")
    SystemConfig findByConfigKey(@Param("configKey") String configKey, @Param("tenantId") Long tenantId);

    /**
     * 查询租户下的所有配置
     *
     * @param tenantId 租户ID
     * @return 系统配置列表
     */
    @Select("SELECT * FROM system_config WHERE tenant_id = #{tenantId} OR tenant_id IS NULL ORDER BY config_type ASC, id ASC")
    List<SystemConfig> listByTenantId(@Param("tenantId") Long tenantId);
}

