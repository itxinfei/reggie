package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.inventory.model.MaterialCategory;
import com.reggie.module.inventory.service.MaterialCategoryService;
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

@Slf4j
@RestController
@RequestMapping("/api/inventory/material-category")
@Tag(name = "食材分类管理")
public class MaterialCategoryController {

    @Autowired
    private MaterialCategoryService materialCategoryService;

    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询食材分类列表，按排序字段升序排列")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    public R<Page<MaterialCategory>> page(int page, int pageSize) {
        Page<MaterialCategory> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<MaterialCategory> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(MaterialCategory::getSort);
        materialCategoryService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增分类", description = "创建新的食材分类")
    public R<String> save(@RequestBody MaterialCategory category) {
        materialCategoryService.save(category);
        return R.success("新增分类成功");
    }

    @PutMapping
    @Operation(summary = "修改分类", description = "更新食材分类信息")
    public R<String> update(@RequestBody MaterialCategory category) {
        materialCategoryService.updateById(category);
        return R.success("修改分类成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "根据ID删除食材分类")
    @Parameter(name = "id", description = "分类ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        materialCategoryService.removeById(id);
        return R.success("删除分类成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询", description = "根据ID查询食材分类详情")
    @Parameter(name = "id", description = "分类ID", required = true)
    public R<MaterialCategory> get(@PathVariable Long id) {
        MaterialCategory category = materialCategoryService.getById(id);
        if (category == null) {
            return R.error("分类不存在");
        }
        return R.success(category);
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有", description = "查询所有食材分类列表，按排序字段升序排列")
    public R<List<MaterialCategory>> list() {
        LambdaQueryWrapper<MaterialCategory> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(MaterialCategory::getSort);
        return R.success(materialCategoryService.list(qw));
    }
}

