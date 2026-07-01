package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/inventory/material")
@Tag(name = "食材管理")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    @GetMapping("/page")
    @Operation(summary = "分页查询")
    public R<Page<Material>> page(int page, int pageSize, String name) {
        Page<Material> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        qw.like(name != null && !name.isEmpty(), Material::getName, name);
        qw.orderByDesc(Material::getUpdatedTime);
        materialService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    @PostMapping
    @Operation(summary = "新增食材")
    public R<String> save(@RequestBody Material material) {
        materialService.save(material);
        return R.success("新增食材成功");
    }

    @PutMapping
    @Operation(summary = "修改食材")
    public R<String> update(@RequestBody Material material) {
        materialService.updateById(material);
        return R.success("修改食材成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除食材")
    public R<String> delete(@PathVariable Long id) {
        materialService.removeById(id);
        return R.success("删除食材成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据id查询")
    public R<Material> get(@PathVariable Long id) {
        Material material = materialService.getById(id);
        if (material == null) {
            return R.error("食材不存在");
        }
        return R.success(material);
    }

    @GetMapping("/list")
    @Operation(summary = "查询所有")
    public R<List<Material>> list() {
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        qw.eq(Material::getStatus, 1);
        qw.orderByAsc(Material::getName);
        return R.success(materialService.list(qw));
    }

    @GetMapping("/warning")
    @Operation(summary = "库存预警列表")
    public R<List<Material>> warning() {
        return R.success(materialService.checkWarning());
    }
}
