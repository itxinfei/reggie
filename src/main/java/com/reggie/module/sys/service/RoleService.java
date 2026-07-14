package com.reggie.module.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.sys.entity.Role;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 角色管理服务接口
 * </p>
 * <p>提供角色CRUD、权限分配、角色选项查询等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface RoleService extends IService<Role> {

    /**
     * 根据角色标识查询
     *
     * @param roleKey 角色标识
     * @return 角色信息
     */
    Role getByRoleKey(String roleKey);

    /**
     * 查询租户下的所有启用角色
     *
     * @param tenantId 租户ID
     * @return 启用的角色列表
     */
    List<Role> listEnabledByTenantId(Long tenantId);

    /**
     * 为角色分配权限
     *
     * @param roleId        角色ID
     * @param permissionIds 权限ID列表
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 查询角色拥有的权限ID列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> getPermissionIds(Long roleId);

    /**
     * 构建角色下拉列表（key-value格式）
     *
     * @param tenantId 租户ID
     * @return 角色选项列表
     */
    List<Map<String, Object>> getRoleOptions(Long tenantId);
}
