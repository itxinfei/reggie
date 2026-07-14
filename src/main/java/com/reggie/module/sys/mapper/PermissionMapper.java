package com.reggie.module.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.sys.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 权限 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    /**
     * 根据权限标识查询
     *
     * @param permissionKey 权限标识
     * @return 权限信息
     */
    @Select("SELECT * FROM permission WHERE permission_key = #{permissionKey} AND status = 1")
    Permission findByPermissionKey(@Param("permissionKey") String permissionKey);

    /**
     * 查询所有权限（用于权限树构建）
     *
     * @return 所有启用权限列表
     */
    @Select("SELECT * FROM permission WHERE status = 1 ORDER BY sort ASC, id ASC")
    List<Permission> listAllEnabled();

    /**
     * 根据角色ID查询权限列表
     *
     * @param roleId 角色ID
     * @return 权限列表
     */
    @Select("SELECT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId} AND p.status = 1 " +
            "ORDER BY p.sort ASC, p.id ASC")
    List<Permission> listByRoleId(@Param("roleId") Long roleId);
}
