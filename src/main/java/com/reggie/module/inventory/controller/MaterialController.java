package com.reggie.module.inventory.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.inventory.dto.BatchRestockDTO;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.vo.WarningMaterialVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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
import java.util.Map;

/**
 * 食材管理控制器
 * 提供食材的增删改查、库存预警等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
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
                                   @Parameter(description = "Status")
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
     * <p>租户安全：强制设置 tenantId。</p>
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增食材", description = "创建新的食材信息")
    public R<String> save(@RequestBody Material material) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        material.setTenantId(tenantId);
        materialService.save(material);
        return R.success("新增食材成功");
    }

    /**
     * 修改食材
     * <p>租户安全：先校验归属，再更新业务字段。</p>
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改食材", description = "更新食材信息")
    public R<String> update(@RequestBody Material material) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        Material exist = materialService.getById(material.getId());
        if (exist == null) {
            throw new CustomException("食材不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("食材不属于当前租户");
        }
        exist.setName(material.getName());
        exist.setCategoryId(material.getCategoryId());
        exist.setUnit(material.getUnit());
        exist.setStockQty(material.getStockQty());
        exist.setMinStock(material.getMinStock());
        exist.setStatus(material.getStatus());
        materialService.updateById(exist);
        return R.success("修改食材成功");
    }

    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除食材", description = "根据ID删除食材（先校验租户归属）")
    @Parameter(name = "id", description = "食材ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        Material exist = materialService.getById(id);
        if (exist == null) {
            throw new CustomException("食材不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("食材不属于当前租户");
        }
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
    @Operation(summary = "库存预警列表（简版）", description = "查询库存低于预警阈值的食材列表（简版）")
    public R<List<Material>> warning() {
        return R.success(materialService.checkWarning());
    }

    /**
     * 预警食材分页查询，带严重度分级和阈值比例
     */
    @GetMapping("/warning/page")
    @Operation(summary = "库存预警分页", description = "预警食材分页查询，带严重度分级和阈值比例")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "categoryId", description = "分类ID（可选）")
    @Parameter(name = "severity", description = "严重度（可选）：CRITICAL/WARNING/LOW")
    public R<Page<WarningMaterialVO>> warningPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String severity) {
        return R.success(materialService.warningPage(page, pageSize, categoryId, severity));
    }

    /**
     * 预警聚合统计
     */
    @GetMapping("/warning-stats")
    @Operation(summary = "预警统计", description = "预警食材聚合统计，按严重度和分类分组")
    public R<Map<String, Object>> warningStats() {
        return R.success(materialService.warningStats());
    }

    /**
     * 补货建议：基于历史出库量计算建议采购量
     */
    @GetMapping("/replenish-suggest")
    @Operation(summary = "补货建议", description = "基于历史出库量计算日均消耗，给出建议采购量")
    @Parameter(name = "days", description = "统计天数", required = false, example = "30")
    public R<List<Map<String, Object>>> replenishSuggest(
            @RequestParam(defaultValue = "30") int days) {
        return R.success(materialService.replenishSuggest(days));
    }

    /**
     * 批量补货：创建采购单并自动入库
     */
    @PostMapping("/batch-restock")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量补货", description = "批量补货：创建采购单并自动入库")
    public R<Long> batchRestock(@Validated @RequestBody BatchRestockDTO dto) {
        Long orderId = materialService.batchRestock(dto);
        return R.success(orderId);
    }
}


