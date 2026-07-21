package com.reggie.module.sys.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.sys.entity.Permission;
import com.reggie.module.sys.entity.Role;
import com.reggie.module.sys.entity.RolePermission;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.common.aspect.PermissionAspect;
import com.reggie.module.sys.mapper.RoleMapper;
import com.reggie.module.sys.mapper.RolePermissionMapper;
import com.reggie.module.sys.service.PermissionService;
import com.reggie.module.sys.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>
 * 角色管理Controller
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RequiresAdmin
@RestController
@RequestMapping("/sys/role")
@Tag(name = "系统管理-角色管理", description = "角色CRUD及权限分配接口")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private PermissionAspect permissionAspect;

    /**
     * 角色分页查询
     * @param page 页码
     * @param pageSize 每页条数
     * @param roleName 角色名称
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "角色分页查询")
    public R<Page<Role>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "角色名称") @RequestParam(required = false) String roleName) {
        Page<Role> pageInfo = PageUtils.of(page, pageSize);
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
     * 角色统计
     * <p>使用 SQL 聚合替代前端 pageSize:999 拉全量遍历，避免全表扫描</p>
     *
     * @return 角色总数、启用数、禁用数、已分配权限数
     */
    @GetMapping("/stats")
    @Operation(summary = "角色统计", description = "聚合统计角色总数、启用数、禁用数、已分配权限角色数")
    public R<Map<String, Object>> stats() {
        Map<String, Object> stats = roleMapper.statRoles();
        if (stats == null) {
            stats = new HashMap<>();
        }
        return R.success(stats);
    }

    /**
     * 所有角色列表（下拉用）
     * @return 角色列表
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
     * @param role 角色信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增角色", description = "创建新角色，需提供角色名称和编码")
    public R<String> add(
            @Parameter(description = "角色信息") @Valid @RequestBody Role role) {
        role.setIsDeleted(0);
        roleService.save(role);
        return R.success("角色创建成功");
    }

    /**
     * 修改角色
     * @param role 角色信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改角色", description = "更新角色信息")
    public R<String> update(
            @Parameter(description = "角色信息") @Valid @RequestBody Role role) {
        roleService.updateById(role);
        return R.success("角色更新成功");
    }

    /**
     * 删除角色（逻辑删除）
     * @param id 角色ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色", description = "逻辑删除指定角色并清理角色-权限关联")
    public R<String> delete(@Parameter(description = "角色ID") @PathVariable Long id) {
        // 修改点：逻辑删除角色的同时清理 role_permission 关联，避免产生孤儿数据
        rolePermissionMapper.delete(
            new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
        Role role = roleService.getById(id);
        if (role != null) {
            role.setIsDeleted(1);
            roleService.updateById(role);
        }
        // 角色权限变更，清除角色与员工权限缓存
        permissionService.clearPermissionCache(id);
        permissionAspect.clearAllEmployeePermissionCache();
        return R.success("角色删除成功");
    }

    /**
     * 查询角色拥有的权限ID列表
     * @param id 角色ID
     * @return 权限ID列表
     */
    @GetMapping("/{id}/permissions")
    @Operation(summary = "查询角色权限", description = "获取指定角色已分配的权限ID列表")
    public R<List<Long>> getPermissions(@Parameter(description = "角色ID") @PathVariable Long id) {
        List<Long> permIds = roleService.getPermissionIds(id);
        return R.success(permIds);
    }

    /**
     * 为角色分配权限
     * @param id 角色ID
     * @param body 权限ID列表
     * @return 操作结果
     */
    @PutMapping("/{id}/permissions")
    @Operation(summary = "分配角色权限", description = "为角色批量分配权限")
    public R<String> assignPermissions(
            @Parameter(description = "角色ID") @PathVariable Long id,
            @Parameter(description = "权限ID列表") @RequestBody Map<String, List<Long>> body) {
        List<Long> permissionIds = body.get("permissionIds");
        roleService.assignPermissions(id, permissionIds);

        // 修改点：权限分配后真正清除缓存，确保新权限立即生效
        permissionService.clearPermissionCache(id);
        permissionAspect.clearAllEmployeePermissionCache();

        log.info("[角色权限] 角色{} 重新分配了{}个权限", id,
                permissionIds != null ? permissionIds.size() : 0);
        return R.success("权限分配成功");
    }

    /**
     * 获取所有权限（构建权限树）
     * @return 权限列表
     */
    @GetMapping("/permissions/tree")
    @Operation(summary = "获取权限树")
    public R<List<Permission>> permissionTree() {
        List<Permission> allPerms = permissionService.getAllPermissions();
        return R.success(allPerms);
    }

    /**
     * 获取筛选下拉选项（角色名称列表）
     * @return 包含角色名称列表的Map
     */
    @GetMapping("/options")
    @Operation(summary = "筛选选项", description = "获取所有角色名称，供搜索条件下拉框使用")
    public R<Map<String, List<String>>> options() {
        List<Role> list = roleService.list();
        Set<String> nameSet = new HashSet<>();
        for (Role role : list) {
            if (role.getRoleName() != null && !role.getRoleName().isEmpty()) {
                nameSet.add(role.getRoleName());
            }
        }
        Map<String, List<String>> result = new HashMap<>();
        result.put("names", new ArrayList<>(nameSet));
        return R.success(result);
    }
}
