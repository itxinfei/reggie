package com.reggie.module.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.marketing.mapper.NewCustomerDiscountMapper;
import com.reggie.module.marketing.mapper.BuyGetFreeMapper;
import com.reggie.module.marketing.mapper.FlashSaleMapper;
import com.reggie.module.marketing.mapper.CampaignUsageRecordMapper;
import com.reggie.module.marketing.model.NewCustomerDiscount;
import com.reggie.module.marketing.model.BuyGetFree;
import com.reggie.module.marketing.model.FlashSale;
import com.reggie.module.marketing.model.CampaignUsageRecord;
import com.reggie.module.marketing.dto.GiftMatch;
import com.reggie.module.marketing.dto.NewCustomerEvaluation;
import com.reggie.module.shopping.model.ShoppingCart;
import com.reggie.module.marketing.service.MarketingToolService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
import com.reggie.common.utils.PageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private CampaignUsageRecordMapper campaignUsageRecordMapper;

    @Autowired
    private UserService userService;

    // ==================== New Customer Discount ====================

    /**
     * 获取 new customer discounts。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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

    /**
     * 保存 or update new customer discount。
     * @param discount 参数 discount
     * @return 返回结果
     */
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

    /**
     * 删除 new customer discount。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNewCustomerDiscount(Long id) {
        return newCustomerDiscountMapper.deleteById(id) > 0;
    }

    /**
     * 计算 new customer discount。
     * @param userId 参数 userId
     * @param orderAmount 参数 orderAmount
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    /**
     * @deprecated 仅后台试算使用；真实下单核价已由
     *             {@link #evaluateNewCustomerDiscount} 取代（补首单判定）。
     */
    @Deprecated
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
                discountAmount = discount.getDiscountValue() != null ? discount.getDiscountValue() : BigDecimal.ZERO;
            } else if (discount.getDiscountType() == NewCustomerDiscount.TYPE_PERCENTAGE) {
                BigDecimal discountValue = discount.getDiscountValue() != null ? discount
                        .getDiscountValue() : BigDecimal.ZERO;
                discountAmount = orderAmount.multiply(discountValue)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                if (discount.getMaxDiscountAmount() != null && discountAmount.compareTo(discount
                        .getMaxDiscountAmount()) > 0) {
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

    /**
     * 获取 buy get free activities。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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

    /**
     * 保存 or update buy get free。
     * @param activity 参数 activity
     * @return 返回结果
     */
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

    /**
     * 删除 buy get free。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteBuyGetFree(Long id) {
        return buyGetFreeMapper.deleteById(id) > 0;
    }

    /**
     * 计算 buy get free gift。
     * @param activityId 参数 activityId
     * @param dishId 参数 dishId
     * @param quantity 参数 quantity
     * @return 返回结果
     */
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

    /**
     * 获取 flash sales。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<FlashSale> getFlashSales(Long tenantId) {
        LambdaQueryWrapper<FlashSale> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(FlashSale::getTenantId, tenantId);
        }
        qw.orderByDesc(FlashSale::getCreateTime);
        return flashSaleMapper.selectList(qw);
    }

    /**
     * 分页查询限时抢购列表。
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   状态筛选（可选）
     * @return 分页结果
     */
    @Override
    public Page<FlashSale> pageFlashSales(int page, int pageSize, Integer status, String name) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文缺失");
        }
        String kw = (name == null) ? null : name.trim();
        Page<FlashSale> p = PageUtils.of(page, pageSize);
        return flashSaleMapper.selectPage(p, new LambdaQueryWrapper<FlashSale>()
                .eq(FlashSale::getTenantId, tenantId)
                .eq(status != null, FlashSale::getStatus, status)
                .like(kw != null && !kw.isEmpty(), FlashSale::getName, kw)
                .orderByDesc(FlashSale::getCreateTime));
    }

    /**
     * 限时抢购统计（各状态数量）。
     * @return 统计数据
     */
    @Override
    public Map<String, Object> getFlashSaleStats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文缺失");
        }
        Map<String, Object> stats = new HashMap<>();
        // 0=草稿, 1=进行中, 2=暂停, 3=已结束
        long draft = flashSaleMapper.selectCount(new LambdaQueryWrapper<FlashSale>()
                .eq(FlashSale::getTenantId, tenantId).eq(FlashSale::getStatus, 0));
        long active = flashSaleMapper.selectCount(new LambdaQueryWrapper<FlashSale>()
                .eq(FlashSale::getTenantId, tenantId).eq(FlashSale::getStatus, 1));
        long paused = flashSaleMapper.selectCount(new LambdaQueryWrapper<FlashSale>()
                .eq(FlashSale::getTenantId, tenantId).eq(FlashSale::getStatus, 2));
        long ended = flashSaleMapper.selectCount(new LambdaQueryWrapper<FlashSale>()
                .eq(FlashSale::getTenantId, tenantId).eq(FlashSale::getStatus, 3));
        stats.put("draft", draft);
        stats.put("active", active);
        stats.put("paused", paused);
        stats.put("ended", ended);
        stats.put("total", draft + active + paused + ended);
        return stats;
    }

    /**
     * 保存 or update flash sale。
     * @param flashSale 参数 flashSale
     * @return 返回结果
     */
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

    /**
     * 删除 flash sale。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFlashSale(Long id) {
        return flashSaleMapper.deleteById(id) > 0;
    }

    /**
     * 获取 active flash sales。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
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
        int remaining = flashSale.getTotalQuantity() - (flashSale.getSoldQuantity() != null ? flashSale
                .getSoldQuantity() : 0);
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

        // 防御性 null 检查：flashPrice/originalPrice 可能在数据库中为 null（历史数据或绕过校验）
        BigDecimal flashPrice = flashSale.getFlashPrice() != null ? flashSale.getFlashPrice() : BigDecimal.ZERO;
        BigDecimal originalPrice = flashSale.getOriginalPrice() != null ? flashSale.getOriginalPrice() : BigDecimal
                .ZERO;
        BigDecimal totalPrice = flashPrice.multiply(new BigDecimal(quantity));
        BigDecimal originalTotal = originalPrice.multiply(new BigDecimal(quantity));
        BigDecimal savings = originalTotal.subtract(totalPrice);

        result.put("eligible", true);
        result.put("quantity", quantity);
        result.put("flashPrice", flashPrice);
        result.put("originalPrice", originalPrice);
        result.put("totalPrice", totalPrice);
        result.put("savings", savings);
        result.put("remaining", remaining);
        result.put("dishName", flashSale.getDishName());

        return result;
    }

    // ==================== Statistics ====================

    /**
     * 获取 marketing tool statistics。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getMarketingToolStatistics(Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // New customer discount count
        LambdaQueryWrapper<NewCustomerDiscount> ncdQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            ncdQw.eq(NewCustomerDiscount::getTenantId, tenantId);
        }
        int newCustomerDiscountCount = newCustomerDiscountMapper.selectCount(ncdQw).intValue();

        // Buy get free count
        LambdaQueryWrapper<BuyGetFree> bgfQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            bgfQw.eq(BuyGetFree::getTenantId, tenantId);
        }
        int buyGetFreeCount = buyGetFreeMapper.selectCount(bgfQw).intValue();

        // Flash sale count
        LambdaQueryWrapper<FlashSale> fsQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            fsQw.eq(FlashSale::getTenantId, tenantId);
        }
        int flashSaleCount = flashSaleMapper.selectCount(fsQw).intValue();

        // Active flash sales
        List<FlashSale> activeFlashSales = getActiveFlashSales(tenantId);

        result.put("newCustomerDiscountCount", newCustomerDiscountCount);
        result.put("buyGetFreeCount", buyGetFreeCount);
        result.put("flashSaleCount", flashSaleCount);
        result.put("activeFlashSaleCount", activeFlashSales.size());

        return result;
    }

    // ==================== 下单核价（外卖主链路，试算/下单同源） ====================

    @Override
    public Map<Long, FlashSale> mapActiveFlashSales(Long tenantId) {
        Map<Long, FlashSale> map = new LinkedHashMap<>();
        List<FlashSale> active = getActiveFlashSales(tenantId);
        if (active == null || active.isEmpty()) {
            return map;
        }
        // 按 dishId 分组
        Map<Long, List<FlashSale>> grouped = new LinkedHashMap<>();
        for (FlashSale fs : active) {
            if (fs.getDishId() == null) {
                continue;
            }
            List<FlashSale> list = grouped.get(fs.getDishId());
            if (list == null) {
                list = new ArrayList<>();
                grouped.put(fs.getDishId(), list);
            }
            list.add(fs);
        }
        // 同菜多活动归约：flashPrice 最低 → 开始最早 → id 最小
        for (Map.Entry<Long, List<FlashSale>> e : grouped.entrySet()) {
            FlashSale best = null;
            for (FlashSale fs : e.getValue()) {
                if (best == null || compareFlashSale(fs, best) < 0) {
                    best = fs;
                }
            }
            map.put(e.getKey(), best);
        }
        return map;
    }

    /**
     * 秒杀归约比较：flashPrice 升序 → startTime 升序 → id 升序。
     */
    private int compareFlashSale(FlashSale a, FlashSale b) {
        BigDecimal pa = a.getFlashPrice() != null ? a.getFlashPrice() : BigDecimal.ZERO;
        BigDecimal pb = b.getFlashPrice() != null ? b.getFlashPrice() : BigDecimal.ZERO;
        int c = pa.compareTo(pb);
        if (c != 0) {
            return c;
        }
        if (a.getStartTime() != null && b.getStartTime() != null) {
            c = a.getStartTime().compareTo(b.getStartTime());
            if (c != 0) {
                return c;
            }
        }
        Long ia = a.getId() != null ? a.getId() : Long.MAX_VALUE;
        Long ib = b.getId() != null ? b.getId() : Long.MAX_VALUE;
        return ia.compareTo(ib);
    }

    @Override
    public int sumFlashSalePurchasedQuantity(Long flashSaleId, Long userId, Long tenantId) {
        return flashSaleMapper.sumPurchasedQuantity(flashSaleId, userId, tenantId);
    }

    @Override
    public List<BuyGetFree> getActiveBuyGetFreeActivities(Long tenantId) {
        LambdaQueryWrapper<BuyGetFree> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(BuyGetFree::getTenantId, tenantId);
        }
        qw.eq(BuyGetFree::getStatus, 1);
        LocalDateTime now = LocalDateTime.now();
        qw.le(BuyGetFree::getStartTime, now);
        qw.ge(BuyGetFree::getEndTime, now);
        return buyGetFreeMapper.selectList(qw);
    }

    @Override
    public List<GiftMatch> matchOrderGifts(List<ShoppingCart> carts, BigDecimal goodsAmount, Long tenantId) {
        List<GiftMatch> matches = new ArrayList<>();
        List<BuyGetFree> activities = getActiveBuyGetFreeActivities(tenantId);
        if (carts == null || carts.isEmpty() || activities.isEmpty()) {
            return matches;
        }
        // 聚合本单数量（同菜多口味行合并）
        Map<Long, Integer> dishQty = new HashMap<>();
        Map<Long, Integer> setmealQty = new HashMap<>();
        int allQty = 0;
        for (ShoppingCart c : carts) {
            int n = c.getNumber() != null ? c.getNumber() : 0;
            if (n <= 0) {
                continue;
            }
            allQty += n;
            if (c.getDishId() != null) {
                addQty(dishQty, c.getDishId(), n);
            } else if (c.getSetmealId() != null) {
                addQty(setmealQty, c.getSetmealId(), n);
            }
        }
        for (BuyGetFree a : activities) {
            int applicableQty;
            if (a.getDishId() != null) {
                applicableQty = qtyOf(dishQty, a.getDishId());
            } else if (a.getSetmealId() != null) {
                applicableQty = qtyOf(setmealQty, a.getSetmealId());
            } else {
                applicableQty = allQty;
            }
            int buyN = a.getBuyQuantity() != null ? a.getBuyQuantity() : 0;
            if (buyN <= 0 || applicableQty < buyN) {
                continue;
            }
            // 商品总额门槛（按整单 goodsAmount，不按触发菜小计）
            if (a.getMinOrderAmount() != null && goodsAmount != null
                    && goodsAmount.compareTo(a.getMinOrderAmount()) < 0) {
                continue;
            }
            int times = applicableQty / buyN;
            if (a.getMaxTimesPerOrder() != null && a.getMaxTimesPerOrder() > 0 && times > a.getMaxTimesPerOrder()) {
                times = a.getMaxTimesPerOrder();
            }
            int getM = a.getGetQuantity() != null ? a.getGetQuantity() : 0;
            int giftQuantity = times * getM;
            if (giftQuantity <= 0) {
                continue;
            }
            GiftMatch m = new GiftMatch();
            m.setActivityId(a.getId());
            m.setActivityName(a.getName());
            m.setGiftDishId(a.getGiftDishId());
            m.setGiftDishName(a.getGiftDishName());
            m.setTimes(times);
            m.setGiftQuantity(giftQuantity);
            matches.add(m);
        }
        return matches;
    }

    private void addQty(Map<Long, Integer> map, Long id, int n) {
        Integer cur = map.get(id);
        map.put(id, (cur == null ? 0 : cur) + n);
    }

    private int qtyOf(Map<Long, Integer> map, Long id) {
        Integer v = map.get(id);
        return v == null ? 0 : v;
    }

    @Override
    public NewCustomerEvaluation evaluateNewCustomerDiscount(Long userId, BigDecimal goodsAmount,
            Long tenantId, boolean firstOrder) {
        NewCustomerEvaluation ev = new NewCustomerEvaluation();
        ev.setEligible(false);
        ev.setDiscountAmount(BigDecimal.ZERO);
        if (!firstOrder || userId == null || goodsAmount == null) {
            return ev;
        }
        User user = userService.getById(userId);
        if (user == null || user.getCreateTime() == null) {
            return ev;
        }
        // 首单且注册在 validDays 内且达门槛（三者且），多配置取优惠最大
        List<NewCustomerDiscount> configs = getNewCustomerDiscounts(tenantId);
        BigDecimal best = BigDecimal.ZERO;
        NewCustomerDiscount bestConfig = null;
        for (NewCustomerDiscount d : configs) {
            if (d.getValidDays() != null) {
                LocalDateTime validUntil = user.getCreateTime().plusDays(d.getValidDays());
                if (LocalDateTime.now().isAfter(validUntil)) {
                    continue;
                }
            }
            if (d.getMinOrderAmount() != null && goodsAmount.compareTo(d.getMinOrderAmount()) < 0) {
                continue;
            }
            BigDecimal amount = calcNcdAmount(d, goodsAmount);
            if (amount.compareTo(best) > 0) {
                best = amount;
                bestConfig = d;
            }
        }
        if (bestConfig == null || best.compareTo(BigDecimal.ZERO) <= 0) {
            return ev;
        }
        ev.setEligible(true);
        ev.setCampaignId(bestConfig.getId());
        ev.setName(bestConfig.getName());
        ev.setDiscountAmount(best.setScale(2, RoundingMode.HALF_UP));
        ev.setCopyText(buildNcdCopy(bestConfig, best));
        return ev;
    }

    private BigDecimal calcNcdAmount(NewCustomerDiscount d, BigDecimal goodsAmount) {
        Integer type = d.getDiscountType();
        if (type != null && type == NewCustomerDiscount.TYPE_PERCENTAGE) {
            BigDecimal value = d.getDiscountValue() != null ? d.getDiscountValue() : BigDecimal.ZERO;
            BigDecimal amount = goodsAmount.multiply(value)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            if (d.getMaxDiscountAmount() != null && amount.compareTo(d.getMaxDiscountAmount()) > 0) {
                amount = d.getMaxDiscountAmount();
            }
            return amount;
        }
        return d.getDiscountValue() != null ? d.getDiscountValue() : BigDecimal.ZERO;
    }

    private String buildNcdCopy(NewCustomerDiscount d, BigDecimal amount) {
        if (d.getDiscountType() != null && d.getDiscountType() == NewCustomerDiscount.TYPE_PERCENTAGE) {
            return "新客 " + d.getDiscountValue().stripTrailingZeros().toPlainString() + " 折立减";
        }
        return "新客立减 ¥" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    @Override
    public void recordFlashSaleUsage(Long flashSaleId, Long orderId, String orderNumber, Long userId,
            Integer quantity, BigDecimal originalAmount, BigDecimal discountAmount,
            BigDecimal actualAmount, Long tenantId) {
        CampaignUsageRecord rec = baseUsageRecord(3, orderId, orderNumber, userId, tenantId);
        rec.setCampaignId(flashSaleId);
        rec.setQuantity(quantity);
        rec.setOrderAmount(originalAmount);
        rec.setDiscountAmount(discountAmount);
        rec.setActualAmount(actualAmount);
        campaignUsageRecordMapper.insert(rec);
    }

    @Override
    public void recordNewCustomerUsage(Long userId, NewCustomerEvaluation hit, Long orderId,
            String orderNumber, BigDecimal goodsAmount, BigDecimal payAmount, Long tenantId) {
        if (hit == null || !hit.isEligible()) {
            return;
        }
        CampaignUsageRecord rec = baseUsageRecord(4, orderId, orderNumber, userId, tenantId);
        rec.setCampaignId(hit.getCampaignId());
        rec.setRuleId(hit.getCampaignId());
        rec.setOrderAmount(goodsAmount);
        rec.setDiscountAmount(hit.getDiscountAmount());
        rec.setActualAmount(payAmount);
        campaignUsageRecordMapper.insert(rec);
    }

    @Override
    public void recordBuyGetFreeUsage(Long userId, GiftMatch match, Long orderId, String orderNumber,
            Long tenantId) {
        if (match == null) {
            return;
        }
        CampaignUsageRecord rec = baseUsageRecord(5, orderId, orderNumber, userId, tenantId);
        rec.setCampaignId(match.getActivityId());
        rec.setQuantity(match.getGiftQuantity());
        rec.setDiscountAmount(BigDecimal.ZERO);
        campaignUsageRecordMapper.insert(rec);
    }

    private CampaignUsageRecord baseUsageRecord(int ruleType, Long orderId, String orderNumber,
            Long userId, Long tenantId) {
        CampaignUsageRecord rec = new CampaignUsageRecord();
        rec.setRuleType(ruleType);
        rec.setOrderId(orderId);
        rec.setOrderNumber(orderNumber);
        rec.setUserId(userId);
        rec.setUseTime(LocalDateTime.now());
        rec.setTenantId(tenantId);
        return rec;
    }
}


