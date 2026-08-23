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
@Tag(name = "Marketing Tool Management")
@RequireEmployee
public class MarketingToolController {

    @Autowired
    private MarketingToolService marketingToolService;

    // ==================== New Customer Discount ====================

    @GetMapping("/new-customer/list")
    @Operation(summary = "Get new customer discount list")
    public R<List<NewCustomerDiscount>> getNewCustomerDiscounts() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<NewCustomerDiscount> list = marketingToolService.getNewCustomerDiscounts(tenantId);
        return R.success(list);
    }

    @PostMapping("/new-customer")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Save new customer discount")
    public R<String> saveNewCustomerDiscount(@RequestBody NewCustomerDiscount discount) {
        Long tenantId = BaseContext.getCurrentTenantId();
        discount.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateNewCustomerDiscount(discount);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    @PutMapping("/new-customer")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Update new customer discount")
    public R<String> updateNewCustomerDiscount(@RequestBody NewCustomerDiscount discount) {
        Long tenantId = BaseContext.getCurrentTenantId();
        discount.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateNewCustomerDiscount(discount);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    @DeleteMapping("/new-customer/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Delete new customer discount")
    public R<String> deleteNewCustomerDiscount(@PathVariable Long id) {
        boolean success = marketingToolService.deleteNewCustomerDiscount(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    @PostMapping("/new-customer/calculate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Calculate new customer discount")
    public R<BigDecimal> calculateNewCustomerDiscount(
                        @Parameter(description = "Order amount") @RequestParam BigDecimal orderAmount) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        BigDecimal discount = marketingToolService.calculateNewCustomerDiscount(userId, orderAmount, tenantId);
        return R.success(discount);
    }

    // ==================== Buy Get Free ====================

    @GetMapping("/buy-get-free/list")
    @Operation(summary = "Get buy get free activity list")
    public R<List<BuyGetFree>> getBuyGetFreeActivities() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<BuyGetFree> list = marketingToolService.getBuyGetFreeActivities(tenantId);
        return R.success(list);
    }

    @PostMapping("/buy-get-free")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Save buy get free activity")
    public R<String> saveBuyGetFree(@RequestBody BuyGetFree activity) {
        Long tenantId = BaseContext.getCurrentTenantId();
        activity.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateBuyGetFree(activity);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    @PutMapping("/buy-get-free")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Update buy get free activity")
    public R<String> updateBuyGetFree(@RequestBody BuyGetFree activity) {
        Long tenantId = BaseContext.getCurrentTenantId();
        activity.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateBuyGetFree(activity);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    @DeleteMapping("/buy-get-free/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Delete buy get free activity")
    public R<String> deleteBuyGetFree(@PathVariable Long id) {
        boolean success = marketingToolService.deleteBuyGetFree(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    @PostMapping("/buy-get-free/calculate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Calculate buy get free gift")
    public R<Map<String, Object>> calculateBuyGetFreeGift(
                        @Parameter(description = "Activity ID") @RequestParam Long activityId,
            @Parameter(description = "Dish ID") @RequestParam Long dishId,
            @Parameter(description = "Quantity") @RequestParam int quantity) {
        Map<String, Object> result = marketingToolService.calculateBuyGetFreeGift(activityId, dishId, quantity);
        return R.success(result);
    }

    // ==================== Flash Sale ====================

    @GetMapping("/flash-sale/list")
    @Operation(summary = "Get flash sale list")
    public R<List<FlashSale>> getFlashSales() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<FlashSale> list = marketingToolService.getFlashSales(tenantId);
        return R.success(list);
    }

    @PostMapping("/flash-sale")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Save flash sale")
    public R<String> saveFlashSale(@RequestBody FlashSale flashSale) {
        Long tenantId = BaseContext.getCurrentTenantId();
        flashSale.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateFlashSale(flashSale);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    @PutMapping("/flash-sale")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Update flash sale")
    public R<String> updateFlashSale(@RequestBody FlashSale flashSale) {
        Long tenantId = BaseContext.getCurrentTenantId();
        flashSale.setTenantId(tenantId);
        boolean success = marketingToolService.saveOrUpdateFlashSale(flashSale);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    @DeleteMapping("/flash-sale/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Delete flash sale")
    public R<String> deleteFlashSale(@PathVariable Long id) {
        boolean success = marketingToolService.deleteFlashSale(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    @GetMapping("/flash-sale/active")
    @Operation(summary = "Get active flash sales")
    public R<List<FlashSale>> getActiveFlashSales() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<FlashSale> list = marketingToolService.getActiveFlashSales(tenantId);
        return R.success(list);
    }

    @PostMapping("/flash-sale/calculate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Calculate flash sale price")
    public R<Map<String, Object>> calculateFlashSalePrice(
                        @Parameter(description = "Flash sale ID") @RequestParam Long flashSaleId,
            @Parameter(description = "Quantity") @RequestParam int quantity) {
        Long userId = BaseContext.getCurrentId();
        Map<String, Object> result = marketingToolService.calculateFlashSalePrice(flashSaleId, userId, quantity);
        return R.success(result);
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    @Operation(summary = "Get marketing tool statistics")
    public R<Map<String, Object>> getMarketingToolStatistics() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = marketingToolService.getMarketingToolStatistics(tenantId);
        return R.success(statistics);
    }
}


