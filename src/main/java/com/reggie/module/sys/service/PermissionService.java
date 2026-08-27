package com.reggie.module.sys.service;

import com.reggie.module.sys.model.Permission;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 权限管理服务接口
 * </p>
 * <p>提供权限树构建、角色权限查询、菜单权限过滤等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface PermissionService {

    /**
     * 获取所有权限（构建树结构）
     *
     * @return 权限树列表
     */
    List<Permission> getAllPermissions();

    /**
     * 根据角色ID列表查询权限列表
     *
     * @param roleIds 角色ID列表
     * @return 权限列表
     */
    List<Permission> getPermissionsByRoleIds(List<Long> roleIds);

    /**
     * 根据角色ID查询权限标识列表（用于权限校验）
     *
     * @param roleId 角色ID
     * @return 权限标识列表
     */
    List<String> getPermissionKeys(Long roleId);

    /**
     * 根据多个角色ID查询所有权限标识（去重）
     *
     * @param roleIds 角色ID列表
     * @return 权限标识列表
     */
    List<String> getPermissionKeysByRoleIds(List<Long> roleIds);

    /**
     * 构建菜单权限树（按角色过滤）
     *
     * @param roleId 角色ID
     * @return 菜单权限树
     */
    List<Map<String, Object>> getMenuTree(Long roleId);

    /**
     * 清除指定角色的权限缓存
     *
     * @param roleId 角色ID
     */
    void clearPermissionCache(Long roleId);
}

