package com.reggie.module.marketing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.marketing.model.NewCustomerDiscount;
import com.reggie.module.marketing.model.BuyGetFree;
import com.reggie.module.marketing.model.FlashSale;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Marketing Tool Service Interface
 * 
 * @author reggie
 * @since 2026-08-11
 */
public interface MarketingToolService extends IService<NewCustomerDiscount> {

    // ==================== New Customer Discount ====================

    /**
     * Get new customer discount list
     *
     * @param tenantId Tenant ID
     * @return Discount list
     */
    List<NewCustomerDiscount> getNewCustomerDiscounts(Long tenantId);

    /**
     * Save or update new customer discount
     *
     * @param discount Discount
     * @return Success or not
     */
    boolean saveOrUpdateNewCustomerDiscount(NewCustomerDiscount discount);

    /**
     * Delete new customer discount
     *
     * @param id Discount ID
     * @return Success or not
     */
    boolean deleteNewCustomerDiscount(Long id);

    /**
     * Calculate new customer discount
     *
     * @param userId      User ID
     * @param orderAmount Order amount
     * @param tenantId    Tenant ID
     * @return Discount amount
     */
    BigDecimal calculateNewCustomerDiscount(Long userId, BigDecimal orderAmount, Long tenantId);

    // ==================== Buy Get Free ====================

    /**
     * Get buy get free activity list
     *
     * @param tenantId Tenant ID
     * @return Activity list
     */
    List<BuyGetFree> getBuyGetFreeActivities(Long tenantId);

    /**
     * Save or update buy get free activity
     *
     * @param activity Activity
     * @return Success or not
     */
    boolean saveOrUpdateBuyGetFree(BuyGetFree activity);

    /**
     * Delete buy get free activity
     *
     * @param id Activity ID
     * @return Success or not
     */
    boolean deleteBuyGetFree(Long id);

    /**
     * Calculate buy get free gift
     *
     * @param activityId Activity ID
     * @param dishId     Dish ID
     * @param quantity   Buy quantity
     * @return Gift info
     */
    Map<String, Object> calculateBuyGetFreeGift(Long activityId, Long dishId, int quantity);

    // ==================== Flash Sale ====================

    /**
     * Get flash sale list
     *
     * @param tenantId Tenant ID
     * @return Flash sale list
     */
    List<FlashSale> getFlashSales(Long tenantId);

    /**
     * Save or update flash sale
     *
     * @param flashSale Flash sale
     * @return Success or not
     */
    boolean saveOrUpdateFlashSale(FlashSale flashSale);

    /**
     * Delete flash sale
     *
     * @param id Flash sale ID
     * @return Success or not
     */
    boolean deleteFlashSale(Long id);

    /**
     * Get active flash sales
     *
     * @param tenantId Tenant ID
     * @return Active flash sales
     */
    List<FlashSale> getActiveFlashSales(Long tenantId);

    /**
     * Calculate flash sale price
     *
     * @param flashSaleId Flash sale ID
     * @param userId      User ID
     * @param quantity    Quantity
     * @return Flash sale info
     */
    Map<String, Object> calculateFlashSalePrice(Long flashSaleId, Long userId, int quantity);

    // ==================== Statistics ====================

    /**
     * Get marketing tool statistics
     *
     * @param tenantId Tenant ID
     * @return Statistics
     */
    Map<String, Object> getMarketingToolStatistics(Long tenantId);
}
