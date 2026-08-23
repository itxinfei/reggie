package com.reggie.module.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.marketing.mapper.NewCustomerDiscountMapper;
import com.reggie.module.marketing.mapper.BuyGetFreeMapper;
import com.reggie.module.marketing.mapper.FlashSaleMapper;
import com.reggie.module.marketing.model.NewCustomerDiscount;
import com.reggie.module.marketing.model.BuyGetFree;
import com.reggie.module.marketing.model.FlashSale;
import com.reggie.module.marketing.service.MarketingToolService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Marketing Tool Service Implementation
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class MarketingToolServiceImpl extends ServiceImpl<NewCustomerDiscountMapper, NewCustomerDiscount> 
        implements MarketingToolService {

    @Autowired
    private NewCustomerDiscountMapper newCustomerDiscountMapper;

    @Autowired
    private BuyGetFreeMapper buyGetFreeMapper;

    @Autowired
    private FlashSaleMapper flashSaleMapper;

    @Autowired
    private UserService userService;

    // ==================== New Customer Discount ====================

    @Override
    public List<NewCustomerDiscount> getNewCustomerDiscounts(Long tenantId) {
        LambdaQueryWrapper<NewCustomerDiscount> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(NewCustomerDiscount::getTenantId, tenantId);
        }
        qw.eq(NewCustomerDiscount::getStatus, 1);
        qw.orderByDesc(NewCustomerDiscount::getCreateTime);
        return newCustomerDiscountMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateNewCustomerDiscount(NewCustomerDiscount discount) {
        if (discount.getId() == null) {
            discount.setCreateTime(LocalDateTime.now());
            discount.setUpdateTime(LocalDateTime.now());
            return newCustomerDiscountMapper.insert(discount) > 0;
        } else {
            discount.setUpdateTime(LocalDateTime.now());
            return newCustomerDiscountMapper.updateById(discount) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNewCustomerDiscount(Long id) {
        return newCustomerDiscountMapper.deleteById(id) > 0;
    }

    @Override
    public BigDecimal calculateNewCustomerDiscount(Long userId, BigDecimal orderAmount, Long tenantId) {
        // Check if user is new customer
        User user = userService.getById(userId);
        if (user == null) {
            return BigDecimal.ZERO;
        }

        // Check registration time (within valid days)
        LocalDateTime registrationTime = user.getCreateTime();
        if (registrationTime == null) {
            return BigDecimal.ZERO;
        }

        List<NewCustomerDiscount> discounts = getNewCustomerDiscounts(tenantId);
        BigDecimal maxDiscount = BigDecimal.ZERO;

        for (NewCustomerDiscount discount : discounts) {
            // Check if within valid period
            if (discount.getValidDays() != null) {
                LocalDateTime validUntil = registrationTime.plusDays(discount.getValidDays());
                if (LocalDateTime.now().isAfter(validUntil)) {
                    continue;
                }
            }

            // Check minimum order amount
            if (discount.getMinOrderAmount() != null && orderAmount.compareTo(discount.getMinOrderAmount()) < 0) {
                continue;
            }

            // Calculate discount
            BigDecimal discountAmount = BigDecimal.ZERO;
            if (discount.getDiscountType() == NewCustomerDiscount.TYPE_FIXED) {
                discountAmount = discount.getDiscountValue();
            } else if (discount.getDiscountType() == NewCustomerDiscount.TYPE_PERCENTAGE) {
                discountAmount = orderAmount.multiply(discount.getDiscountValue())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                if (discount.getMaxDiscountAmount() != null && discountAmount.compareTo(discount.getMaxDiscountAmount()) > 0) {
                    discountAmount = discount.getMaxDiscountAmount();
                }
            }

            if (discountAmount.compareTo(maxDiscount) > 0) {
                maxDiscount = discountAmount;
            }
        }

        return maxDiscount;
    }

    // ==================== Buy Get Free ====================

    @Override
    public List<BuyGetFree> getBuyGetFreeActivities(Long tenantId) {
        LambdaQueryWrapper<BuyGetFree> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(BuyGetFree::getTenantId, tenantId);
        }
        qw.eq(BuyGetFree::getStatus, 1);
        qw.orderByDesc(BuyGetFree::getCreateTime);
        return buyGetFreeMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBuyGetFree(BuyGetFree activity) {
        if (activity.getId() == null) {
            activity.setCreateTime(LocalDateTime.now());
            activity.setUpdateTime(LocalDateTime.now());
            activity.setUsageCount(0);
            return buyGetFreeMapper.insert(activity) > 0;
        } else {
            activity.setUpdateTime(LocalDateTime.now());
            return buyGetFreeMapper.updateById(activity) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBuyGetFree(Long id) {
        return buyGetFreeMapper.deleteById(id) > 0;
    }

    @Override
    public Map<String, Object> calculateBuyGetFreeGift(Long activityId, Long dishId, int quantity) {
        Map<String, Object> result = new HashMap<>();

        BuyGetFree activity = buyGetFreeMapper.selectById(activityId);
        if (activity == null || activity.getStatus() != 1) {
            result.put("eligible", false);
            result.put("giftQuantity", 0);
            return result;
        }

        // Check if within time range
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()) || now.isAfter(activity.getEndTime())) {
            result.put("eligible", false);
            result.put("giftQuantity", 0);
            return result;
        }

        // Check if dish is applicable
        if (activity.getDishId() != null && !activity.getDishId().equals(dishId)) {
            result.put("eligible", false);
            result.put("giftQuantity", 0);
            return result;
        }

        // Calculate gift quantity
        int giftQuantity = (quantity / activity.getBuyQuantity()) * activity.getGetQuantity();
        
        // Check max times per order
        if (activity.getMaxTimesPerOrder() != null && activity.getMaxTimesPerOrder() > 0) {
            int maxGift = activity.getMaxTimesPerOrder() * activity.getGetQuantity();
            giftQuantity = Math.min(giftQuantity, maxGift);
        }

        result.put("eligible", giftQuantity > 0);
        result.put("giftQuantity", giftQuantity);
        result.put("giftDishId", activity.getGiftDishId());
        result.put("giftDishName", activity.getGiftDishName());
        result.put("activityName", activity.getName());

        return result;
    }

    // ==================== Flash Sale ====================

    @Override
    public List<FlashSale> getFlashSales(Long tenantId) {
        LambdaQueryWrapper<FlashSale> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(FlashSale::getTenantId, tenantId);
        }
        qw.orderByDesc(FlashSale::getCreateTime);
        return flashSaleMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateFlashSale(FlashSale flashSale) {
        if (flashSale.getId() == null) {
            flashSale.setCreateTime(LocalDateTime.now());
            flashSale.setUpdateTime(LocalDateTime.now());
            flashSale.setSoldQuantity(0);
            return flashSaleMapper.insert(flashSale) > 0;
        } else {
            flashSale.setUpdateTime(LocalDateTime.now());
            return flashSaleMapper.updateById(flashSale) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFlashSale(Long id) {
        return flashSaleMapper.deleteById(id) > 0;
    }

    @Override
    public List<FlashSale> getActiveFlashSales(Long tenantId) {
        LambdaQueryWrapper<FlashSale> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(FlashSale::getTenantId, tenantId);
        }
        qw.eq(FlashSale::getStatus, 1);
        qw.le(FlashSale::getStartTime, LocalDateTime.now());
        qw.ge(FlashSale::getEndTime, LocalDateTime.now());
        qw.orderByAsc(FlashSale::getStartTime);
        return flashSaleMapper.selectList(qw);
    }

    /**
     * 计算秒杀价格（查询方法，不扣库存）。
     *
     * 并发安全说明：
     * 本方法仅为价格查询/计算，内部读取库存快照判断是否可售。
     * 高并发场景下存在"读-检查"竞态条件：多个请求可能同时读到有库存后放行，
     * 但真正库存扣减并未在此执行，因此此处不会产生实际超卖。
     *
     * 真正的原子扣减必须在下单时执行，请调用 FlashSaleMapper.deductStock() 方法：
     * 该方法利用 SQL WHERE 条件 (total_quantity - sold_quantity) >= qty 实现
     * 数据库行级锁的 CAS 乐观扣减，保证并发安全、绝不超卖。
     * 扣减失败（返回 0 行受影响）时应回滚订单并提示"库存不足"。
     *
     * @param flashSaleId 秒杀活动ID
     * @param userId      用户ID
     * @param quantity    购买数量
     * @return 价格计算结果
     */
    @Override
    public Map<String, Object> calculateFlashSalePrice(Long flashSaleId, Long userId, int quantity) {
        Map<String, Object> result = new HashMap<>();

        FlashSale flashSale = flashSaleMapper.selectById(flashSaleId);
        if (flashSale == null || flashSale.getStatus() != 1) {
            result.put("eligible", false);
            result.put("message", "Flash sale not available");
            return result;
        }

        // Check if within time range
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(flashSale.getStartTime()) || now.isAfter(flashSale.getEndTime())) {
            result.put("eligible", false);
            result.put("message", "Flash sale not started or ended");
            return result;
        }

        // Check stock
        int remaining = flashSale.getTotalQuantity() - (flashSale.getSoldQuantity() != null ? flashSale.getSoldQuantity() : 0);
        if (remaining <= 0) {
            result.put("eligible", false);
            result.put("message", "Flash sale sold out");
            return result;
        }

        // Check max per user
        if (flashSale.getMaxPerUser() != null && quantity > flashSale.getMaxPerUser()) {
            quantity = flashSale.getMaxPerUser();
        }

        // Check remaining stock
        quantity = Math.min(quantity, remaining);

        BigDecimal totalPrice = flashSale.getFlashPrice().multiply(new BigDecimal(quantity));
        BigDecimal originalTotal = flashSale.getOriginalPrice().multiply(new BigDecimal(quantity));
        BigDecimal savings = originalTotal.subtract(totalPrice);

        result.put("eligible", true);
        result.put("quantity", quantity);
        result.put("flashPrice", flashSale.getFlashPrice());
        result.put("originalPrice", flashSale.getOriginalPrice());
        result.put("totalPrice", totalPrice);
        result.put("savings", savings);
        result.put("remaining", remaining);
        result.put("dishName", flashSale.getDishName());

        return result;
    }

    // ==================== Statistics ====================

    @Override
    public Map<String, Object> getMarketingToolStatistics(Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // New customer discount count
        LambdaQueryWrapper<NewCustomerDiscount> ncdQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            ncdQw.eq(NewCustomerDiscount::getTenantId, tenantId);
        }
        int newCustomerDiscountCount = (int) newCustomerDiscountMapper.selectCount(ncdQw);

        // Buy get free count
        LambdaQueryWrapper<BuyGetFree> bgfQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            bgfQw.eq(BuyGetFree::getTenantId, tenantId);
        }
        int buyGetFreeCount = (int) buyGetFreeMapper.selectCount(bgfQw);

        // Flash sale count
        LambdaQueryWrapper<FlashSale> fsQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            fsQw.eq(FlashSale::getTenantId, tenantId);
        }
        int flashSaleCount = (int) flashSaleMapper.selectCount(fsQw);

        // Active flash sales
        List<FlashSale> activeFlashSales = getActiveFlashSales(tenantId);

        result.put("newCustomerDiscountCount", newCustomerDiscountCount);
        result.put("buyGetFreeCount", buyGetFreeCount);
        result.put("flashSaleCount", flashSaleCount);
        result.put("activeFlashSaleCount", activeFlashSales.size());

        return result;
    }
}


