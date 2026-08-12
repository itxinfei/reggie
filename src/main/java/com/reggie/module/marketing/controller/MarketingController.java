package com.reggie.module.marketing.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.marketing.model.FullReductionRule;
import com.reggie.module.marketing.model.DiscountRule;
import com.reggie.module.marketing.model.CampaignUsageRecord;
import com.reggie.module.marketing.service.MarketingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Marketing Activity Controller
 *
 * @author reggie
 * @since 2026-08-11
 */
@Slf4j
@RestController
@RequestMapping("/marketing")
@Tag(name = "Marketing Activity Management")
public class MarketingController {

    @Autowired
    private MarketingService marketingService;

    // ==================== Full Reduction Rule Management ====================

    @GetMapping("/full-reduction/list")
    @Operation(summary = "Get full reduction rule list")
    public R<List<FullReductionRule>> getFullReductionRules(
            @Parameter(description = "Campaign ID") @RequestParam(required = false) Long campaignId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<FullReductionRule> rules = marketingService.getFullReductionRules(campaignId, tenantId);
        return R.success(rules);
    }

    @PostMapping("/full-reduction")
    @Operation(summary = "Save full reduction rule")
    public R<String> saveFullReductionRule(@RequestBody FullReductionRule rule) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rule.setTenantId(tenantId);
        boolean success = marketingService.saveOrUpdateFullReductionRule(rule);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    @PutMapping("/full-reduction")
    @Operation(summary = "Update full reduction rule")
    public R<String> updateFullReductionRule(@RequestBody FullReductionRule rule) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rule.setTenantId(tenantId);
        boolean success = marketingService.saveOrUpdateFullReductionRule(rule);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    @DeleteMapping("/full-reduction/{id}")
    @Operation(summary = "Delete full reduction rule")
    public R<String> deleteFullReductionRule(@Parameter(description = "ID") @PathVariable Long id) {
        boolean success = marketingService.deleteFullReductionRule(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    @PostMapping("/full-reduction/batch")
    @Operation(summary = "Batch save full reduction rules")
    public R<String> batchSaveFullReductionRules(@RequestBody List<FullReductionRule> rules) {
        Long tenantId = BaseContext.getCurrentTenantId();
        for (FullReductionRule rule : rules) {
            rule.setTenantId(tenantId);
        }
        boolean success = marketingService.batchSaveFullReductionRules(rules);
        return success ? R.success("Batch save successful") : R.error("Batch save failed");
    }

    // ==================== Discount Rule Management ====================

    @GetMapping("/discount/list")
    @Operation(summary = "Get discount rule list")
    public R<List<DiscountRule>> getDiscountRules(
            @Parameter(description = "Campaign ID") @RequestParam(required = false) Long campaignId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<DiscountRule> rules = marketingService.getDiscountRules(campaignId, tenantId);
        return R.success(rules);
    }

    @PostMapping("/discount")
    @Operation(summary = "Save discount rule")
    public R<String> saveDiscountRule(@RequestBody DiscountRule rule) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rule.setTenantId(tenantId);
        boolean success = marketingService.saveOrUpdateDiscountRule(rule);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    @PutMapping("/discount")
    @Operation(summary = "Update discount rule")
    public R<String> updateDiscountRule(@RequestBody DiscountRule rule) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rule.setTenantId(tenantId);
        boolean success = marketingService.saveOrUpdateDiscountRule(rule);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    @DeleteMapping("/discount/{id}")
    @Operation(summary = "Delete discount rule")
    public R<String> deleteDiscountRule(@Parameter(description = "ID") @PathVariable Long id) {
        boolean success = marketingService.deleteDiscountRule(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    @PostMapping("/discount/batch")
    @Operation(summary = "Batch save discount rules")
    public R<String> batchSaveDiscountRules(@RequestBody List<DiscountRule> rules) {
        Long tenantId = BaseContext.getCurrentTenantId();
        for (DiscountRule rule : rules) {
            rule.setTenantId(tenantId);
        }
        boolean success = marketingService.batchSaveDiscountRules(rules);
        return success ? R.success("Batch save successful") : R.error("Batch save failed");
    }

    // ==================== Marketing Calculation ====================

    @PostMapping("/calculate/full-reduction")
    @Operation(summary = "Calculate full reduction discount")
    public R<BigDecimal> calculateFullReduction(
            @Parameter(description = "Campaign ID") @RequestParam Long campaignId,
            @Parameter(description = "Order amount") @RequestParam BigDecimal orderAmount) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        BigDecimal discount = marketingService.calculateFullReduction(campaignId, orderAmount, userId, tenantId);
        return R.success(discount);
    }

    @PostMapping("/calculate/discount")
    @Operation(summary = "Calculate discount")
    public R<BigDecimal> calculateDiscount(
            @Parameter(description = "Campaign ID") @RequestParam Long campaignId,
            @Parameter(description = "Order amount") @RequestParam BigDecimal orderAmount,
            @Parameter(description = "Dish ID list") @RequestBody List<Long> dishIds) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        BigDecimal discount = marketingService.calculateDiscount(campaignId, orderAmount, dishIds, userId, tenantId);
        return R.success(discount);
    }

    @PostMapping("/calculate/best")
    @Operation(summary = "Calculate best discount")
    public R<Map<String, Object>> calculateBestDiscount(
            @Parameter(description = "Order amount") @RequestParam BigDecimal orderAmount,
            @Parameter(description = "Dish ID list") @RequestBody List<Long> dishIds) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        Map<String, Object> result = marketingService.calculateBestDiscount(orderAmount, dishIds, userId, tenantId);
        return R.success(result);
    }

    // ==================== Usage Records ====================

    @GetMapping("/usage/list")
    @Operation(summary = "Get usage record list")
    public R<List<CampaignUsageRecord>> getUsageRecords(
            @Parameter(description = "Campaign ID") @RequestParam(required = false) Long campaignId,
            @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<CampaignUsageRecord> records = marketingService.getUsageRecords(campaignId, startDate, endDate, tenantId);
        return R.success(records);
    }

    @GetMapping("/usage/count")
    @Operation(summary = "Get user usage count")
    public R<Integer> getUserUsageCount(
            @Parameter(description = "Campaign ID") @RequestParam Long campaignId,
            @Parameter(description = "Rule ID") @RequestParam Long ruleId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        int count = marketingService.getUserUsageCount(campaignId, ruleId, userId, tenantId);
        return R.success(count);
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    @Operation(summary = "Get marketing statistics")
    public R<Map<String, Object>> getMarketingStatistics(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = marketingService.getMarketingStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    @GetMapping("/effect/full-reduction/{campaignId}")
    @Operation(summary = "Get full reduction effect")
    public R<Map<String, Object>> getFullReductionEffect(@Parameter(description = "Campaign ID") @PathVariable Long campaignId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> effect = marketingService.getFullReductionEffect(campaignId, tenantId);
        return R.success(effect);
    }

    @GetMapping("/effect/discount/{campaignId}")
    @Operation(summary = "Get discount effect")
    public R<Map<String, Object>> getDiscountEffect(@Parameter(description = "Campaign ID") @PathVariable Long campaignId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> effect = marketingService.getDiscountEffect(campaignId, tenantId);
        return R.success(effect);
    }

    @GetMapping("/trend")
    @Operation(summary = "Get marketing trend")
    public R<Map<String, Object>> getMarketingTrend(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = marketingService.getMarketingTrend(startDate, endDate, tenantId);
        return R.success(trend);
    }

    @GetMapping("/top")
    @Operation(summary = "Get top activities")
    public R<List<Map<String, Object>>> getTopActivities(
            @Parameter(description = "Limit") @RequestParam(defaultValue = "10") int limit) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> topActivities = marketingService.getTopActivities(limit, tenantId);
        return R.success(topActivities);
    }
}

