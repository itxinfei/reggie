package com.reggie.module.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.sys.model.Role;

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

    /**
     * 角色统计（SQL 聚合）
     * <p>域4 改造：从 RoleController 下沉</p>
     *
     * @return 角色总数、启用数、禁用数、已分配权限数
     */
    Map<String, Object> statRoles();

    /**
     * 逻辑删除角色并清理角色-权限关联（级联删除）
     * <p>域4 改造：从 RoleController 下沉，包含事务管理</p>
     *
     * @param roleId 角色 ID
     * @return 是否删除成功（角色不存在返回 false）
     */
    boolean deleteRoleByCascade(Long roleId);

    /**
     * 新增角色（租户安全）
     * <p>tenantId 从 BaseContext 强制取得，前端无法通过 DTO 字段篡改租户归属。</p>
     *
     * @param roleName    角色名称
     * @param roleKey     角色标识
     * @param description 角色描述
     * @param sort        排序
     * @param status      状态（0=禁用，1=启用）
     * @return 是否创建成功
     */
    boolean addTenantRole(String roleName, String roleKey, String description, Integer sort, Integer status);

    /**
     * 更新角色信息（租户安全）
     * <p>先通过 id 查询确认该角色属于当前租户，再仅更新业务字段，
     * 避免前端通过全实体覆盖 tenantId / roleKey 等敏感字段。</p>
     *
     * @param id          角色 ID
     * @param roleName    新角色名称
     * @param roleKey     新角色标识（允许修改但需校验唯一性）
     * @param description 新角色描述
     * @param sort        新排序
     * @param status      新状态
     * @return 是否更新成功
     */
    boolean updateTenantRole(Long id, String roleName, String roleKey, String description, Integer sort, Integer status);

    /**
     * 删除角色并校验租户归属（租户安全）
     * <p>先查询确认该角色属于当前租户，再执行级联删除。</p>
     *
     * @param roleId 角色 ID
     * @return 是否删除成功
     */
    boolean deleteTenantRole(Long roleId);

    /**
     * 为角色分配员工（多对多，删旧批插新，幂等）
     * <p>补全 RBAC 闭环：用户→角色。配合 {@link #assignPermissions}（角色→权限）
     * 实现"分配权限时可选用户"。tenantId 从 BaseContext 强制取得，防前端篡改租户归属。
     * 删除旧关联时按 role_id + tenant_id 过滤，防跨租户误删。</p>
     *
     * @param roleId       角色ID
     * @param employeeIds  员工ID列表（空列表表示清空该角色所有员工）
     */
    void assignUsersToRole(Long roleId, List<Long> employeeIds);

    /**
     * 查询角色已分配的员工ID列表
     *
     * @param roleId 角色ID
     * @return 员工ID列表
     */
    List<Long> getRoleUserIds(Long roleId);

    /**
     * 查询员工已分配的角色ID列表（供 PermissionAspect 多角色权限聚合用）
     * <p>tenantId 显式传入，与 MP 租户拦截器双保险；null 时不过滤（跨租户聚合场景）。</p>
     *
     * @param employeeId 员工ID
     * @param tenantId   租户ID
     * @return 角色ID列表
     */
    List<Long> getEmployeeRoleIds(Long employeeId, Long tenantId);
}

