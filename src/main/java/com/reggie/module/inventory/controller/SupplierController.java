package com.reggie.module.inventory.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.module.inventory.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.List;

/**
 * 供应商管理控制器
 * 提供供应商的增删改查接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RequireEmployee
@RestController
@RequestMapping("/api/inventory/supplier")
@Tag(name = "供应商管理")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    /**
     * 分页查询供应商列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 供应商名称（可选，模糊查询）
     * @param status 状态（可选）：0-禁用，1-启用
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询供应商列表，支持按名称搜索和状态筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "供应商名称（可选，模糊查询）")
    @Parameter(name = "status", description = "状态（可选）：0-禁用，1-启用")
    public R<Page<Supplier>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) Integer status) {
        Page<Supplier> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        qw.like(name != null && !name.isEmpty(), Supplier::getName, name);
        // 修改点：添加按状态筛选支持，修复前端 status 参数被后端静默丢弃的 Bug
        qw.eq(status != null, Supplier::getStatus, status);
        qw.orderByDesc(Supplier::getUpdateTime);
        supplierService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 新增供应商
     * <p>租户安全：强制设置 tenantId。</p>
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增供应商", description = "创建新的供应商信息")
    public R<String> save(@RequestBody Supplier supplier) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        supplier.setTenantId(tenantId);
        supplierService.save(supplier);
        return R.success("新增供应商成功");
    }

    /**
     * 修改供应商
     * <p>租户安全：先校验归属，再更新业务字段。</p>
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改供应商", description = "更新供应商信息")
    public R<String> update(@RequestBody Supplier supplier) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        Supplier exist = supplierService.getById(supplier.getId());
        if (exist == null) {
            throw new CustomException("供应商不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("供应商不属于当前租户");
        }
        exist.setName(supplier.getName());
        exist.setContact(supplier.getContact());
        exist.setPhone(supplier.getPhone());
        exist.setAddress(supplier.getAddress());
        exist.setStatus(supplier.getStatus());
        supplierService.updateById(exist);
        return R.success("修改供应商成功");
    }

    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除供应商", description = "根据ID删除供应商（先校验租户归属）")
    @Parameter(name = "id", description = "供应商ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        Supplier exist = supplierService.getById(id);
        if (exist == null) {
            throw new CustomException("供应商不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("供应商不属于当前租户");
        }
        supplierService.removeById(id);
        return R.success("删除供应商成功");
    }

    /**
     * 根据ID查询供应商
     * @param id 供应商ID
     * @return 供应商详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询", description = "根据ID查询供应商详情")
    @Parameter(name = "id", description = "供应商ID", required = true)
    public R<Supplier> get(@PathVariable Long id) {
        Supplier supplier = supplierService.getById(id);
        if (supplier == null) {
            return R.error("供应商不存在");
        }
        return R.success(supplier);
    }

    /**
     * 查询所有启用的供应商
     * @return 供应商列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有", description = "查询所有启用状态的供应商列表")
    public R<List<Supplier>> list() {
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        qw.eq(Supplier::getStatus, 1);
        qw.orderByAsc(Supplier::getName);
        return R.success(supplierService.list(qw));
    }
}

