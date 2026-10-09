package com.reggie.module.sys.service.impl;

import com.reggie.common.BaseContext;
import com.reggie.module.sys.mapper.RoleMapper;
import com.reggie.module.sys.model.Role;
import com.reggie.module.sys.service.MenuAccessService;
import com.reggie.module.sys.service.PermissionService;
import com.reggie.module.sys.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜单访问服务实现：权限加载逻辑与 PermissionAspect 同源（单一真源）。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Slf4j
@Service
public class MenuAccessServiceImpl implements MenuAccessService {

    @Autowired
    private RoleService roleService;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionService permissionService;

    @Override
    public Set<String> loadPermissionKeys(Long employeeId, String roleKey) {
        if (employeeId == null) {
            return Collections.emptySet();
        }
        try {
            Long tenantId = BaseContext.getCurrentTenantId();
            // RBAC 闭环：优先查 employee_role 显式关联角色（多对多），实现多角色权限聚合
            List<Long> roleIds = roleService.getEmployeeRoleIds(employeeId, tenantId);
            if (roleIds == null || roleIds.isEmpty()) {
                // 兼容老员工：未显式分配角色时 fallback 到 roleKey 内置角色（不 union，避免越权）
                Role role = roleMapper.findByRoleKeyAndTenantId(tenantId, roleKey);
                if (role == null) {
                    log.warn("[权限加载] 未找到角色：roleKey={}, employeeId={}", roleKey, employeeId);
                    return Collections.emptySet();
                }
                roleIds = new ArrayList<>();
                roleIds.add(role.getId());
            }

            List<String> permKeys = permissionService.getPermissionKeysByRoleIds(roleIds);
            if (permKeys == null || permKeys.isEmpty()) {
                log.warn("[权限加载] 角色无权限：roleIds={}, roleKey={}, employeeId={}",
                        roleIds, roleKey, employeeId);
                return Collections.emptySet();
            }
            return new HashSet<>(permKeys);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，菜单侧降级为空权限而非抛错
            log.error("[权限加载] 数据库查询异常：employeeId={}, roleKey={}, error={}",
                    employeeId, roleKey, e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    @Override
    public Map<String, Object> myAccess(Long employeeId, String roleKey) {
        boolean superAdmin = ADMIN_ROLE_KEY.equals(roleKey);
        Set<String> permissions = loadPermissionKeys(employeeId, roleKey);
        Map<String, Object> data = new HashMap<>(4);
        data.put("roleKey", roleKey);
        data.put("isSuperAdmin", superAdmin);
        data.put("permissions", permissions);
        return data;
    }
}
