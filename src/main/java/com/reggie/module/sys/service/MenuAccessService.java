package com.reggie.module.sys.service;

import java.util.Map;
import java.util.Set;

/**
 * 菜单访问服务：解析当前员工的角色与权限，供后台菜单按角色过滤。
 * <p>
 * 权限加载逻辑与 {@code PermissionAspect} 共用同一实现（单一真源），
 * 避免菜单可见性与接口鉴权两套口径不一致导致"菜单能进、接口被拦"。</p>
 *
 * @author reggie
 * @since 2026-09-28
 */
public interface MenuAccessService {

    /** 超级管理员角色标识（与 PermissionAspect 对齐） */
    String ADMIN_ROLE_KEY = "SUPER_ADMIN";

    /**
     * 按角色加载权限 key 集合（与 PermissionAspect 同源）。
     *
     * @param employeeId 员工ID
     * @param roleKey    角色标识（登录时写入会话）
     * @return 权限 key 集合；异常或无权限时返回空集合（安全降级，不放行）
     */
    Set<String> loadPermissionKeys(Long employeeId, String roleKey);

    /**
     * 当前员工菜单访问信息。
     *
     * @param employeeId 员工ID
     * @param roleKey    角色标识
     * @return {@code roleKey / isSuperAdmin / permissions}
     */
    Map<String, Object> myAccess(Long employeeId, String roleKey);
}
