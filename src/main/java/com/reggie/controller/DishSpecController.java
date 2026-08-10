package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.entity.DishSpecGroup;
import com.reggie.entity.DishSpecOption;
import com.reggie.service.DishSpecService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 菜品规格控制器
 *
 * @author reggie
 * @since 2026-08-11
 */
@Slf4j
@RestController
@RequestMapping("/dish/spec")
@Tag(name = "菜品规格管理")
public class DishSpecController {

    @Autowired
    private DishSpecService dishSpecService;

    // ==================== 规格组管理 ====================

    @GetMapping("/group/list")
    @Operation(summary = "获取规格组列表")
    public R<List<DishSpecGroup>> getSpecGroups() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<DishSpecGroup> groups = dishSpecService.getSpecGroups(tenantId);
        return R.success(groups);
    }

    @GetMapping("/group/{id}")
    @Operation(summary = "获取规格组详情")
    @Parameter(description = "I d")
    public R<DishSpecGroup> getSpecGroupById(@PathVariable Long id) {
        DishSpecGroup group = dishSpecService.getSpecGroupById(id);
        return R.success(group);
    }

    @PostMapping("/group")
    @Operation(summary = "保存规格组")
    public R<String> saveSpecGroup(@RequestBody DishSpecGroup group) {
        Long tenantId = BaseContext.getCurrentTenantId();
        group.setTenantId(tenantId);
        boolean success = dishSpecService.saveOrUpdateSpecGroup(group);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @PutMapping("/group")
    @Operation(summary = "更新规格组")
    public R<String> updateSpecGroup(@RequestBody DishSpecGroup group) {
        Long tenantId = BaseContext.getCurrentTenantId();
        group.setTenantId(tenantId);
        boolean success = dishSpecService.saveOrUpdateSpecGroup(group);
        return success ? R.success("更新成功") : R.error("更新失败");
    }

    @DeleteMapping("/group/{id}")
    @Operation(summary = "删除规格组")
    @Parameter(description = "I d")
    public R<String> deleteSpecGroup(@PathVariable Long id) {
        boolean success = dishSpecService.deleteSpecGroup(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    // ==================== 规格选项管理 ====================

    @GetMapping("/option/list")
    @Operation(summary = "获取规格选项列表")
    public R<List<DishSpecOption>> getSpecOptions(
            @Parameter(description = "G r o u p I d")
            @Parameter(description = "规格组ID") @RequestParam Long groupId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<DishSpecOption> options = dishSpecService.getSpecOptions(groupId, tenantId);
        return R.success(options);
    }

    @PostMapping("/option")
    @Operation(summary = "保存规格选项")
    public R<String> saveSpecOption(@RequestBody DishSpecOption option) {
        Long tenantId = BaseContext.getCurrentTenantId();
        option.setTenantId(tenantId);
        boolean success = dishSpecService.saveOrUpdateSpecOption(option);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @PutMapping("/option")
    @Operation(summary = "更新规格选项")
    public R<String> updateSpecOption(@RequestBody DishSpecOption option) {
        Long tenantId = BaseContext.getCurrentTenantId();
        option.setTenantId(tenantId);
        boolean success = dishSpecService.saveOrUpdateSpecOption(option);
        return success ? R.success("更新成功") : R.error("更新失败");
    }

    @DeleteMapping("/option/{id}")
    @Operation(summary = "删除规格选项")
    @Parameter(description = "I d")
    public R<String> deleteSpecOption(@PathVariable Long id) {
        boolean success = dishSpecService.deleteSpecOption(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    @PostMapping("/option/batch")
    @Operation(summary = "批量保存规格选项")
    public R<String> batchSaveSpecOptions(@RequestBody List<DishSpecOption> options) {
        Long tenantId = BaseContext.getCurrentTenantId();
        for (DishSpecOption option : options) {
            option.setTenantId(tenantId);
        }
        boolean success = dishSpecService.batchSaveSpecOptions(options);
        return success ? R.success("批量保存成功") : R.error("批量保存失败");
    }

    // ==================== 菜品规格关联 ====================

    @GetMapping("/dish/{dishId}")
    @Operation(summary = "获取菜品规格组")
    @Parameter(description = "D i s h I d")
    public R<List<Map<String, Object>>> getDishSpecGroups(@PathVariable Long dishId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> groups = dishSpecService.getDishSpecGroups(dishId, tenantId);
        return R.success(groups);
    }

    @PostMapping("/dish/{dishId}")
    @Operation(summary = "设置菜品规格关联")
    public R<String> setDishSpecGroups(
            @Parameter(description = "Dish ID")
            @PathVariable Long dishId,
            @RequestBody List<Long> groupIds) {
        Long tenantId = BaseContext.getCurrentTenantId();
        boolean success = dishSpecService.setDishSpecGroups(dishId, groupIds, tenantId);
        return success ? R.success("设置成功") : R.error("设置失败");
    }

    @DeleteMapping("/dish/{dishId}")
    @Operation(summary = "删除菜品规格关联")
    @Parameter(description = "D i s h I d")
    public R<String> deleteDishSpecRelations(@PathVariable Long dishId) {
        boolean success = dishSpecService.deleteDishSpecRelations(dishId);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    // ==================== 规格价格计算 ====================

    @PostMapping("/price/calculate")
    @Operation(summary = "计算规格价格")
    public R<BigDecimal> calculateSpecPrice(
            @Parameter(description = "D i s h I d")
            @Parameter(description = "菜品ID") @RequestParam Long dishId,
            @Parameter(description = "基础价格") @RequestParam BigDecimal basePrice,
            @Parameter(description = "规格选项ID列表") @RequestBody List<Long> optionIds) {
        BigDecimal price = dishSpecService.calculateSpecPrice(dishId, basePrice, optionIds);
        return R.success(price);
    }

    @GetMapping("/detail/{dishId}")
    @Operation(summary = "获取菜品规格详情")
    @Parameter(description = "D i s h I d")
    public R<Map<String, Object>> getDishSpecDetail(@PathVariable Long dishId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> detail = dishSpecService.getDishSpecDetail(dishId, tenantId);
        return R.success(detail);
    }

    // ==================== 统计分析 ====================

    @GetMapping("/statistics")
    @Operation(summary = "获取规格统计")
    public R<Map<String, Object>> getSpecStatistics() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = dishSpecService.getSpecStatistics(tenantId);
        return R.success(statistics);
    }
}


