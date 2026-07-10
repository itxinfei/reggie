package com.reggie.module.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.sys.entity.Permission;
import com.reggie.module.sys.entity.Role;
import com.reggie.module.sys.entity.RolePermission;
import com.reggie.module.sys.service.PermissionService;
import com.reggie.module.sys.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 角色管理Controller
 */
@Slf4j
@RestController
@RequestMapping("/sys/role")
@Tag(name = "系统管理-角色管理", description = "角色CRUD及权限分配接口")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    /**
     * 角色列表（分页）
     */
    @GetMapping("/page")
    @Operation(summary = "角色分页查询")
    public R<Page<Role>> page(int page, int pageSize, String roleName) {
        Page<Role> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        if (roleName != null && !roleName.isEmpty()) {
            wrapper.like(Role::getRoleName, roleName);
        }
        wrapper.eq(Role::getIsDeleted, 0)
               .orderByDesc(Role::getSort);
        roleService.page(pageInfo, wrapper);
        return R.success(pageInfo);
    }

    /**
     * 所有角色列表（下拉用）
     */
    @GetMapping("/list")
    @Operation(summary = "角色列表")
    public R<List<Role>> list() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getIsDeleted, 0)
               .eq(Role::getStatus, 1)
               .orderByDesc(Role::getSort);
        return R.success(roleService.list(wrapper));
    }

    /**
     * 新增角色
     */
    @PostMapping
    @Operation(summary = "新增角色")
    public R<String> add(@Valid @RequestBody Role role) {
        role.setIsDeleted(0);
        roleService.save(role);
        return R.success("角色创建成功");
    }

    /**
     * 修改角色
     */
    @PutMapping
    @Operation(summary = "修改角色")
    public R<String> update(@Valid @RequestBody Role role) {
        roleService.updateById(role);
        return R.success("角色更新成功");
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    public R<String> delete(@PathVariable Long id) {
        Role role = roleService.getById(id);
        if (role != null) {
            role.setIsDeleted(1);
            roleService.updateById(role);
        }
        return R.success("角色删除成功");
    }

    /**
     * 查询角色拥有的权限ID列表
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "查询角色权限")
    public R<List<Long>> getPermissions(@PathVariable Long id) {
        List<Long> permIds = roleService.getPermissionIds(id);
        return R.success(permIds);
    }

    /**
     * 为角色分配权限
     */
    @PutMapping("/{id}/permissions")
    @Operation(summary = "分配角色权限")
    public R<String> assignPermissions(@PathVariable Long id, @RequestBody Map<String, List<Long>> body) {
        List<Long> permissionIds = body.get("permissionIds");
        roleService.assignPermissions(id, permissionIds);

        // 清除权限缓存
        String cacheKey = "sys:permissions:" + id;
        // Redis template would clear here if available

        log.info("[角色权限] 角色{} 重新分配了{}个权限", id,
                permissionIds != null ? permissionIds.size() : 0);
        return R.success("权限分配成功");
    }

    /**
     * 获取所有权限（构建权限树）
     */
    @GetMapping("/permissions/tree")
    @Operation(summary = "获取权限树")
    public R<List<Permission>> permissionTree() {
        List<Permission> allPerms = permissionService.getAllPermissions();
        return R.success(allPerms);
    }
}
