package com.reggie.module.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.sys.entity.Role;
import com.reggie.module.sys.entity.RolePermission;
import com.reggie.module.sys.mapper.RoleMapper;
import com.reggie.module.sys.mapper.RolePermissionMapper;
import com.reggie.module.sys.service.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 角色服务实现
 */
@Slf4j
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
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    public Map<String, Object> statRoles() {
        return roleMapper.statRoles();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRoleByCascade(Long roleId) {
        Role role = this.getById(roleId);
        if (role == null) {
            return false;
        }
        // 租户归属校验：防止跨租户越权删除角色
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(role.getTenantId())) {
            throw new CustomException("无权删除其他租户的角色");
        }
        // 清理角色-权限关联
        LambdaQueryWrapper<RolePermission> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(RolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(delWrapper);

        role.setIsDeleted(1);
        this.updateById(role);
        log.info("[角色删除] 逻辑删除角色{}，同时清理权限关联", roleId);
        return true;
    }

    /**
     * 新增角色（租户安全）
     * <p>tenantId 从 BaseContext 强制取得，前端无法通过 DTO 字段篡改租户归属。
     * 若当前租户已存在同 roleKey 角色，则拒绝创建。</p>
     */
    @Override
    public boolean addTenantRole(String roleName, String roleKey, String description,
                                  Integer sort, Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法创建角色");
        }
        // 校验 roleKey 在当前租户下唯一
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getTenantId, tenantId)
               .eq(Role::getRoleKey, roleKey)
               .eq(Role::getIsDeleted, 0);
        Long count = Long.valueOf(this.count(wrapper));
        if (count > 0) {
            throw new CustomException("当前租户已存在角色标识 [" + roleKey + "]，请更换标识后重试");
        }
        Role role = new Role();
        role.setTenantId(tenantId);
        role.setRoleName(roleName);
        role.setRoleKey(roleKey);
        role.setDescription(description);
        role.setSort(sort != null ? sort : 0);
        role.setStatus(status != null ? status : 1);
        role.setIsDeleted(0);
        role.setCreateTime(java.time.LocalDateTime.now());
        role.setCreateUser(BaseContext.getCurrentId());
        return this.save(role);
    }

    /**
     * 更新角色信息（租户安全）
     * <p>先通过 id 查询确认该角色属于当前租户，再仅更新业务字段，
     * 避免前端通过全实体覆盖 tenantId / roleKey 等敏感字段。
     * roleKey 允许修改，但需校验新值在当前租户下唯一（且非自身已有值）。</p>
     */
    @Override
    public boolean updateTenantRole(Long id, String roleName, String roleKey,
                                     String description, Integer sort, Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法更新角色");
        }
        // 先按 tenantId + id 查询确认归属
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getId, id)
               .eq(Role::getTenantId, tenantId)
               .eq(Role::getIsDeleted, 0);
        Role existing = this.getOne(wrapper);
        if (existing == null) {
            throw new CustomException("角色不存在或不属于当前租户（id=" + id + "）");
        }
        // 若 roleKey 发生变更，校验新值唯一性
        if (roleKey != null && !roleKey.equals(existing.getRoleKey())) {
            LambdaQueryWrapper<Role> keyWrapper = new LambdaQueryWrapper<>();
            keyWrapper.eq(Role::getTenantId, tenantId)
                      .eq(Role::getRoleKey, roleKey)
                      .eq(Role::getIsDeleted, 0)
                      .ne(Role::getId, id);
            Long count = Long.valueOf(this.count(keyWrapper));
            if (count > 0) {
                throw new CustomException("角色标识 [" + roleKey + "] 已被其他角色使用");
            }
        }
        existing.setRoleName(roleName);
        existing.setRoleKey(roleKey);
        existing.setDescription(description);
        existing.setSort(sort);
        existing.setStatus(status);
        existing.setUpdateTime(java.time.LocalDateTime.now());
        existing.setUpdateUser(BaseContext.getCurrentId());
        return this.updateById(existing);
    }

    /**
     * 删除角色并校验租户归属（租户安全）
     * <p>先查询确认该角色属于当前租户，再执行级联删除，防止通过 ID 猜测跨租户删除。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTenantRole(Long roleId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法删除角色");
        }
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getId, roleId)
               .eq(Role::getTenantId, tenantId)
               .eq(Role::getIsDeleted, 0);
        Role existing = this.getOne(wrapper);
        if (existing == null) {
            throw new CustomException("角色不存在或不属于当前租户（id=" + roleId + "）");
        }
        // 清理角色-权限关联
        LambdaQueryWrapper<RolePermission> delWrapper = new LambdaQueryWrapper<>();
        delWrapper.eq(RolePermission::getRoleId, roleId);
        rolePermissionMapper.delete(delWrapper);
        // 逻辑删除
        existing.setIsDeleted(1);
        existing.setUpdateTime(java.time.LocalDateTime.now());
        existing.setUpdateUser(BaseContext.getCurrentId());
        this.updateById(existing);
        log.info("[角色删除] 租户{} 逻辑删除角色{}，同时清理权限关联", tenantId, roleId);
        return true;
    }
}






