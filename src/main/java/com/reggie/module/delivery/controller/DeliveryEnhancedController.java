package com.reggie.module.delivery.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.delivery.model.DeliveryRangeRule;
import com.reggie.module.delivery.model.DeliveryFeeStep;
import com.reggie.module.delivery.service.DeliveryEnhancedService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 配送增强控制器
 *
 * @author reggie
 * @since 2026-08-11
 */
@RestController
@RequestMapping("/delivery/enhanced")
@Tag(name = "配送增强管理")
@RequireEmployee
public class DeliveryEnhancedController {

    @Autowired
    private DeliveryEnhancedService deliveryEnhancedService;

    // ==================== 配送范围管理 ====================

    @GetMapping("/range/list")
    @Operation(summary = "获取配送范围规则列表")
    public R<List<DeliveryRangeRule>> getRangeRules() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<DeliveryRangeRule> rules = deliveryEnhancedService.getRangeRules(tenantId);
        return R.success(rules);
    }

    @GetMapping("/range/{id}")
    @Operation(summary = "获取配送范围规则详情")
    public R<DeliveryRangeRule> getRangeRuleById(@PathVariable Long id) {
        DeliveryRangeRule rule = deliveryEnhancedService.getRangeRuleById(id);
        return R.success(rule);
    }

    @PostMapping("/range")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "保存配送范围规则")
    public R<String> saveRangeRule(@RequestBody DeliveryRangeRule rule) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rule.setTenantId(tenantId);
        boolean success = deliveryEnhancedService.saveOrUpdateRangeRule(rule);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @PutMapping("/range")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "更新配送范围规则")
    public R<String> updateRangeRule(@RequestBody DeliveryRangeRule rule) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rule.setTenantId(tenantId);
        boolean success = deliveryEnhancedService.saveOrUpdateRangeRule(rule);
        return success ? R.success("更新成功") : R.error("更新失败");
    }

    @DeleteMapping("/range/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除配送范围规则")
    public R<String> deleteRangeRule(@PathVariable Long id) {
        boolean success = deliveryEnhancedService.deleteRangeRule(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    // ==================== 配送费阶梯管理 ====================

    @GetMapping("/fee-step/list")
    @Operation(summary = "获取配送费阶梯规则列表")
    public R<List<DeliveryFeeStep>> getFeeSteps(
                        @Parameter(description = "规则ID") @RequestParam Long ruleId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<DeliveryFeeStep> steps = deliveryEnhancedService.getFeeSteps(ruleId, tenantId);
        return R.success(steps);
    }

    @PostMapping("/fee-step")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "保存配送费阶梯规则")
    public R<String> saveFeeStep(@RequestBody DeliveryFeeStep step) {
        Long tenantId = BaseContext.getCurrentTenantId();
        step.setTenantId(tenantId);
        boolean success = deliveryEnhancedService.saveOrUpdateFeeStep(step);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @PutMapping("/fee-step")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "更新配送费阶梯规则")
    public R<String> updateFeeStep(@RequestBody DeliveryFeeStep step) {
        Long tenantId = BaseContext.getCurrentTenantId();
        step.setTenantId(tenantId);
        boolean success = deliveryEnhancedService.saveOrUpdateFeeStep(step);
        return success ? R.success("更新成功") : R.error("更新失败");
    }

    @DeleteMapping("/fee-step/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除配送费阶梯规则")
    public R<String> deleteFeeStep(@PathVariable Long id) {
        boolean success = deliveryEnhancedService.deleteFeeStep(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    @PostMapping("/fee-step/batch")
    @RateLimit(maxRequestsPerSecond = 3)
    @Operation(summary = "批量保存配送费阶梯规则")
    public R<String> batchSaveFeeSteps(@RequestBody List<DeliveryFeeStep> steps) {
        Long tenantId = BaseContext.getCurrentTenantId();
        for (DeliveryFeeStep step : steps) {
            step.setTenantId(tenantId);
        }
        boolean success = deliveryEnhancedService.batchSaveFeeSteps(steps);
        return success ? R.success("批量保存成功") : R.error("批量保存失败");
    }

    // ==================== 配送范围校验 ====================

    @PostMapping("/range/check")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "校验地址是否在配送范围内")
    public R<Boolean> isInRange(
                        @Parameter(description = "规则ID") @RequestParam Long ruleId,
            @Parameter(description = "经度") @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度") @RequestParam BigDecimal latitude) {
        boolean inRange = deliveryEnhancedService.isInRange(ruleId, longitude, latitude);
        return R.success(inRange);
    }

    @PostMapping("/range/find")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "查找匹配的配送范围规则")
    public R<Long> findMatchingRule(
                        @Parameter(description = "经度") @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度") @RequestParam BigDecimal latitude) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long ruleId = deliveryEnhancedService.findMatchingRule(longitude, latitude, tenantId);
        return R.success(ruleId);
    }

    // ==================== 配送费计算 ====================

    @PostMapping("/fee/calculate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "计算配送费")
    public R<BigDecimal> calculateDeliveryFee(
                        @Parameter(description = "规则ID") @RequestParam Long ruleId,
            @Parameter(description = "距离（米）") @RequestParam BigDecimal distance,
            @Parameter(description = "订单金额") @RequestParam BigDecimal orderAmount) {
        BigDecimal fee = deliveryEnhancedService.calculateDeliveryFee(ruleId, distance, orderAmount);
        return R.success(fee);
    }

    @PostMapping("/fee/auto-calculate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "自动计算配送费")
    public R<Map<String, Object>> calculateFee(
                        @Parameter(description = "经度") @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度") @RequestParam BigDecimal latitude,
            @Parameter(description = "距离（米）") @RequestParam BigDecimal distance,
            @Parameter(description = "订单金额") @RequestParam BigDecimal orderAmount) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> result = deliveryEnhancedService.calculateFee(longitude, latitude, distance, orderAmount, tenantId);
        return R.success(result);
    }

    @PostMapping("/distance/calculate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "计算两点间距离")
    public R<BigDecimal> calculateDistance(
                        @Parameter(description = "经度1") @RequestParam BigDecimal lon1,
            @Parameter(description = "纬度1") @RequestParam BigDecimal lat1,
            @Parameter(description = "经度2") @RequestParam BigDecimal lon2,
            @Parameter(description = "纬度2") @RequestParam BigDecimal lat2) {
        BigDecimal distance = deliveryEnhancedService.calculateDistance(lon1, lat1, lon2, lat2);
        return R.success(distance);
    }

    // ==================== 统计分析 ====================

    @GetMapping("/statistics")
    @Operation(summary = "获取配送统计")
    public R<Map<String, Object>> getDeliveryStatistics() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = deliveryEnhancedService.getDeliveryStatistics(tenantId);
        return R.success(statistics);
    }

    @GetMapping("/coverage")
    @Operation(summary = "获取配送范围覆盖分析")
    public R<Map<String, Object>> getRangeCoverage() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> coverage = deliveryEnhancedService.getRangeCoverage(tenantId);
        return R.success(coverage);
    }
}


