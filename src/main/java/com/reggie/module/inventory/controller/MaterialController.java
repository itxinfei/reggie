package com.reggie.module.inventory.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.service.MaterialService;
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
import java.util.List;

/**
 * 食材管理控制器
 * 提供食材的增删改查、库存预警等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RequireEmployee
@RestController
@RequestMapping("/api/inventory/material")
@Tag(name = "食材管理")
public class MaterialController {

    @Autowired
    private MaterialService materialService;

    /**
     * 分页查询食材列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 食材名称（可选，模糊查询）
     * @param categoryId 分类ID（可选）
     * @param status 状态（可选）：0-禁用，1-启用
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询食材列表，支持按名称搜索、分类筛选和状态筛选")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "食材名称（可选，模糊查询）")
    @Parameter(name = "categoryId", description = "分类ID（可选）")
    @Parameter(name = "status", description = "状态（可选）：0-禁用，1-启用")
    public R<Page<Material>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) Long categoryId,
                                   @Parameter(description = "S t a t u s")
                                   @RequestParam(required = false) String status) {
        Page<Material> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        qw.like(name != null && !name.isEmpty(), Material::getName, name);
        // 修改点：添加分类筛选支持，修复前端 categoryId 参数被后端静默丢弃的 Bug
        qw.eq(categoryId != null, Material::getCategoryId, categoryId);
        // 修改点：添加状态筛选支持，修复前端 status 参数被后端静默丢弃的 Bug
        qw.eq(status != null && !status.isEmpty(), Material::getStatus, status);
        qw.orderByDesc(Material::getUpdateTime);
        materialService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 新增食材
     * @param material 食材信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增食材", description = "创建新的食材信息")
    public R<String> save(@RequestBody Material material) {
        materialService.save(material);
        return R.success("新增食材成功");
    }

    /**
     * 修改食材
     * @param material 食材信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改食材", description = "更新食材信息")
    public R<String> update(@RequestBody Material material) {
        materialService.updateById(material);
        return R.success("修改食材成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除食材", description = "根据ID删除食材")
    @Parameter(name = "id", description = "食材ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        materialService.removeById(id);
        return R.success("删除食材成功");
    }

    /**
     * 根据ID查询食材
     * @param id 食材ID
     * @return 食材详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询", description = "根据ID查询食材详情")
    @Parameter(name = "id", description = "食材ID", required = true)
    public R<Material> get(@PathVariable Long id) {
        Material material = materialService.getById(id);
        if (material == null) {
            return R.error("食材不存在");
        }
        return R.success(material);
    }

    /**
     * 查询所有启用的食材
     * @return 食材列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有", description = "查询所有启用的食材列表")
    public R<List<Material>> list() {
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        qw.eq(Material::getStatus, 1);
        qw.orderByAsc(Material::getName);
        return R.success(materialService.list(qw));
    }

    /**
     * 查询库存预警食材列表
     * @return 低于预警阈值的食材列表
     */
    @GetMapping("/warning")
    @Operation(summary = "库存预警列表", description = "查询库存低于预警阈值的食材列表")
    public R<List<Material>> warning() {
        return R.success(materialService.checkWarning());
    }
}


