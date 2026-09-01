package com.reggie.module.marketing.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.marketing.model.NewCustomerDiscount;
import com.reggie.module.marketing.model.BuyGetFree;
import com.reggie.module.marketing.model.FlashSale;
import com.reggie.module.marketing.service.MarketingToolService;
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
import javax.validation.Valid;
import com.reggie.common.RateLimit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Marketing Tool Controller
 * 
 * @author reggie
 * @since 2026-08-11
 */
@RestController
@RequestMapping("/marketing/tool")
@Tag(name = "营销工具管理")
@RequireEmployee
public class MarketingToolController {

    @Autowired
    private MarketingToolService marketingToolService;

    // ==================== New Customer Discount ====================

    @GetMapping("/new-customer/list")
    @Operation(summary = "查询新客立减列表")
    public R<List<NewCustomerDiscount>> getNewCustomerDiscounts() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<NewCustomerDiscount> list = marketingToolService.getNewCustomerDiscounts(tenantId);
        return R.success(list);
    }

    @PostMapping("/new-customer")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增新客立减")
    public R<String> saveNewCustomerDiscount(@Parameter(description = "新客立减信息", required = true) @Valid @RequestBody NewCustomerDiscount discount) {
        Long tenantId = BaseContext.getCurrentTenantId();
        discount.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateNewCustomerDiscount(discount);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    @PutMapping("/new-customer")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改新客立减")
    public R<String> updateNewCustomerDiscount(@Parameter(description = "新客立减信息（含ID）", required = true) @Valid @RequestBody NewCustomerDiscount discount) {
        Long tenantId = BaseContext.getCurrentTenantId();
        discount.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateNewCustomerDiscount(discount);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    @DeleteMapping("/new-customer/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除新客立减")
    public R<String> deleteNewCustomerDiscount(@Parameter(description = "新客立减ID", required = true) @PathVariable Long id) {
        boolean success = marketingToolService.deleteNewCustomerDiscount(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    @PostMapping("/new-customer/calculate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "计算新客立减优惠")
    public R<BigDecimal> calculateNewCustomerDiscount(
                        @Parameter(description = "订单金额", required = true) @RequestParam BigDecimal orderAmount) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        BigDecimal discount = marketingToolService.calculateNewCustomerDiscount(userId, orderAmount, tenantId);
        return R.success(discount);
    }

    // ==================== Buy Get Free ====================

    @GetMapping("/buy-get-free/list")
    @Operation(summary = "查询买赠活动列表")
    public R<List<BuyGetFree>> getBuyGetFreeActivities() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<BuyGetFree> list = marketingToolService.getBuyGetFreeActivities(tenantId);
        return R.success(list);
    }

    @PostMapping("/buy-get-free")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增买赠活动")
    public R<String> saveBuyGetFree(@Parameter(description = "买赠活动信息", required = true) @Valid @RequestBody BuyGetFree activity) {
        Long tenantId = BaseContext.getCurrentTenantId();
        activity.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateBuyGetFree(activity);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    @PutMapping("/buy-get-free")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改买赠活动")
    public R<String> updateBuyGetFree(@Parameter(description = "买赠活动信息（含ID）", required = true) @Valid @RequestBody BuyGetFree activity) {
        Long tenantId = BaseContext.getCurrentTenantId();
        activity.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateBuyGetFree(activity);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    @DeleteMapping("/buy-get-free/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除买赠活动")
    public R<String> deleteBuyGetFree(@Parameter(description = "买赠活动ID", required = true) @PathVariable Long id) {
        boolean success = marketingToolService.deleteBuyGetFree(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    @PostMapping("/buy-get-free/calculate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "计算买赠赠品")
    public R<Map<String, Object>> calculateBuyGetFreeGift(
                        @Parameter(description = "活动ID", required = true) @RequestParam Long activityId,
            @Parameter(description = "菜品ID", required = true) @RequestParam Long dishId,
            @Parameter(description = "数量", required = true) @RequestParam int quantity) {
        Map<String, Object> result = marketingToolService.calculateBuyGetFreeGift(activityId, dishId, quantity);
        return R.success(result);
    }

    // ==================== Flash Sale ====================

    @GetMapping("/flash-sale/list")
    @Operation(summary = "查询限时抢购列表")
    public R<List<FlashSale>> getFlashSales() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<FlashSale> list = marketingToolService.getFlashSales(tenantId);
        return R.success(list);
    }

    @PostMapping("/flash-sale")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增限时抢购")
    public R<String> saveFlashSale(@Parameter(description = "限时抢购信息", required = true) @Valid @RequestBody FlashSale flashSale) {
        Long tenantId = BaseContext.getCurrentTenantId();
        flashSale.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateFlashSale(flashSale);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    @PutMapping("/flash-sale")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改限时抢购")
    public R<String> updateFlashSale(@Parameter(description = "限时抢购信息（含ID）", required = true) @Valid @RequestBody FlashSale flashSale) {
        Long tenantId = BaseContext.getCurrentTenantId();
        flashSale.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateFlashSale(flashSale);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    @DeleteMapping("/flash-sale/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除限时抢购")
    public R<String> deleteFlashSale(@Parameter(description = "限时抢购ID", required = true) @PathVariable Long id) {
        boolean success = marketingToolService.deleteFlashSale(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    @GetMapping("/flash-sale/active")
    @Operation(summary = "查询进行中的限时抢购")
    public R<List<FlashSale>> getActiveFlashSales() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<FlashSale> list = marketingToolService.getActiveFlashSales(tenantId);
        return R.success(list);
    }

    @PostMapping("/flash-sale/calculate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "计算限时抢购价格")
    public R<Map<String, Object>> calculateFlashSalePrice(
                        @Parameter(description = "限时抢购ID", required = true) @RequestParam Long flashSaleId,
            @Parameter(description = "数量", required = true) @RequestParam int quantity) {
        Long userId = BaseContext.getCurrentId();
        Map<String, Object> result = marketingToolService.calculateFlashSalePrice(flashSaleId, userId, quantity);
        return R.success(result);
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    @Operation(summary = "营销工具统计")
    public R<Map<String, Object>> getMarketingToolStatistics() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = marketingToolService.getMarketingToolStatistics(tenantId);
        return R.success(statistics);
    }
}


