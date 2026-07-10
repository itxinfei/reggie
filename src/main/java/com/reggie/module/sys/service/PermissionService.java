package com.reggie.module.sys.service;

import com.reggie.module.sys.entity.Permission;

import java.util.List;
import java.util.Map;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 获取所有权限（构建树结构）
     */
    List<Permission> getAllPermissions();

    /**
     * 根据角色ID列表查询权限列表
     */
    List<Permission> getPermissionsByRoleIds(List<Long> roleIds);

    /**
     * 根据角色ID查询权限标识列表（用于权限校验）
     */
    List<String> getPermissionKeys(Long roleId);

    /**
     * 根据多个角色ID查询所有权限标识（去重）
     */
    List<String> getPermissionKeysByRoleIds(List<Long> roleIds);

    /**
     * 构建菜单权限树（按角色过滤）
     */
    List<Map<String, Object>> getMenuTree(Long roleId);
}
