package com.reggie.module.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.auth.service.EmployeeService;
import com.reggie.module.sys.dto.DepartmentSaveDTO;
import com.reggie.module.sys.dto.DepartmentUpdateDTO;
import com.reggie.module.sys.mapper.DepartmentMapper;
import com.reggie.module.sys.model.Department;
import com.reggie.module.sys.service.DepartmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 部门 Service 实现
 * <p>租户安全：新增强制挂当前租户；修改/删除先按 id + tenantId 校验归属。
 * 删除守卫：仍有员工挂靠该部门时拒绝，避免员工表出现悬挂部门引用。</p>
 */
@Slf4j
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
        implements DepartmentService {

    @Autowired
    private EmployeeService employeeService;

    @Override
    public boolean addTenantDepartment(DepartmentSaveDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法创建部门");
        }
        checkNameDuplicate(tenantId, dto.getName(), null);

        Department dept = new Department();
        dept.setTenantId(tenantId);
        dept.setName(dto.getName());
        dept.setCode(dto.getCode());
        dept.setLeaderName(dto.getLeaderName());
        dept.setRemark(dto.getRemark());
        dept.setSort(dto.getSort() != null ? dto.getSort() : 0);
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        dept.setIsDeleted(0);
        dept.setCreateTime(LocalDateTime.now());
        dept.setCreateUser(BaseContext.getCurrentId());
        return this.save(dept);
    }

    @Override
    public boolean updateTenantDepartment(DepartmentUpdateDTO dto) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法修改部门");
        }
        Department existing = getOwnedDepartment(dto.getId(), tenantId);
        if (dto.getName() != null) {
            checkNameDuplicate(tenantId, dto.getName(), dto.getId());
        }

        existing.setName(dto.getName() != null ? dto.getName() : existing.getName());
        existing.setCode(dto.getCode());
        existing.setLeaderName(dto.getLeaderName());
        existing.setRemark(dto.getRemark());
        if (dto.getSort() != null) {
            existing.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            existing.setStatus(dto.getStatus());
        }
        existing.setUpdateTime(LocalDateTime.now());
        existing.setUpdateUser(BaseContext.getCurrentId());
        return this.updateById(existing);
    }

    @Override
    public boolean deleteTenantDepartment(Long departmentId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在，无法删除部门");
        }
        getOwnedDepartment(departmentId, tenantId);

        // 删除守卫：仍有在职员工挂靠时拒绝（员工表无逻辑删除，直接计数）
        LambdaQueryWrapper<Employee> empWrapper = new LambdaQueryWrapper<>();
        empWrapper.eq(Employee::getDepartmentId, departmentId)
                  .eq(Employee::getTenantId, tenantId);
        long empCount = employeeService.count(empWrapper);
        if (empCount > 0) {
            throw new CustomException("该部门下仍有 " + empCount + " 名员工，请先在员工管理中调整其部门后再删除");
        }
        return this.removeById(departmentId);
    }

    /** 校验部门归属当前租户，不存在/越权一律抛异常。 */
    private Department getOwnedDepartment(Long id, Long tenantId) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getId, id)
               .eq(Department::getTenantId, tenantId)
               .eq(Department::getIsDeleted, 0);
        Department existing = this.getOne(wrapper);
        if (existing == null) {
            throw new CustomException("部门不存在或不属于当前租户（id=" + id + "）");
        }
        return existing;
    }

    /** 同租户下部门名称唯一（新增时 excludeId 传 null，更新时排除自身）。 */
    private void checkNameDuplicate(Long tenantId, String name, Long excludeId) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getTenantId, tenantId)
               .eq(Department::getName, name)
               .eq(Department::getIsDeleted, 0)
               .ne(excludeId != null, Department::getId, excludeId);
        if (this.count(wrapper) > 0) {
            throw new CustomException("当前租户已存在部门 [" + name + "]，请更换名称后重试");
        }
    }
}
