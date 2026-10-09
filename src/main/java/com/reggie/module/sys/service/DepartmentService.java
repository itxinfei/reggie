package com.reggie.module.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.sys.dto.DepartmentSaveDTO;
import com.reggie.module.sys.dto.DepartmentUpdateDTO;
import com.reggie.module.sys.model.Department;

/**
 * 部门 Service
 * <p>租户安全约定与角色管理一致：租户归属由服务端 BaseContext 决定，
 * 前端不可篡改；跨租户操作一律拒绝。</p>
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 新增部门（挂当前租户）
     *
     * @param dto 部门信息
     * @return 是否成功
     */
    boolean addTenantDepartment(DepartmentSaveDTO dto);

    /**
     * 修改部门（先校验归属再更新业务字段）
     *
     * @param dto 部门信息（含 id）
     * @return 是否成功
     */
    boolean updateTenantDepartment(DepartmentUpdateDTO dto);

    /**
     * 删除部门（逻辑删除；有员工挂靠时拒绝删除）
     *
     * @param departmentId 部门ID
     * @return 是否成功
     */
    boolean deleteTenantDepartment(Long departmentId);
}
