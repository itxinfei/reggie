package com.reggie.module.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.sys.entity.Role;

import java.util.List;
import java.util.Map;

/**
 * 角色服务接口
 */
public interface RoleService extends IService<Role> {

    /**
     * 根据角色标识查询
     */
    Role getByRoleKey(String roleKey);

    /**
     * 查询租户下的所有启用角色
     */
    List<Role> listEnabledByTenantId(Long tenantId);

    /**
     * 为角色分配权限
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 查询角色拥有的权限ID列表
     */
    List<Long> getPermissionIds(Long roleId);

    /**
     * 构建角色下拉列表（key-value格式）
     */
    List<Map<String, Object>> getRoleOptions(Long tenantId);
}
