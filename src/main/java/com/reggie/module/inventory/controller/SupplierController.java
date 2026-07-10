package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.module.inventory.service.SupplierService;
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
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 供应商管理控制器
 * 提供供应商的增删改查接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory/supplier")
@Tag(name = "供应商管理")
public class SupplierController {

    @Autowired
    private SupplierService supplierService;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询供应商列表，支持按名称搜索")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "供应商名称（可选，模糊查询）")
    public R<Page<Supplier>> page(int page, int pageSize, String name) {
        Page<Supplier> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        qw.like(name != null && !name.isEmpty(), Supplier::getName, name);
        qw.orderByDesc(Supplier::getUpdatedTime);
        supplierService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增供应商", description = "创建新的供应商信息")
    public R<String> save(@RequestBody Supplier supplier) {
        supplierService.save(supplier);
        return R.success("新增供应商成功");
    }

    @PutMapping
    @Operation(summary = "修改供应商", description = "更新供应商信息")
    public R<String> update(@RequestBody Supplier supplier) {
        supplierService.updateById(supplier);
        return R.success("修改供应商成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除供应商", description = "根据ID删除供应商")
    @Parameter(name = "id", description = "供应商ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        supplierService.removeById(id);
        return R.success("删除供应商成功");
    }

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

    @GetMapping("/list")
    @Operation(summary = "查询所有", description = "查询所有启用状态的供应商列表")
    public R<List<Supplier>> list() {
        LambdaQueryWrapper<Supplier> qw = new LambdaQueryWrapper<>();
        qw.eq(Supplier::getStatus, 1);
        qw.orderByAsc(Supplier::getName);
        return R.success(supplierService.list(qw));
    }
}

