package com.reggie.module.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.sys.entity.Role;
import com.reggie.module.sys.entity.RolePermission;
import com.reggie.module.sys.mapper.RoleMapper;
import com.reggie.module.sys.mapper.RolePermissionMapper;
import com.reggie.module.sys.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 */
@Slf4j
/**
 * Role service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;
    @Override
    public Role getByRoleKey(String roleKey) {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getRoleKey, roleKey)
               .eq(Role::getIsDeleted, 0)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    @Override
    public List<Role> listEnabledByTenantId(Long tenantId) {
        return roleMapper.listEnabledByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        // 删除旧权限关联
        LambdaQueryWrapper<RolePermission> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(RolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(delWrapper);

        // 批量插入新权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            for (Long pid : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(pid);
                rp.setCreateTime(java.time.LocalDateTime.now());
                rolePermissionMapper.insert(rp);
            }
        }
        log.info("[角色权限] 角色{} 分配了{}个权限", roleId,
                permissionIds != null ? permissionIds.size() : 0);
    }

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        LambdaQueryWrapper<RolePermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RolePermission::getRoleId, roleId)
               .select(RolePermission::getPermissionId);
        List<RolePermission> rpList = rolePermissionMapper.selectList(wrapper);
        return rpList.stream()
                .map(RolePermission::getPermissionId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRoleOptions(Long tenantId) {
        List<Role> roles = listEnabledByTenantId(tenantId);
        return roles.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", r.getId());
            map.put("label", r.getRoleName());
            map.put("roleKey", r.getRoleKey());
            return map;
        }).collect(Collectors.toList());
    }
}







