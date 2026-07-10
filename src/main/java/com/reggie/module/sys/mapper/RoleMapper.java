package com.reggie.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.sys.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色Mapper
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色标识查询
     */
    @Select("SELECT * FROM role WHERE role_key = #{roleKey} AND is_deleted = 0 AND status = 1")
    Role findByRoleKey(@Param("roleKey") String roleKey);

    /**
     * 查询租户下的所有启用角色
     */
    @Select("SELECT * FROM role WHERE (tenant_id = #{tenantId} OR tenant_id IS NULL) AND is_deleted = 0 AND status = 1 ORDER BY sort DESC")
    List<Role> listEnabledByTenantId(@Param("tenantId") Long tenantId);
}
