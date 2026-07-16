package com.reggie.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.sys.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

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

    /**
     * 角色统计（总数、启用、禁用、已分配权限的角色数）
     * <p>使用 SQL 聚合替代前端 pageSize:999 拉全量后遍历统计 withPerms，避免全表扫描</p>
     *
     * @return 聚合结果：total/enabled/disabled/withPerms
     */
    @Select("SELECT "
            + "COUNT(*) AS total, "
            + "COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS enabled, "
            + "COALESCE(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS disabled, "
            + "COALESCE(SUM(CASE WHEN EXISTS "
            + "(SELECT 1 FROM role_permission rp WHERE rp.role_id = r.id) THEN 1 ELSE 0 END), 0) AS withPerms "
            + "FROM role r WHERE r.is_deleted = 0")
    Map<String, Object> statRoles();
}
