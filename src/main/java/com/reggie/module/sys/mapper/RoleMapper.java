package com.reggie.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.sys.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 角色 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据租户ID和角色标识查询
     *
     * @param tenantId 租户ID
     * @param roleKey 角色标识
     * @return 角色信息
     */
    @Select("SELECT * FROM role WHERE tenant_id = #{tenantId} AND role_key = #{roleKey} AND is_deleted = 0 AND status = 1")
    Role findByRoleKeyAndTenantId(@Param("tenantId") Long tenantId, @Param("roleKey") String roleKey);

    /**
     * 查询租户下的所有启用角色
     *
     * @param tenantId 租户ID
     * @return 启用角色列表
     */
    @Select("SELECT * FROM role WHERE (tenant_id = #{tenantId} OR tenant_id IS NULL) AND is_deleted = 0 AND status = 1 ORDER BY sort DESC")
    List<Role> listEnabledByTenantId(@Param("tenantId") Long tenantId);
}
