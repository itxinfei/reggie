package com.reggie.module.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.sys.dto.DepartmentSaveDTO;
import com.reggie.module.sys.dto.DepartmentUpdateDTO;
import com.reggie.module.sys.model.Department;
import com.reggie.module.sys.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;

/**
 * 部门管理 Controller（企业组织架构）
 * <p>小型餐饮店可不用；企业内部订餐场景下作为餐补发放、按部门对账、
 * 团餐预订的组织维度。操作权限与角色管理一致，仅超管可操作。</p>
 */
@Slf4j
@RequiresAdmin
@RestController
@RequestMapping("/sys/department")
@Tag(name = "系统管理-部门管理", description = "企业组织架构：部门CRUD接口")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 部门分页查询
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param name     部门名称（模糊）
     * @param status   状态：1=启用 0=禁用
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "部门分页查询")
    public R<Page<Department>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "部门名称") @RequestParam(required = false) String name,
            @Parameter(description = "状态：1=启用 0=禁用") @RequestParam(required = false) Integer status) {
        Page<Department> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) {
            wrapper.like(Department::getName, name);
        }
        if (status != null) {
            wrapper.eq(Department::getStatus, status);
        }
        wrapper.eq(Department::getIsDeleted, 0)
               .orderByAsc(Department::getSort)
               .orderByAsc(Department::getId);
        departmentService.page(pageInfo, wrapper);
        return R.success(pageInfo);
    }

    /**
     * 启用中的部门列表（下拉用）
     *
     * @return 部门列表
     */
    @GetMapping("/list")
    @Operation(summary = "启用中的部门列表（下拉用）")
    public R<List<Department>> list() {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getIsDeleted, 0)
               .eq(Department::getStatus, 1)
               .orderByAsc(Department::getSort)
               .orderByAsc(Department::getId);
        return R.success(departmentService.list(wrapper));
    }

    /**
     * 新增部门
     * <p>租户安全：使用 DepartmentSaveDTO 仅接收业务字段，tenantId 由 Service 层通过 BaseContext 强制设置。</p>
     *
     * @param dto 部门信息
     * @return 操作结果
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增部门")
    public R<String> add(@Parameter(description = "部门信息") @Valid @RequestBody DepartmentSaveDTO dto) {
        departmentService.addTenantDepartment(dto);
        return R.success("部门创建成功");
    }

    /**
     * 修改部门
     * <p>租户安全：Service 层先校验归属再更新业务字段。</p>
     *
     * @param dto 部门信息（含 id）
     * @return 操作结果
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改部门")
    public R<String> update(@Parameter(description = "部门信息") @Valid @RequestBody DepartmentUpdateDTO dto) {
        departmentService.updateTenantDepartment(dto);
        return R.success("部门更新成功");
    }

    /**
     * 删除部门（逻辑删除）
     * <p>仍有员工挂靠时拒绝删除，由 Service 层守卫。</p>
     *
     * @param id 部门ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除部门", description = "逻辑删除指定部门；部门下仍有员工时拒绝")
    public R<String> delete(@Parameter(description = "部门ID") @PathVariable Long id) {
        departmentService.deleteTenantDepartment(id);
        return R.success("部门删除成功");
    }
}
