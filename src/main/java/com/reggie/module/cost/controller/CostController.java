package com.reggie.module.cost.controller;

import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.cost.model.DishCost;
import com.reggie.module.cost.model.CostRecord;
import com.reggie.module.cost.model.LaborCost;
import com.reggie.module.cost.model.OtherCost;
import com.reggie.module.cost.service.CostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 成本核算控制器
 *
 * @author reggie
 * @since 2026-08-10
 */
@RestController
@RequestMapping("/cost")
@Tag(name = "成本核算管理")
@RequireEmployee
public class CostController {

    @Autowired
    private CostService costService;

    // ==================== 菜品成本管理 ====================

    @GetMapping("/dish/list")
    @Operation(summary = "获取菜品成本列表")
    public R<List<DishCost>> getDishCostList() {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        List<DishCost> list = costService.getDishCostList(tenantId);
        return R.success(list);
    }

    @GetMapping("/dish/{dishId}")
    @Operation(summary = "根据菜品ID获取成本")
    public R<DishCost> getDishCostByDishId(@Parameter(description = "菜品ID", required = true) @PathVariable Long dishId) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        DishCost dishCost = costService.getDishCostByDishId(dishId, tenantId);
        return R.success(dishCost);
    }

    @PostMapping("/dish")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "保存菜品成本")
    public R<String> saveDishCost(@Parameter(description = "菜品成本信息", required = true) @RequestBody DishCost dishCost) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        dishCost.setTenantId(tenantId);
        boolean success = costService.saveOrUpdateDishCost(dishCost);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @PutMapping("/dish")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "更新菜品成本")
    public R<String> updateDishCost(@Parameter(description = "菜品成本信息", required = true) @RequestBody DishCost dishCost) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        dishCost.setTenantId(tenantId);
        boolean success = costService.saveOrUpdateDishCost(dishCost);
        return success ? R.success("更新成功") : R.error("更新失败");
    }

    @DeleteMapping("/dish/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除菜品成本")
    public R<String> deleteDishCost(@Parameter(description = "菜品成本记录ID", required = true) @PathVariable Long id) {
        boolean success = costService.deleteDishCost(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    @PostMapping("/dish/batch")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量更新菜品成本")
    public R<String> batchUpdateDishCost(@Parameter(description = "菜品成本列表", required = true) @RequestBody List<DishCost> dishCosts) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        for (DishCost dishCost : dishCosts) {
            dishCost.setTenantId(tenantId);
        }
        boolean success = costService.batchUpdateDishCost(dishCosts);
        return success ? R.success("批量更新成功") : R.error("批量更新失败");
    }

    // ==================== 成本记录管理 ====================

    @GetMapping("/record/list")
    @Operation(summary = "获取成本记录列表")
    public R<List<CostRecord>> getCostRecordList(
                        @Parameter(description = "成本类型") @RequestParam(required = false) Integer costType,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        List<CostRecord> list = costService.getCostRecordList(costType, startDate, endDate, tenantId);
        return R.success(list);
    }

    @PostMapping("/record")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "保存成本记录")
    public R<String> saveCostRecord(@Parameter(description = "成本记录信息", required = true) @RequestBody CostRecord costRecord) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        costRecord.setTenantId(tenantId);
        costRecord.setCreateUser(com.reggie.common.BaseContext.getCurrentId());
        boolean success = costService.saveCostRecord(costRecord);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @DeleteMapping("/record/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除成本记录")
    public R<String> deleteCostRecord(@Parameter(description = "成本记录ID", required = true) @PathVariable Long id) {
        boolean success = costService.deleteCostRecord(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    // ==================== 人工成本管理 ====================

    @GetMapping("/labor/list")
    @Operation(summary = "获取人工成本列表")
    public R<List<LaborCost>> getLaborCostList(
                        @Parameter(description = "成本月份") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") LocalDate costMonth) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        List<LaborCost> list = costService.getLaborCostList(costMonth, tenantId);
        return R.success(list);
    }

    @PostMapping("/labor")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "保存人工成本")
    public R<String> saveLaborCost(@Parameter(description = "人工成本信息", required = true) @RequestBody LaborCost laborCost) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        laborCost.setTenantId(tenantId);
        boolean success = costService.saveOrUpdateLaborCost(laborCost);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @PutMapping("/labor")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "更新人工成本")
    public R<String> updateLaborCost(@Parameter(description = "人工成本信息", required = true) @RequestBody LaborCost laborCost) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        laborCost.setTenantId(tenantId);
        boolean success = costService.saveOrUpdateLaborCost(laborCost);
        return success ? R.success("更新成功") : R.error("更新失败");
    }

    @DeleteMapping("/labor/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除人工成本")
    public R<String> deleteLaborCost(@Parameter(description = "人工成本记录ID", required = true) @PathVariable Long id) {
        boolean success = costService.deleteLaborCost(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    @PostMapping("/labor/batch")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量保存人工成本")
    public R<String> batchSaveLaborCost(@Parameter(description = "人工成本列表", required = true) @RequestBody List<LaborCost> laborCosts) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        for (LaborCost laborCost : laborCosts) {
            laborCost.setTenantId(tenantId);
        }
        boolean success = costService.batchSaveLaborCost(laborCosts);
        return success ? R.success("批量保存成功") : R.error("批量保存失败");
    }

    // ==================== 其他成本管理 ====================

    @GetMapping("/other/list")
    @Operation(summary = "获取其他成本列表")
    public R<List<OtherCost>> getOtherCostList(
                        @Parameter(description = "成本类型") @RequestParam(required = false) Integer costType,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        List<OtherCost> list = costService.getOtherCostList(costType, startDate, endDate, tenantId);
        return R.success(list);
    }

    @PostMapping("/other")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "保存其他成本")
    public R<String> saveOtherCost(@Parameter(description = "其他成本信息", required = true) @RequestBody OtherCost otherCost) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        otherCost.setTenantId(tenantId);
        boolean success = costService.saveOrUpdateOtherCost(otherCost);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @PutMapping("/other")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "更新其他成本")
    public R<String> updateOtherCost(@Parameter(description = "其他成本信息", required = true) @RequestBody OtherCost otherCost) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        otherCost.setTenantId(tenantId);
        boolean success = costService.saveOrUpdateOtherCost(otherCost);
        return success ? R.success("更新成功") : R.error("更新失败");
    }

    @DeleteMapping("/other/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除其他成本")
    public R<String> deleteOtherCost(@Parameter(description = "其他成本记录ID", required = true) @PathVariable Long id) {
        boolean success = costService.deleteOtherCost(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    // ==================== 成本统计分析 ====================

    @GetMapping("/summary")
    @Operation(summary = "获取成本汇总统计")
    public R<Map<String, Object>> getCostSummary(
                        @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        Map<String, Object> summary = costService.getCostSummary(startDate, endDate, tenantId);
        return R.success(summary);
    }

    @GetMapping("/trend")
    @Operation(summary = "获取成本趋势分析")
    public R<Map<String, Object>> getCostTrend(
                        @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        Map<String, Object> trend = costService.getCostTrend(startDate, endDate, tenantId);
        return R.success(trend);
    }

    @GetMapping("/structure")
    @Operation(summary = "获取成本结构分析")
    public R<Map<String, Object>> getCostStructure(
                        @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        Map<String, Object> structure = costService.getCostStructure(startDate, endDate, tenantId);
        return R.success(structure);
    }

    @GetMapping("/dish/ranking")
    @Operation(summary = "获取菜品成本排行")
    public R<List<Map<String, Object>>> getDishCostRanking(
                        @Parameter(description = "排行数量") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        List<Map<String, Object>> ranking = costService.getDishCostRanking(limit, tenantId);
        return R.success(ranking);
    }

    @GetMapping("/dish/profit-rate/{dishId}")
    @Operation(summary = "计算菜品毛利率")
    public R<BigDecimal> calculateProfitRate(@Parameter(description = "菜品ID", required = true) @PathVariable Long dishId) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        BigDecimal profitRate = costService.calculateProfitRate(dishId, tenantId);
        return R.success(profitRate);
    }

    @GetMapping("/alert")
    @Operation(summary = "获取成本预警列表")
    public R<List<Map<String, Object>>> getCostAlert(
                        @Parameter(description = "预警阈值") @RequestParam(defaultValue = "20") BigDecimal threshold) {
        Long tenantId = com.reggie.common.BaseContext.getCurrentTenantId();
        List<Map<String, Object>> alerts = costService.getCostAlert(threshold, tenantId);
        return R.success(alerts);
    }
}



