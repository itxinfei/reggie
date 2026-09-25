package com.reggie.module.marketing.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.marketing.model.NewCustomerDiscount;
import com.reggie.module.marketing.model.BuyGetFree;
import com.reggie.module.marketing.model.FlashSale;
import com.reggie.module.marketing.dto.GiftMatch;
import com.reggie.module.marketing.dto.NewCustomerEvaluation;
import com.reggie.module.shopping.model.ShoppingCart;

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
     * 分页查询限时抢购列表
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   状态筛选（可选）
     * @param name     活动名称模糊搜索（可选）
     * @return 分页结果
     */
    Page<FlashSale> pageFlashSales(int page, int pageSize, Integer status, String name);

    /**
     * 限时抢购统计（各状态数量）
     *
     * @return 统计数据
     */
    Map<String, Object> getFlashSaleStats();

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

    // ==================== 下单核价（外卖主链路，试算/下单同源） ====================

    /**
     * 当前生效秒杀按菜品收敛：dishId → 唯一秒杀活动。
     * 同菜多活动取 flashPrice 最低 → 开始时间最早 → id 最小。
     *
     * @param tenantId 租户ID
     * @return dishId → 生效秒杀
     */
    Map<Long, FlashSale> mapActiveFlashSales(Long tenantId);

    /**
     * 统计用户在某秒杀活动已占用限购的购买件数（排除取消/退款订单）。
     *
     * @param flashSaleId 秒杀活动ID
     * @param userId      用户ID
     * @param tenantId    租户ID
     * @return 已购件数
     */
    int sumFlashSalePurchasedQuantity(Long flashSaleId, Long userId, Long tenantId);

    /**
     * 当前生效买赠活动（status=1 且当前时间在窗口内）。
     *
     * @param tenantId 租户ID
     * @return 生效买赠列表
     */
    List<BuyGetFree> getActiveBuyGetFreeActivities(Long tenantId);

    /**
     * 按整单购物车匹配全部生效买赠活动。
     *
     * @param carts       购物车条目
     * @param goodsAmount 商品应付金额（秒杀后）
     * @param tenantId    租户ID
     * @return 买赠命中列表
     */
    List<GiftMatch> matchOrderGifts(List<ShoppingCart> carts, BigDecimal goodsAmount, Long tenantId);

    /**
     * 新客立减核价（首单且注册在有效期内且达门槛）。
     *
     * @param userId      用户ID
     * @param goodsAmount 商品应付金额
     * @param tenantId    租户ID
     * @param firstOrder  是否首单（order 模块据历史成单判定）
     * @return 新客立减结果
     */
    NewCustomerEvaluation evaluateNewCustomerDiscount(Long userId, BigDecimal goodsAmount,
            Long tenantId, boolean firstOrder);

    /**
     * 落库后写秒杀参与记录（rule_type=3）。
     */
    void recordFlashSaleUsage(Long flashSaleId, Long orderId, String orderNumber, Long userId,
            Integer quantity, BigDecimal originalAmount, BigDecimal discountAmount,
            BigDecimal actualAmount, Long tenantId);

    /**
     * 落库后写新客立减核销记录（rule_type=4）。
     */
    void recordNewCustomerUsage(Long userId, NewCustomerEvaluation hit, Long orderId,
            String orderNumber, BigDecimal goodsAmount, BigDecimal payAmount, Long tenantId);

    /**
     * 落库后写买赠核销记录（rule_type=5）。
     */
    void recordBuyGetFreeUsage(Long userId, GiftMatch match, Long orderId, String orderNumber,
            Long tenantId);
}
