package com.reggie.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.dto.OrderDto;
import com.reggie.enums.DishStatus;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.cashier.mapper.CashierRecordMapper;
import com.reggie.module.cashier.model.CashierRecord;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.statusflow.OrderStatusFlowService;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.shopping.model.ShoppingCart;
import com.reggie.module.user.model.User;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.address.service.AddressBookService;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.delivery.service.DeliveryEnhancedService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.setmeal.service.SetmealDishService;
import com.reggie.module.setmeal.service.SetmealService;
import com.reggie.module.shopping.service.ShoppingCartService;
import com.reggie.module.store.model.StoreInfo;
import com.reggie.module.store.service.BusinessHoursService;
import com.reggie.module.store.service.StoreService;
import com.reggie.module.user.service.UserService;
import com.reggie.module.printer.service.PrinterService;
import com.reggie.module.inventory.service.MaterialStockService;
import com.reggie.module.dining.service.DiningTableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 订单服务实现类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    /** 购物车服务 */
    @Autowired
    private ShoppingCartService shoppingCartService;

    /** 用户服务 */
    @Autowired
    private UserService userService;

    /** 地址簿服务 */
    @Autowired
    private AddressBookService addressBookService;

    /** 订单明细服务 */
    @Autowired
    private OrderDetailService orderDetailService;

    /** 菜品服务 */
    @Autowired
    private DishService dishService;

    /** 套餐菜品关联服务 */
    @Autowired
    private SetmealDishService setmealDishService;

    /** 套餐服务 */
    @Autowired
    private SetmealService setmealService;

    /** 状态流转服务（接单/拒单/完成/取消等） */
    @Autowired
    private OrderStatusFlowService statusFlowService;

    /**
     * 打印服务（可选注入，无打印机配置时降级跳过）
     */
    @Autowired(required = false)
    private PrinterService printerService;

    /**
     * 堂食桌台服务
     */
    @Autowired(required = false)
    private DiningTableService diningTableService;

    /**
     * 原料库存联动服务（可选注入，无 BOM 配方时降级跳过）
     */
    @Autowired(required = false)
    private MaterialStockService materialStockService;

    /**
     * 收银记录 Mapper（用于查询订单是否已有收银记录，判断待收银状态）
     */
    @Autowired(required = false)
    private CashierRecordMapper cashierRecordMapper;

    /**
     * Redis 模板（可选，用于下单幂等性 SETNX 抢占，防止 check-then-act 竞态）
     */
    @Autowired(required = false)
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    /** 配送增强服务（可选注入，未配置配送规则时降级跳过配送校验） */
    @Autowired(required = false)
    private DeliveryEnhancedService deliveryEnhancedService;

    /** 营业时间与暂停接单服务（可选注入，无配置时降级跳过校验） */
    @Autowired(required = false)
    private BusinessHoursService businessHoursService;

    /** 门店服务（可选注入，用于读取门店坐标/起送价/配送配置） */
    @Autowired(required = false)
    private StoreService storeService;

    /** 用户优惠券服务（下单应用折扣 + 立即核销） */
    @Autowired(required = false)
    private com.reggie.module.member.service.CouponUserService couponUserService;

    /** 优惠券模板服务（计算折扣时直接查模板，不依赖 availableCoupons） */
    @Autowired(required = false)
    private com.reggie.module.member.service.CouponTemplateService couponTemplateService;

    /** 会员服务（可用券以会员维度查询） */
    @Autowired(required = false)
    private com.reggie.module.member.service.MemberService memberService;

    /** 下单幂等锁过期时间（分钟） */
    private static final long IDEMPOTENCY_TTL_MINUTES = 30;

    /**
     * 用户下单（从购物车生成订单）
     * 流程：查询购物车 → 验证用户和地址 → 生成订单和明细 → 扣减库存 → 清空购物车 → 触发打印
     *
     * @param orders 订单信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Orders orders) {
        //获得当前用户id
        Long userId = BaseContext.getCurrentId();

        // 营业时间 + 暂停接单校验（可选服务，降级兼容）
        Long tenantId = orders.getTenantId() != null ? orders.getTenantId() : BaseContext.getCurrentTenantId();
        if (businessHoursService != null) {
            businessHoursService.checkBusinessHours(tenantId);
        }

        //查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(wrapper);

        if (shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new CustomException("购物车为空，不能下单");
        }

        //查询用户与地址数据（等价抽取）
        Map<String, Object> ctx = loadAndValidateSubmitContext(userId, orders);
        User user = (User) ctx.get("user");
        AddressBook addressBook = (AddressBook) ctx.get("addressBook");

        // 配送校验：起送价 + 配送范围 + 配送费（外卖单专属，堂食/预订不进入此分支）
        // 门店坐标 / 起送价 / 配送费配置均来自 StoreInfo，地址经纬度来自 AddressBook（GeoUtils 自动回填）
        // 任一依赖缺失时降级跳过（不阻断下单），保证开发环境无配置也能下单
        Long currentTenantId = BaseContext.getCurrentTenantId();
        StoreInfo storeInfo = (storeService != null) ? storeService.findByTenantId(currentTenantId) : null;
        boolean deliveryCheckEnabled = deliveryEnhancedService != null && storeInfo != null
                && storeInfo.getIsDeliveryEnabled() != null && storeInfo.getIsDeliveryEnabled() == 1;

        long orderId = IdWorker.getId();//订单号

        // 服务端重新核价并构建订单明细（等价抽取，幽灵菜品防御）
        Map<String, Object> detailsHolder = buildOrderDetailsAndComputeAmount(shoppingCarts, orderId,
                currentTenantId);
        @SuppressWarnings("unchecked")
        List<OrderDetail> orderDetails = (List<OrderDetail>) detailsHolder.get("orderDetails");
        BigDecimal totalAmount = (BigDecimal) detailsHolder.get("totalAmount");

        // 起送价精确校验 + 配送费精确计算（等价抽取）
        BigDecimal deliveryFee = computeDeliveryFee(deliveryCheckEnabled, storeInfo, addressBook, totalAmount,
                currentTenantId);
        // 配送费计入订单总额
        BigDecimal finalAmount = totalAmount.add(deliveryFee);

        // 优惠券折扣（等价抽取）
        Map<String, Object> couponHolder = resolveCouponDiscount(orders, userId, orderId, totalAmount);
        BigDecimal couponDiscount = (BigDecimal) couponHolder.get("couponDiscount");
        boolean couponOk = (Boolean) couponHolder.get("couponOk");
        Long usedCouponId = (Long) couponHolder.get("usedCouponId");
        finalAmount = finalAmount.subtract(couponDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }
        orders.setUsedCouponId(couponOk ? usedCouponId : null);

        // 设置订单字段（等价抽取）
        applySubmitOrderFields(orders, orderId, userId, user, addressBook, finalAmount, deliveryFee);
        //向订单表插入数据，一条数据
        // 修复 check-then-act 竞态：用 Redis SETNX 原子抢占幂等令牌，防止并发重复下单（等价抽取）
        String idempotencyKey = orders.getIdempotencyKey();
        String lockKey = "order:idem:" + idempotencyKey;
        int lockState = prepareIdempotencyLock(orders, idempotencyKey, lockKey);
        if (lockState < 0) {
            // 并发请求或已下单：已回填既有订单，直接返回
            return;
        }
        boolean lockAcquired = lockState == 1;

        // 落库订单与明细、扣库存、清空购物车（失败释放幂等锁）（等价抽取）
        saveOrderWithLockRelease(orders, orderDetails, shoppingCarts, wrapper, lockAcquired, lockKey);

        // 自动触发打印（等价抽取）
        printOrderQuietly(orderId);
    }

    /**
     * 加载并校验下单上下文（用户 + 地址簿）（等价抽取，降低方法长度）。
     */
    private Map<String, Object> loadAndValidateSubmitContext(Long userId, Orders orders) {
        User user = userService.getById(userId);
        if (user == null) {
            throw new CustomException("用户信息不存在，不能下单");
        }
        Long addressBookId = orders.getAddressBookId();
        if (addressBookId == null) {
            throw new CustomException("请选择收货地址");
        }
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("用户地址信息有误，不能下单");
        }
        Map<String, Object> holder = new HashMap<>();
        holder.put("user", user);
        holder.put("addressBook", addressBook);
        return holder;
    }

    /**
     * 服务端重新核价并构建订单明细（幽灵菜品防御）（等价抽取，降低方法长度）。
     *
     * @return {orderDetails, totalAmount}
     */
    private Map<String, Object> buildOrderDetailsAndComputeAmount(List<ShoppingCart> shoppingCarts, long orderId,
            Long currentTenantId) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        // 幽灵菜品防御：下单时服务端重新核价——菜品/套餐存在性、租户归属、启售状态、
        // 价格一律以数据库为准，禁止信任购物车中可能被注入的客户端金额（与 submitEatInOrder 核价逻辑对齐）
        List<Long> dishIds = new ArrayList<>();
        List<Long> setmealIds = new ArrayList<>();
        for (ShoppingCart item : shoppingCarts) {
            if (item.getDishId() != null) {
                dishIds.add(item.getDishId());
            } else if (item.getSetmealId() != null) {
                setmealIds.add(item.getSetmealId());
            } else {
                throw new CustomException("购物车明细缺少菜品或套餐ID");
            }
        }
        // N+1 规避：批量预加载菜品与套餐
        Map<Long, Dish> dishMap = new HashMap<>();
        if (!dishIds.isEmpty()) {
            for (Dish d : dishService.listByIds(dishIds)) {
                dishMap.put(d.getId(), d);
            }
        }
        Map<Long, Setmeal> setmealMap = new HashMap<>();
        if (!setmealIds.isEmpty()) {
            for (Setmeal s : setmealService.listByIds(setmealIds)) {
                setmealMap.put(s.getId(), s);
            }
        }

        List<OrderDetail> orderDetails = new ArrayList<>();
        for (ShoppingCart item : shoppingCarts) {
            BigDecimal unitPrice;
            if (item.getDishId() != null) {
                Dish dish = dishMap.get(item.getDishId());
                if (dish == null) {
                    throw new CustomException("菜品不存在，ID：" + item.getDishId());
                }
                if (currentTenantId != null && !currentTenantId.equals(dish.getTenantId())) {
                    throw new CustomException("无权使用其他门店的菜品");
                }
                if (dish.getStatus() == null || dish.getStatus() != DishStatus.ENABLED.getValue()) {
                    throw new CustomException("菜品「" + dish.getName() + "」已停售，无法下单");
                }
                unitPrice = dish.getPrice() != null ? dish.getPrice() : BigDecimal.ZERO;
            } else {
                Setmeal setmeal = setmealMap.get(item.getSetmealId());
                if (setmeal == null) {
                    throw new CustomException("套餐不存在，ID：" + item.getSetmealId());
                }
                if (currentTenantId != null && !currentTenantId.equals(setmeal.getTenantId())) {
                    throw new CustomException("无权使用其他门店的套餐");
                }
                if (setmeal.getStatus() == null || setmeal.getStatus() != DishStatus.ENABLED.getValue()) {
                    throw new CustomException("套餐「" + setmeal.getName() + "」已停用，无法下单");
                }
                unitPrice = setmeal.getPrice() != null ? setmeal.getPrice() : BigDecimal.ZERO;
            }
            // 行小计 = 服务端单价 × 数量（明细金额语义与 submitEatInOrder 对齐）
            Integer num = item.getNumber() != null ? item.getNumber() : 0;
            BigDecimal lineTotal = unitPrice.multiply(new BigDecimal(num));
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(lineTotal);
            orderDetails.add(orderDetail);
            totalAmount = totalAmount.add(lineTotal);
        }

        Map<String, Object> holder = new HashMap<>();
        holder.put("orderDetails", orderDetails);
        holder.put("totalAmount", totalAmount);
        return holder;
    }

    /**
     * 校验起送价并计算配送费（等价抽取，降低方法长度）。
     */
    private BigDecimal computeDeliveryFee(boolean deliveryCheckEnabled, StoreInfo storeInfo, AddressBook addressBook,
            BigDecimal totalAmount, Long currentTenantId) {
        BigDecimal deliveryFee = BigDecimal.ZERO;
        if (!deliveryCheckEnabled) {
            return deliveryFee;
        }
        // 起送价精确校验（服务端核价完成后再计算，免运门槛基于真实菜品金额）
        BigDecimal minAmount = storeInfo.getMinDeliveryAmount();
        if (minAmount != null && minAmount.compareTo(BigDecimal.ZERO) > 0
                && totalAmount.compareTo(minAmount) < 0) {
            throw new CustomException("订单金额未达到起送价 " + minAmount + " 元，无法下单");
        }
        // 配送范围 + 配送费（地址经纬度存在时才校验，避免无地图 Key 环境阻断下单）
        BigDecimal addrLon = addressBook.getLongitude();
        BigDecimal addrLat = addressBook.getLatitude();
        BigDecimal storeLon = storeInfo.getLongitude();
        BigDecimal storeLat = storeInfo.getLatitude();
        if (addrLon != null && addrLat != null && storeLon != null && storeLat != null) {
            BigDecimal distance = deliveryEnhancedService.calculateDistance(storeLon, storeLat, addrLon, addrLat);
            // 修复免运门槛失效：传真实核价后 totalAmount，满额自动免配送费
            java.util.Map<String, Object> feeResult = deliveryEnhancedService.calculateFee(
                    addrLon, addrLat, distance, totalAmount, currentTenantId);
            Boolean inRange = (Boolean) feeResult.get("inRange");
            if (inRange != null && !inRange) {
                throw new CustomException("收货地址不在配送范围内");
            }
            Object feeObj = feeResult.get("fee");
            if (feeObj instanceof BigDecimal) {
                deliveryFee = ((BigDecimal) feeObj).setScale(2, java.math.RoundingMode.HALF_UP);
            }
        }
        return deliveryFee;
    }

    /**
     * 校验并核销优惠券，返回 {couponDiscount, couponOk, usedCouponId}（等价抽取，降低方法长度）。
     */
    private Map<String, Object> resolveCouponDiscount(Orders orders, Long userId, long orderId,
            BigDecimal totalAmount) {
        BigDecimal couponDiscount = BigDecimal.ZERO;
        Long usedCouponId = orders.getUsedCouponId();
        boolean couponOk = false;
        if (usedCouponId != null && couponUserService != null) {
            try {
                // 归属 + 未使用 + 未过期校验，并在同一事务内 CAS 核销（并发重复下单时第二个请求核销失败）
                // coupon_user.member_id 为会员ID，先经 user→member 映射（与选券列表 availableCoupons 语义一致）
                com.reggie.module.member.model.Member member = memberService != null
                        ? memberService.getByUserId(userId) : null;
                Long memberId = member != null ? member.getId() : userId;
                couponOk = couponUserService.useCoupon(memberId, usedCouponId, orderId);
                if (couponOk) {
                    // 按订单菜品金额计算实际折扣（规则与选券列表 availableCoupons 一致）
                    couponDiscount = computeCouponDiscount(memberId, usedCouponId, totalAmount);
                    if (couponDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                        // 门槛不满足/折扣为0 → 撤销核销，视为未用券
                        couponUserService.restoreCoupon(usedCouponId, orderId);
                        couponOk = false;
                    }
                }
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.warn("[优惠券] 订单{}核销失败，按未用券处理: {}", orderId, e.getMessage());
            }
            if (!couponOk) {
                log.warn("[优惠券] 订单{}所选优惠券不可用（不属于该用户/已用/已过期/未达门槛），按未用券处理", orderId);
                usedCouponId = null;
            }
        }
        Map<String, Object> holder = new HashMap<>();
        holder.put("couponDiscount", couponDiscount);
        holder.put("couponOk", couponOk);
        holder.put("usedCouponId", usedCouponId);
        return holder;
    }

    /**
     * 设置提交订单的字段（等价抽取，降低方法长度）。
     */
    private void applySubmitOrderFields(Orders orders, long orderId, Long userId, User user, AddressBook addressBook,
            BigDecimal finalAmount, BigDecimal deliveryFee) {
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        // 修复：下单状态按支付方式分流，避免支付主链路断裂。
        // - 货到付款(COD, payMethod=6)：无需线上支付，直接 STATUS_ORDERED + 立即接单处理
        // - 微信(2)/支付宝(3) 或未传 payMethod：需线上支付，STATUS_PENDING_PAY，
        //   支付成功后由 PaymentOrderServiceImpl.handlePaymentSuccess() 流转为 ORDERED 并回填 checkoutTime
        // 此前无条件设 STATUS_ORDERED 导致 PaymentController.pay() 拒绝支付（要求 PENDING_PAY）
        if (orders.getPayMethod() != null && orders.getPayMethod() == 6) {
            orders.setCheckoutTime(LocalDateTime.now());
            orders.setStatus(Orders.STATUS_ORDERED);
        } else {
            orders.setCheckoutTime(null);
            orders.setStatus(Orders.STATUS_PENDING_PAY);
        }
        orders.setDeliveryFee(deliveryFee.compareTo(BigDecimal.ZERO) > 0 ? deliveryFee : null);
        orders.setAmount(finalAmount.setScale(2, java.math.RoundingMode.HALF_UP));//总金额（菜品+配送费-优惠券）
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        // 幂等性保护：如果请求未提供幂等令牌，自动生成一个
        if (orders.getIdempotencyKey() == null || orders.getIdempotencyKey().trim().isEmpty()) {
            orders.setIdempotencyKey(generateIdempotencyKey(userId));
        }
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
    }

    /**
     * 落库订单与明细、扣库存、清空购物车，失败释放幂等锁（等价抽取，降低方法长度）。
     */
    private void saveOrderWithLockRelease(Orders orders, List<OrderDetail> orderDetails,
            List<ShoppingCart> shoppingCarts, LambdaQueryWrapper<ShoppingCart> wrapper, boolean lockAcquired,
            String lockKey) {
        try {
            this.save(orders);
            //向订单明细表插入数据，多条数据
            orderDetailService.saveBatch(orderDetails);
            this.deductStockForOrder(shoppingCarts);
            //清空购物车数据
            shoppingCartService.remove(wrapper);
        } catch (RuntimeException e) {
            // 落库失败时释放锁，允许用户重试（锁成功保留则作为去重记录由 TTL 过期）
            if (lockAcquired && redisTemplate != null) {
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception ignored) {
                    // 释放锁失败不影响异常抛出
                }
            }
            throw e;
        }
    }

    /**
     * 原子抢占下单幂等令牌（等价抽取，降低方法长度）。
     *
     * @return -1 表示已存在既有订单并已回填（调用方应直接 return）；0 表示未加锁（无 Redis）；
     *         1 表示已获得锁
     */
    private int prepareIdempotencyLock(Orders orders, String idempotencyKey, String lockKey) {
        if (redisTemplate == null || idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return 0;
        }
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", IDEMPOTENCY_TTL_MINUTES, java.util
                .concurrent.TimeUnit.MINUTES);
        boolean lockAcquired = ok != null && ok;
        if (lockAcquired) {
            return 1;
        }
        // 并发请求或已下单：查询既有订单并回填，避免重复落库
        Orders existing = checkIdempotency(idempotencyKey);
        if (existing != null) {
            orders.setId(existing.getId());
            orders.setNumber(existing.getNumber());
            orders.setAmount(existing.getAmount());
            orders.setStatus(existing.getStatus());
            orders.setUsedCouponId(existing.getUsedCouponId());
            return -1;
        }
        // 锁存在但订单未落库（并发处理中），拒绝重复提交
        throw new CustomException("订单正在处理中，请勿重复提交");
    }

    /**
     * 异步自动打印订单小票（失败不影响下单）（等价抽取）。
     */
    private void printOrderQuietly(long orderId) {
        if (printerService == null) {
            return;
        }
        final long finalOrderId = orderId;
        try {
            printerService.printOrder(finalOrderId, "BILL");
            printerService.printOrder(finalOrderId, "KITCHEN");
        } catch (Exception e) {
            // 打印失败不影响下单结果
            log.warn("[打印] 自动打印触发失败，订单ID={}, 原因={}", finalOrderId, e.getMessage(), e);
        }
    }

    // ==================== 优惠券折扣 ====================

    /**
     * 计算优惠券针对订单菜品金额的实际可抵扣金额（规则与选券列表 availableCoupons 一致）：
     * <ul>
     *   <li>折扣券（DISCOUNT）：orderAmount × (1 - discountRate)</li>
     *   <li>满减券/其他：min(discountAmount, orderAmount)</li>
     * </ul>
     * 先核销再算折扣，若门槛不满足返回 0 由调用方撤销核销。
     * <p>注意：不能调用 availableCoupons 查询——该方法只查 status=unused 的券，
     * 而 useCoupon 已将券标记为 used，导致查不到、折扣恒为 0。
     * 改为直接按 couponId 查 CouponUser + CouponTemplate 计算。</p>
     */
    private BigDecimal computeCouponDiscount(Long memberId, Long couponId, BigDecimal orderAmount) {
        try {
            com.reggie.module.member.model.CouponUser couponUser = couponUserService.getById(couponId);
            if (couponUser == null) {
                return BigDecimal.ZERO;
            }
            // 校验归属与租户
            if (!Objects.equals(couponUser.getMemberId(), memberId)) {
                return BigDecimal.ZERO;
            }
            Long currentTenantId = BaseContext.getCurrentTenantId();
            if (currentTenantId != null && !Objects.equals(couponUser.getTenantId(), currentTenantId)) {
                return BigDecimal.ZERO;
            }
            // 校验过期
            if (couponUser.getExpireTime() != null && couponUser.getExpireTime().isBefore(LocalDateTime.now())) {
                return BigDecimal.ZERO;
            }
            // 查模板计算折扣
            com.reggie.module.member.model.CouponTemplate template =
                    couponTemplateService.getById(couponUser.getTemplateId());
            if (template == null) {
                return BigDecimal.ZERO;
            }
            // 门槛校验：订单金额需达到满额条件
            BigDecimal conditionAmount = template.getConditionAmount() == null
                    ? BigDecimal.ZERO : template.getConditionAmount();
            if (orderAmount.compareTo(conditionAmount) < 0) {
                return BigDecimal.ZERO;
            }
            // 折扣券
            if ("DISCOUNT".equals(template.getType()) && template.getDiscountRate() != null) {
                BigDecimal rate = template.getDiscountRate();
                if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                    return BigDecimal.ZERO;
                }
                return orderAmount.multiply(BigDecimal.ONE.subtract(rate))
                        .setScale(2, java.math.RoundingMode.HALF_UP);
            }
            // 满减券/代金券
            BigDecimal discountAmount = template.getDiscountAmount() == null
                    ? BigDecimal.ZERO : template.getDiscountAmount();
            return discountAmount.compareTo(orderAmount) > 0 ? orderAmount : discountAmount;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.warn("[优惠券] 计算折扣失败 couponId={}, 按0处理: {}", couponId, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    // ==================== 堂食扫码下单 ====================

    /**
     * 堂食扫码下单（不经过购物车，直接从前端传入菜品列表）
     *
     * @param orders       订单基本信息（source/tableId/tableName/contact 等）
     * @param orderDetails 订单明细列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitEatInOrder(Orders orders, List<OrderDetail> orderDetails) {
        if (orderDetails == null || orderDetails.isEmpty()) {
            throw new CustomException("请至少选择一道菜品");
        }

        // 营业时间 + 暂停接单校验（可选服务，降级兼容）
        Long eatInTenantId = orders.getTenantId() != null ? orders.getTenantId() : BaseContext.getCurrentTenantId();
        if (businessHoursService != null) {
            businessHoursService.checkBusinessHours(eatInTenantId);
        }

        // 设置堂食来源
        orders.setSource(com.reggie.enums.OrderSource.EAT_IN.getValue());
        Long tableId = orders.getTableId();
        if (tableId == null) {
            throw new CustomException("桌台信息缺失，请重新扫码");
        }

        // 修复堂食并发重复下单：用 Redis SETNX 原子抢占幂等令牌，防止同一桌台并发扫码重复下单
        // 修复 P0-3：幂等键去掉 UUID（原实现拼入随机值导致每次 key 不同，Redis SETNX 防重完全失效）
        // 修复 P2-8：幂等键追加菜品明细签名。原实现 key 恒为 {userId}_{tableId}，30 分钟 TTL 内
        // 同一用户+桌台的「加菜」（菜品不同）会被误判为重复提交而静默丢弃，导致加菜功能失效。
        // 现改为 {userId}_{tableId}_{明细签名}：完全相同的提交（双击/重试）→ 返回已有订单；
        // 菜品不同 → 视为加菜，正常创建新订单。
        Long eatInUserId = BaseContext.getCurrentId();
        String eatInIdemKey = (eatInUserId != null ? eatInUserId.toString() : "unknown")
                + "_" + (tableId != null ? tableId.toString() : "unknown")
                + "_" + buildEatInDetailSignature(orderDetails);
        orders.setIdempotencyKey(eatInIdemKey);
        String lockKey = "order:eatin:idem:" + eatInIdemKey;
        boolean eatInLockAcquired = false;
        if (redisTemplate != null) {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, eatInIdemKey, IDEMPOTENCY_TTL_MINUTES, java
                    .util.concurrent.TimeUnit.MINUTES);
            eatInLockAcquired = ok != null && ok;
            if (!eatInLockAcquired) {
                Orders existing = checkIdempotency(eatInIdemKey);
                if (existing != null) {
                    orders.setId(existing.getId());
                    orders.setNumber(existing.getNumber());
                    orders.setAmount(existing.getAmount());
                    orders.setStatus(existing.getStatus());
                    return;
                }
                throw new CustomException("堂食订单正在处理中，请勿重复提交");
            }
        }

        // 查询桌台信息（用于填充桌台名称）
        if (diningTableService != null) {
            com.reggie.module.dining.model.DiningTable table = diningTableService.getById(tableId);
            if (table != null) {
                Long currentTenantId = BaseContext.getCurrentTenantId();
                if (currentTenantId != null && !currentTenantId.equals(table.getTenantId())) {
                    throw new CustomException("无权使用其他门店的桌台");
                }
                orders.setTableName(table.getName());
            }
        }

        Long userId = BaseContext.getCurrentId();
        long orderId = IdWorker.getId();

        // 计算总金额（价格从菜品/套餐表服务端查询，防止客户端篡改）（等价抽取）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        BigDecimal totalAmount = computeEatInTotalAmount(orderDetails, orderId, currentTenantId);

        // 设置订单字段（等价抽取）
        applyEatInOrderFields(orders, orderId, totalAmount, userId);

        // 落库 + 扣库存（失败释放幂等锁）（等价抽取）
        saveEatInOrderWithLockRelease(orders, orderDetails, eatInLockAcquired, lockKey);

        // 更新桌台状态 + 自动打印（等价抽取）
        afterEatInOrderSaved(tableId, orderId);
    }

    /**
     * 计算堂食订单总金额并按服务端价格回填明细（等价抽取，降低方法长度）。
     */
    private BigDecimal computeEatInTotalAmount(List<OrderDetail> orderDetails, long orderId, Long currentTenantId) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        // N+1 修复：批量预加载菜品和套餐，避免循环内 getById
        java.util.List<Long> dishIds = new java.util.ArrayList<>();
        java.util.List<Long> setmealIds = new java.util.ArrayList<>();
        for (OrderDetail detail : orderDetails) {
            if (detail.getDishId() != null) dishIds.add(detail.getDishId());
            else if (detail.getSetmealId() != null) setmealIds.add(detail.getSetmealId());
        }
        java.util.Map<Long, Dish> dishMap = new java.util.HashMap<>();
        if (!dishIds.isEmpty()) {
            for (Dish d : dishService.listByIds(dishIds)) { dishMap.put(d.getId(), d); }
        }
        java.util.Map<Long, Setmeal> setmealMap = new java.util.HashMap<>();
        if (!setmealIds.isEmpty()) {
            for (Setmeal s : setmealService.listByIds(setmealIds)) { setmealMap.put(s.getId(), s); }
        }

        for (OrderDetail detail : orderDetails) {
            detail.setOrderId(orderId);
            BigDecimal unitPrice;
            String dishName;
            if (detail.getDishId() != null) {
                Dish dish = dishMap.get(detail.getDishId());
                if (dish == null) {
                    throw new CustomException("菜品不存在，ID：" + detail.getDishId());
                }
                if (currentTenantId != null && !currentTenantId.equals(dish.getTenantId())) {
                    throw new CustomException("无权使用其他门店的菜品");
                }
                unitPrice = dish.getPrice() != null ? dish.getPrice() : BigDecimal.ZERO;
                dishName = dish.getName();
            } else if (detail.getSetmealId() != null) {
                Setmeal setmeal = setmealMap.get(detail.getSetmealId());
                if (setmeal == null) {
                    throw new CustomException("套餐不存在，ID：" + detail.getSetmealId());
                }
                if (currentTenantId != null && !currentTenantId.equals(setmeal.getTenantId())) {
                    throw new CustomException("无权使用其他门店的套餐");
                }
                unitPrice = setmeal.getPrice() != null ? setmeal.getPrice() : BigDecimal.ZERO;
                dishName = setmeal.getName();
            } else {
                throw new CustomException("订单明细缺少菜品或套餐ID");
            }
            Integer num = detail.getNumber() != null ? detail.getNumber() : 0;
            BigDecimal lineTotal = unitPrice.multiply(new BigDecimal(num));
            detail.setAmount(lineTotal);
            detail.setName(dishName);
            totalAmount = totalAmount.add(lineTotal);
        }
        return totalAmount;
    }

    /**
     * 设置堂食订单字段（等价抽取）。
     */
    private void applyEatInOrderFields(Orders orders, long orderId, BigDecimal totalAmount, Long userId) {
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        // 修复：堂食订单按支付方式分流
        // - payMethod=6(货到付款)：已"付过款"，直接 STATUS_ORDERED
        // - payMethod=1(现金)/2(微信)/3(支付宝)/4(银行卡)/5(会员储值) 或未传：需线上支付或收银收款，STATUS_PENDING_PAY
        if (orders.getPayMethod() != null && orders.getPayMethod() == 6) {
            orders.setCheckoutTime(LocalDateTime.now());
            orders.setStatus(Orders.STATUS_ORDERED);
        } else {
            orders.setCheckoutTime(null);
            orders.setStatus(Orders.STATUS_PENDING_PAY);
        }
        orders.setAmount(totalAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(orders.getUserName() != null ? orders.getUserName() : "堂食顾客");
        orders.setConsignee(orders.getConsignee() != null ? orders.getConsignee() : orders.getUserName());
        orders.setPhone(orders.getPhone() != null ? orders.getPhone() : "");
        orders.setAddress(orders.getTableName() != null ? "堂食-" + orders.getTableName() : "堂食");
        orders.setAddressBookId(null); // 堂食无地址簿
    }

    /**
     * 落库堂食订单与明细并扣减库存，失败时释放幂等锁（等价抽取）。
     */
    private void saveEatInOrderWithLockRelease(Orders orders, List<OrderDetail> orderDetails,
            boolean eatInLockAcquired, String lockKey) {
        try {
            this.save(orders);
            orderDetailService.saveBatch(orderDetails);
            // 扣减库存
            this.deductStockForOrderDetails(orderDetails);
        } catch (RuntimeException e) {
            // 落库失败时释放幂等锁，允许用户重试
            if (eatInLockAcquired && redisTemplate != null) {
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception ignored) {
                    // 释放锁失败不影响异常抛出
                }
            }
            throw e;
        }
    }

    /**
     * 堂食订单落库后续处理：更新桌台状态为占用 + 自动打印（等价抽取）。
     */
    private void afterEatInOrderSaved(Long tableId, long orderId) {
        // 更新桌台状态为占用，并绑定当前订单（结账依赖 currentOrderId）
        if (diningTableService != null) {
            try {
                diningTableService.changeStatus(tableId, com.reggie.enums.DiningTableStatus.OCCUPIED.getValue());
                // 绑定 currentOrderId，使结账按钮能正确跳转收银台
                diningTableService.bindOrderId(tableId, orderId);
                log.info("[堂食] 桌台已标记为占用并绑定订单: tableId={}, orderId={}", tableId, orderId);
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.error("[堂食] 更新桌台状态失败: tableId={}, error={}", tableId, e.getMessage(), e);
                throw e;
            }
        }

        // 自动触发打印（异步）
        if (printerService != null) {
            final long finalOrderId = orderId;
            try {
                printerService.printOrder(finalOrderId, "BILL");
                printerService.printOrder(finalOrderId, "KITCHEN");
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.warn("[打印] 堂食订单打印触发失败，订单ID={}, 原因={}", finalOrderId, e.getMessage(), e);
            }
        }
    }

    // ==================== 库存扣减/回退公共方法 ====================

    /**
     * 库存操作函数式接口
     * @return 操作是否成功
     */
    @FunctionalInterface
    private interface StockOperation {
        boolean apply(Long dishId, BigDecimal qty);
    }

    /**
     * 处理菜品/套餐的库存操作（扣减或回退）
     * 统一处理单品菜品和套餐内所有菜品的库存变更，消除重复代码
     *
     * @param dishId     单品菜品ID（可为null）
     * @param setmealId  套餐ID（可为null）
     * @param quantity   数量
     * @param operation  库存操作（扣减或回退）
     * @return 操作是否全部成功
     */
    private boolean processStockForItems(Long dishId, Long setmealId, BigDecimal quantity, StockOperation operation) {
        boolean success = true;

        // 单品菜品
        if (dishId != null) {
            if (!operation.apply(dishId, quantity)) {
                success = false;
            }
        }

        // 套餐：处理套餐内所有菜品
        if (setmealId != null) {
            LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
            sdWrapper.eq(SetmealDish::getSetmealId, setmealId);
            List<SetmealDish> setmealDishes = setmealDishService.list(sdWrapper);
            for (SetmealDish sd : setmealDishes) {
                int copies = sd.getCopies() != null ? sd.getCopies() : 1;
                if (!operation.apply(sd.getDishId(), quantity.multiply(new BigDecimal(copies)))) {
                    success = false;
                }
            }
        }

        return success;
    }

    /**
     * 扣减库存操作（购物车维度）
     */
    private void deductStockForOrder(List<ShoppingCart> shoppingCarts) {
        for (ShoppingCart item : shoppingCarts) {
            int number = item.getNumber() != null ? item.getNumber() : 1;
            BigDecimal qty = new BigDecimal(number);
            processStockForItems(item.getDishId(), item.getSetmealId(), qty, this::deductStockAtomicVoid);
        }
    }

    /**
     * 扣减库存操作（订单明细维度）
     */
    private void deductStockForOrderDetails(List<OrderDetail> orderDetails) {
        for (OrderDetail detail : orderDetails) {
            int number = detail.getNumber() != null ? detail.getNumber() : 1;
            BigDecimal qty = new BigDecimal(number);
            processStockForItems(detail.getDishId(), detail.getSetmealId(), qty, this::deductStockAtomicVoid);
        }
    }


    /**
     * 扣减库存原子操作（void 版本，失败时抛异常）
     */
    private boolean deductStockAtomicVoid(Long dishId, BigDecimal qty) {
        deductStockAtomic(dishId, qty);
        return true;
    }

    /**
     * 使用乐观锁原子扣减菜品库存
     * WHERE stock_qty >= qty，防止并发超卖
     */
    private void deductStockAtomic(Long dishId, BigDecimal qty) {
        if (dishId == null || qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        dishService.deductStock(dishId, qty);
        dishService.autoToggleSoldOut(dishId);
        // 原料库存联动：按 BOM 配方同步扣减原料
        if (materialStockService != null) {
            materialStockService.deductMaterialStock(dishId, qty);
        }
    }


    /**
     * 后台分页查询订单
     *
     * @param page 页码
     * @param pageSize 每页大小
     * @param number 订单号
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @param status 订单状态（可选）
     * @return 订单分页结果
     */
    @Override
    public Page<Orders> orderPage(int page, int pageSize, String number, String beginTime, String endTime,
            Integer status) {
        Page<Orders> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();

        // 安全加固：后台订单分页必须附加租户条件，防止跨租户数据泄露
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(Orders::getTenantId, tenantId);
        }

        queryWrapper.like(StringUtils.isNotBlank(number), Orders::getNumber, number);
        queryWrapper.eq(status != null, Orders::getStatus, status);

        if (StringUtils.isNotBlank(beginTime)) {
            queryWrapper.ge(Orders::getOrderTime, beginTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le(Orders::getOrderTime, endTime);
        }

        queryWrapper.orderByDesc(Orders::getOrderTime);
        this.page(pageInfo, queryWrapper);
        backfillUserInfoBatch(pageInfo.getRecords());
        return pageInfo;
    }

    /**
     * 用户端分页查询订单
     *
     * @param page 页码
     * @param pageSize 每页大小
     * @param status 订单状态（可选）
     * @return 订单分页结果
     */
    @Override
    public Page<OrderDto> userPage(int page, int pageSize, Integer status) {
        Page<Orders> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        if (status != null) {
            queryWrapper.eq(Orders::getStatus, status);
        }
        queryWrapper.orderByDesc(Orders::getOrderTime);
        this.page(pageInfo, queryWrapper);

        backfillUserInfoBatch(pageInfo.getRecords());

        List<Long> orderIds = pageInfo.getRecords().stream().map(Orders::getId).collect(Collectors.toList());
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<OrderDetail> detailWrapper = new LambdaQueryWrapper<>();
            detailWrapper.in(OrderDetail::getOrderId, orderIds);
            List<OrderDetail> details = orderDetailService.list(detailWrapper);
            // Pre-group details by orderId to avoid O(n²) filtering
            Map<Long, List<OrderDetail>> detailsMap = details.stream()
                    .collect(Collectors.groupingBy(OrderDetail::getOrderId));

            List<OrderDto> orderDtoList = pageInfo.getRecords().stream().map(order -> {
                OrderDto dto = new OrderDto();
                org.springframework.beans.BeanUtils.copyProperties(order, dto);
                dto.setOrderDetails(detailsMap.getOrDefault(order.getId(), Collections.emptyList()));
                return dto;
            }).collect(Collectors.toList());
            Page<OrderDto> dtoPage = PageUtils.of(page, pageSize);
            dtoPage.setTotal(pageInfo.getTotal());
            dtoPage.setRecords(orderDtoList);
            return dtoPage;
        }
        // 空数据时同样返回 OrderDto 分页，保证前端拿到的始终是统一结构（含 orderDetails 字段）
        Page<OrderDto> emptyPage = PageUtils.of(page, pageSize);
        emptyPage.setTotal(0);
        emptyPage.setRecords(Collections.emptyList());
        return emptyPage;
    }

    /**
     * 用户订单列表（最近订单）
     *
     * @return 订单列表
     */
    @Override
    public List<Orders> userList() {
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(Orders::getOrderTime);
        List<Orders> result = this.list(queryWrapper);
        backfillUserInfoBatch(result);
        return result;
    }

    /**
     * 查询列表 pending checkout。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<Orders> listPendingCheckout(Long tenantId) {
        // 查本租户下所有待收银订单，两类场景都要覆盖：
        // 1. STATUS_PENDING_PAY 且 source=EAT_IN（堂食扫码/开台订单，待线上支付或线下收银）
        // 2. STATUS_ORDERED 且 payMethod 为空（历史堂食下单，尚未完成收款）
        LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
        qw.eq(Orders::getTenantId, tenantId)
                .and(w -> w.eq(Orders::getStatus, Orders.STATUS_PENDING_PAY)
                                  .eq(Orders::getSource, "EAT_IN")
                        .or()
                        .eq(Orders::getStatus, Orders.STATUS_ORDERED)
                                  .isNull(Orders::getPayMethod))
                // 修改点(2026-09-18)：排除「一键开台」产生的占位单（amount=0 且无订单明细）。
                // 此前这类 ¥0.00 幽灵单会永久滞留在待结账列表，点进去结账还会报「订单金额异常」。
                .gt(Orders::getAmount, BigDecimal.ZERO)
                .orderByDesc(Orders::getOrderTime);
        List<Orders> orders = this.list(qw);
        // 二次过滤：排除已有收银记录的订单（payMethod 设 null 但有收银记录的情况）
        List<Long> orderIds = new ArrayList<>();
        for (Orders o : orders) {
            orderIds.add(o.getId());
        }
        if (orderIds.isEmpty()) {
            return orders;
        }
        LambdaQueryWrapper<CashierRecord> crQw = new LambdaQueryWrapper<>();
        crQw.select(CashierRecord::getOrderId)
                .in(CashierRecord::getOrderId, orderIds);
        List<CashierRecord> records = cashierRecordMapper.selectList(crQw);
        Set<Long> checkedOutIds = new HashSet<>();
        for (CashierRecord cr : records) {
            checkedOutIds.add(cr.getOrderId());
        }
        List<Orders> result = new ArrayList<>();
        for (Orders o : orders) {
            if (!checkedOutIds.contains(o.getId())) {
                result.add(o);
            }
        }
        return result;
    }

    /**
     * 再来一单（将历史订单商品添加到购物车）
     * 自动合并购物车中已存在的商品（累加数量）
     *
     * @param orderId 原订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void again(Long orderId) {
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(wrapper);

        if (details.isEmpty()) {
            return;
        }

        Long userId = BaseContext.getCurrentId();

        LambdaQueryWrapper<ShoppingCart> cartQuery = new LambdaQueryWrapper<>();
        cartQuery.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> existingCarts = shoppingCartService.list(cartQuery);

        // 构建 Map 方便查找：key = "dishId:xxx" 或 "setmealId:xxx"（含口味区分）
        java.util.Map<String, ShoppingCart> existingMap = new java.util.HashMap<>();
        for (ShoppingCart cart : existingCarts) {
            String key = cart.getDishId() != null
                ? "dishId:" + cart.getDishId() + ":flavor:" + (cart.getDishFlavor() == null ? "" : cart.getDishFlavor())
                : "setmealId:" + cart.getSetmealId() + ":flavor:" + (cart.getDishFlavor() == null ? "" : cart
                        .getDishFlavor());
            existingMap.put(key, cart);
        }

        java.util.List<ShoppingCart> toAdd = new java.util.ArrayList<>();
        java.util.List<ShoppingCart> toUpdate = new java.util.ArrayList<>();

        // N+1 修复：批量预加载菜品和套餐（等价抽取）
        java.util.Map<String, Object> maps = preloadDishAndSetmealMaps(details);
        @SuppressWarnings("unchecked")
        java.util.Map<Long, Dish> batchDishMap = (java.util.Map<Long, Dish>) maps.get("dishMap");
        @SuppressWarnings("unchecked")
        java.util.Map<Long, Setmeal> batchSetmealMap = (java.util.Map<Long, Setmeal>) maps.get("setmealMap");

        // 构建购物车新增/更新操作（等价抽取）
        processAgainDetails(details, userId, existingMap, batchDishMap, batchSetmealMap, toAdd, toUpdate);

        if (!toUpdate.isEmpty()) {
            shoppingCartService.updateBatchById(toUpdate);
        }
        if (!toAdd.isEmpty()) {
            shoppingCartService.saveBatch(toAdd);
        }
    }

    /**
     * 批量预加载菜品与套餐映射（等价抽取，降低方法长度）。
     *
     * @return {dishMap, setmealMap}
     */
    private java.util.Map<String, Object> preloadDishAndSetmealMaps(List<OrderDetail> details) {
        java.util.List<Long> batchDishIds = new java.util.ArrayList<>();
        java.util.List<Long> batchSetmealIds = new java.util.ArrayList<>();
        for (OrderDetail d : details) {
            if (d.getDishId() != null) batchDishIds.add(d.getDishId());
            else if (d.getSetmealId() != null) batchSetmealIds.add(d.getSetmealId());
        }
        java.util.Map<Long, Dish> batchDishMap = new java.util.HashMap<>();
        if (!batchDishIds.isEmpty()) {
            for (Dish d : dishService.listByIds(batchDishIds)) { batchDishMap.put(d.getId(), d); }
        }
        java.util.Map<Long, Setmeal> batchSetmealMap = new java.util.HashMap<>();
        if (!batchSetmealIds.isEmpty()) {
            for (Setmeal s : setmealService.listByIds(batchSetmealIds)) { batchSetmealMap.put(s.getId(), s); }
        }
        java.util.Map<String, Object> holder = new java.util.HashMap<>();
        holder.put("dishMap", batchDishMap);
        holder.put("setmealMap", batchSetmealMap);
        return holder;
    }

    /**
     * 按订单明细构建购物车的新增/更新操作（等价抽取）。
     */
    private void processAgainDetails(List<OrderDetail> details, Long userId,
            java.util.Map<String, ShoppingCart> existingMap, java.util.Map<Long, Dish> batchDishMap,
            java.util.Map<Long, Setmeal> batchSetmealMap, java.util.List<ShoppingCart> toAdd,
            java.util.List<ShoppingCart> toUpdate) {
        for (OrderDetail d : details) {
            String key = d.getDishId() != null
                ? "dishId:" + d.getDishId() + ":flavor:" + (d.getDishFlavor() == null ? "" : d.getDishFlavor())
                : "setmealId:" + d.getSetmealId() + ":flavor:" + (d.getDishFlavor() == null ? "" : d.getDishFlavor());

            ShoppingCart existing = existingMap.get(key);
            if (existing != null) {
                // 已存在，累加数量，并刷新为最新价格（修复 toUpdate 分支未刷新价格，防止历史购物车项沿用旧价）
                existing.setNumber(existing.getNumber() + (d.getNumber() != null ? d.getNumber() : 0));
                refreshCartFromSource(existing, d, batchDishMap, batchSetmealMap);
                toUpdate.add(existing);
            } else {
                // 不存在，新增——从数据库查询最新价格，防止历史订单中的旧价格被复用
                ShoppingCart cart = new ShoppingCart();
                cart.setName(d.getName());
                cart.setImage(d.getImage());
                cart.setUserId(userId);
                cart.setDishId(d.getDishId());
                cart.setSetmealId(d.getSetmealId());
                cart.setDishFlavor(d.getDishFlavor());
                cart.setNumber(d.getNumber());
                cart.setAmount(d.getAmount());
                cart.setCreateTime(LocalDateTime.now());
                // 重新从数据库查询最新价格
                refreshCartFromSource(cart, d, batchDishMap, batchSetmealMap);
                toAdd.add(cart);
            }
        }
    }

    /**
     * 更新 status。
     * @param status 参数 status
     * @param id 参数 id
     */
    @Override
    public void updateStatus(Integer status, Long id) {
        statusFlowService.updateStatus(status, id);
    }

    /**
     * 按菜品/套餐刷新购物车的价格、名称、图片（等价抽取，降低嵌套）。
     *
     * @param cart 购物车项
     * @param d 订单明细
     * @param batchDishMap 菜品批量映射
     * @param batchSetmealMap 套餐批量映射
     */
    private void refreshCartFromSource(ShoppingCart cart, OrderDetail d,
            java.util.Map<Long, Dish> batchDishMap, java.util.Map<Long, Setmeal> batchSetmealMap) {
        if (d.getDishId() != null) {
            Dish dish = batchDishMap.get(d.getDishId());
            if (dish != null && dish.getPrice() != null) {
                cart.setAmount(dish.getPrice());
                cart.setName(dish.getName());
                cart.setImage(dish.getImage());
            }
            return;
        }
        if (d.getSetmealId() != null) {
            Setmeal setmeal = batchSetmealMap.get(d.getSetmealId());
            if (setmeal != null && setmeal.getPrice() != null) {
                cart.setAmount(setmeal.getPrice());
                cart.setName(setmeal.getName());
                cart.setImage(setmeal.getImage());
            }
        }
    }

    // ==================== 后台订单管理 ====================

    /**
     * 确认 order。
     * @param id 参数 id
     */
    @Override
    public void confirmOrder(Long id) {
        statusFlowService.confirmOrder(id);
    }

    /**
     * 驳回 order。
     * @param id 参数 id
     */
    @Override
    public void rejectOrder(Long id) {
        statusFlowService.rejectOrder(id);
    }

    /**
     * 完成 order。
     * @param id 参数 id
     */
    @Override
    public void completeOrder(Long id) {
        statusFlowService.completeOrder(id);
    }

    /**
     * 取消 order。
     * @param id 参数 id
     * @param reason 参数 reason
     */
    @Override
    public void cancelOrder(Long id, String reason) {
        statusFlowService.cancelOrder(id, reason);
    }



    /**
     * 订单统计：今日各状态订单数量汇总
     */
    @Override
    public Map<String, Object> getOrderStatistics() {
        Map<String, Object> stats = new java.util.LinkedHashMap<>();
        Long tenantId = BaseContext.getCurrentTenantId();

        // 今日开始时间
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 全部订单数
        LambdaQueryWrapper<Orders> allWrapper = new LambdaQueryWrapper<>();
        allWrapper.eq(Orders::getTenantId, tenantId);
        stats.put("totalOrders", this.count(allWrapper));

        // 待接单
        LambdaQueryWrapper<Orders> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Orders::getTenantId, tenantId)
                      .eq(Orders::getStatus, Orders.STATUS_ORDERED);
        stats.put("pendingOrders", this.count(pendingWrapper));

        // 配送中
        LambdaQueryWrapper<Orders> deliveringWrapper = new LambdaQueryWrapper<>();
        deliveringWrapper.eq(Orders::getTenantId, tenantId)
                         .eq(Orders::getStatus, Orders.STATUS_DELIVERING);
        stats.put("deliveringOrders", this.count(deliveringWrapper));

        // 今日已完成
        LambdaQueryWrapper<Orders> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(Orders::getTenantId, tenantId)
                        .eq(Orders::getStatus, Orders.STATUS_COMPLETED)
                        .ge(Orders::getOrderTime, todayStart);
        stats.put("completedToday", this.count(completedWrapper));

        // 已取消
        LambdaQueryWrapper<Orders> cancelledWrapper = new LambdaQueryWrapper<>();
        cancelledWrapper.eq(Orders::getTenantId, tenantId)
                        .eq(Orders::getStatus, Orders.STATUS_CANCELLED);
        stats.put("cancelledOrders", this.count(cancelledWrapper));

        // 今日营业额（已完成订单）——聚合查询，避免全量加载内存求和
        java.math.BigDecimal totalRevenue = getBaseMapper().sumAmount(tenantId, Orders.STATUS_COMPLETED, todayStart);
        stats.put("todayRevenue", totalRevenue != null ? totalRevenue : java.math.BigDecimal.ZERO);

        return stats;
    }


    // ==================== 幂等性保护 ====================

    /**
     * 生成幂等令牌：userId_timestamp_uuid（使用UUID保证唯一性和安全性）
     */
    private String generateIdempotencyKey(Long userId) {
        return userId + "_" + System.currentTimeMillis() + "_"
            + java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 构建堂食订单明细签名：按 dishId/setmealId + number 排序后拼接，取稳定 hash。
     * 完全相同的明细列表（双击/重试）签名一致 → 幂等命中；菜品不同（加菜）签名不同 → 创建新订单。
     * 使用 String.hashCode() 足够：仅用于幂等去重，非安全场景，碰撞概率可接受。
     */
    private String buildEatInDetailSignature(List<OrderDetail> orderDetails) {
        List<String> parts = new ArrayList<>();
        for (OrderDetail d : orderDetails) {
            Long itemId = d.getDishId() != null ? d.getDishId() : d.getSetmealId();
            Integer num = d.getNumber() != null ? d.getNumber() : 0;
            parts.add(itemId + "x" + num);
        }
        Collections.sort(parts);
        return String.valueOf(parts.toString().hashCode());
    }

    /**
     * 检查幂等令牌是否已存在（重复提交检测）
     */
    @Override
    public Orders checkIdempotency(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getIdempotencyKey, idempotencyKey)
               .orderByDesc(Orders::getOrderTime)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    // ==================== 用户信息回填 ====================

    /**
     * 回填单个订单的用户信息（用户名、手机号、地址、收货人）
     * 当orders表冗余字段为空时，从user表和address_book表查询回填，确保前端正常显示
     *
     * @param order 订单实体
     */
    @Override
    public void backfillUserInfo(Orders order) {
        if (order != null) {
            backfillUserInfoBatch(Collections.singletonList(order));
        }
    }

    /**
     * 批量回填订单的用户信息
     * 通过userId关联user表获取userName，通过addressBookId关联address_book表获取phone/consignee/address
     * 只回填当前为空的字段，已有值的保持不变
     *
     * @param orders 订单列表
     */
    private void backfillUserInfoBatch(List<Orders> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }

        // 批量加载回填所需映射（等价抽取，降低方法长度）
        Map<Long, String> tableNameMap = collectTableNameMap(orders);
        Map<Long, User> userMap = collectUserMap(orders);
        Map<Long, AddressBook> addrMap = collectAddressMap(orders);

        // 回填各订单的空字段（等价抽取）
        applyBackfill(orders, tableNameMap, userMap, addrMap);
    }

    /**
     * 收集并批量加载桌台名映射（tableName 为空的订单）（等价抽取）。
     */
    private Map<Long, String> collectTableNameMap(List<Orders> orders) {
        Set<Long> tableIds = orders.stream()
                .filter(o -> StringUtils.isBlank(o.getTableName()))
                .map(Orders::getTableId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> tableNameMap = new HashMap<>();
        if (!tableIds.isEmpty() && diningTableService != null) {
            List<com.reggie.module.dining.model.DiningTable> tables =
                    diningTableService.listByIds(new ArrayList<>(tableIds));
            for (com.reggie.module.dining.model.DiningTable t : tables) {
                if (t != null && StringUtils.isNotBlank(t.getName())) {
                    tableNameMap.put(t.getId(), t.getName());
                }
            }
        }
        return tableNameMap;
    }

    /**
     * 收集并批量加载用户映射（userName 为空的订单）（等价抽取）。
     */
    private Map<Long, User> collectUserMap(List<Orders> orders) {
        Set<Long> userIds = orders.stream()
                .filter(o -> StringUtils.isBlank(o.getUserName()))
                .map(Orders::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(new ArrayList<>(userIds));
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }
        return userMap;
    }

    /**
     * 收集并批量加载地址簿映射（phone/address/consignee 任一为空的订单）（等价抽取）。
     */
    private Map<Long, AddressBook> collectAddressMap(List<Orders> orders) {
        Set<Long> addrIds = orders.stream()
                .filter(o -> StringUtils.isBlank(o.getPhone())
                        || StringUtils.isBlank(o.getAddress())
                        || StringUtils.isBlank(o.getConsignee()))
                .map(Orders::getAddressBookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AddressBook> addrMap = new HashMap<>();
        if (!addrIds.isEmpty()) {
            List<AddressBook> addrs = addressBookService.listByIds(new ArrayList<>(addrIds));
            for (AddressBook a : addrs) {
                addrMap.put(a.getId(), a);
            }
        }
        return addrMap;
    }

    /**
     * 回填订单的 tableName/userName/地址等空字段（等价抽取）。
     */
    private void applyBackfill(List<Orders> orders, Map<Long, String> tableNameMap, Map<Long, User> userMap,
            Map<Long, AddressBook> addrMap) {
        for (Orders order : orders) {
            // 回填tableName
            if (StringUtils.isBlank(order.getTableName()) && order.getTableId() != null) {
                String tableName = tableNameMap.get(order.getTableId());
                if (StringUtils.isNotBlank(tableName)) {
                    order.setTableName(tableName);
                }
            }

            // 回填userName
            if (StringUtils.isBlank(order.getUserName()) && order.getUserId() != null) {
                User user = userMap.get(order.getUserId());
                if (user != null && StringUtils.isNotBlank(user.getName())) {
                    order.setUserName(user.getName());
                }
            }

            // 回填phone/consignee/address
            if (order.getAddressBookId() != null) {
                AddressBook addr = addrMap.get(order.getAddressBookId());
                if (addr != null) {
                    fillAddressFields(order, addr);
                }
            }
        }
    }

    /**
     * 从地址簿回填订单的收件人/电话/地址（等价抽取，降低嵌套）。
     *
     * @param order 订单
     * @param addr 地址簿
     */
    private void fillAddressFields(Orders order, AddressBook addr) {
        if (StringUtils.isBlank(order.getPhone()) && StringUtils.isNotBlank(addr.getPhone())) {
            order.setPhone(addr.getPhone());
        }
        if (StringUtils.isBlank(order.getConsignee()) && StringUtils.isNotBlank(addr.getConsignee())) {
            order.setConsignee(addr.getConsignee());
        }
        if (!StringUtils.isBlank(order.getAddress())) {
            return;
        }
        String address = (addr.getProvinceName() == null ? "" : addr.getProvinceName())
                + (addr.getCityName() == null ? "" : addr.getCityName())
                + (addr.getDistrictName() == null ? "" : addr.getDistrictName())
                + (addr.getDetail() == null ? "" : addr.getDetail());
        if (StringUtils.isNotBlank(address)) {
            order.setAddress(address);
        }
    }

    // ==================== 平台订单支持 ====================

    /**
     * 获取 by platform order。
     * @param platformType 参数 platformType
     * @param platformOrderId 参数 platformOrderId
     * @return 返回结果
     */
    @Override
    public Orders getByPlatformOrder(String platformType, String platformOrderId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getPlatformType, platformType)
                .eq(Orders::getPlatformOrderId, platformOrderId)
                .eq(Orders::getIsDeleted, 0)
                .last("LIMIT 1");
        return this.getOne(wrapper, false);
    }

    /**
     * 处理 platform order page。
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @param platformType 参数 platformType
     * @param status 参数 status
     * @return 返回结果
     */
    @Override
    public Page<Orders> platformOrderPage(int page, int pageSize, String platformType, Integer status) {
        Page<Orders> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getIsDeleted, 0);
        if (StringUtils.isNotBlank(platformType)) {
            wrapper.eq(Orders::getPlatformType, platformType);
        }
        if (status != null) {
            wrapper.eq(Orders::getStatus, status);
        }
        wrapper.orderByDesc(Orders::getOrderTime);
        return this.page(pageParam, wrapper);
    }

    /**
     * 平台订单全量统计（供平台订单页顶部统计卡片使用，翻页不重算）
     * <p>与平台订单列表同租户语义：租户隔离由 TenantLineInnerInterceptor 自动注入；
     * 待接单=STATUS_ORDERED(2)、已完成=STATUS_COMPLETED(4)，金额仅统计已完成订单。</p>
     *
     * @param platformType 平台类型（可选，与列表筛选保持一致）
     * @param status       订单状态（可选，与列表筛选保持一致）
     * @return totalOrders/pendingOrders/completedOrders/amount
     */
    @Override
    public Map<String, Object> getPlatformOrderStatistics(String platformType, Integer status) {
        Map<String, Object> stats = new LinkedHashMap<>();
        // 租户隔离由拦截器自动注入，无需手动拼接 tenant_id（与 platformOrderPage 一致）
        LambdaQueryWrapper<Orders> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(Orders::getIsDeleted, 0);
        if (StringUtils.isNotBlank(platformType)) {
            totalWrapper.eq(Orders::getPlatformType, platformType);
        }
        if (status != null) {
            totalWrapper.eq(Orders::getStatus, status);
        }
        stats.put("totalOrders", this.count(totalWrapper));

        LambdaQueryWrapper<Orders> pendingWrapper = new LambdaQueryWrapper<>();
        pendingWrapper.eq(Orders::getIsDeleted, 0)
                      .eq(Orders::getStatus, Orders.STATUS_ORDERED);
        if (StringUtils.isNotBlank(platformType)) {
            pendingWrapper.eq(Orders::getPlatformType, platformType);
        }
        stats.put("pendingOrders", this.count(pendingWrapper));

        LambdaQueryWrapper<Orders> completedWrapper = new LambdaQueryWrapper<>();
        completedWrapper.eq(Orders::getIsDeleted, 0)
                        .eq(Orders::getStatus, Orders.STATUS_COMPLETED);
        if (StringUtils.isNotBlank(platformType)) {
            completedWrapper.eq(Orders::getPlatformType, platformType);
        }
        stats.put("completedOrders", this.count(completedWrapper));

        // 已完成订单金额总和——聚合查询，避免全量加载内存求和
        List<Map<String, Object>> amountRows = getBaseMapper().statPlatformAmount(
                platformType, Orders.STATUS_COMPLETED);
        Object amt = (amountRows != null && !amountRows.isEmpty()) ? amountRows.get(0).get("amt") : null;
        stats.put("amount", amt != null ? new BigDecimal(amt.toString()) : BigDecimal.ZERO);
        return stats;
    }

    // ==================== 堂食加菜 ====================

    /**
     * 为已有堂食订单追加菜品（加菜）
     * <p>
     * 流程：校验订单状态 → 服务端查价格 → 新建 OrderDetail 扣库存 → 重算总额
     * </p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addItemsToCurrentOrder(Long orderId,
                                       List<com.reggie.module.dining.dto.AddItemsToOrderDTO.OrderItem> items) {
        // 1. 校验订单存在、状态、租户归属
        Orders order = getById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null || !tenantId.equals(order.getTenantId())) {
            throw new CustomException("无权操作该订单");
        }
        if (order.getStatus() == null || order.getStatus() != com.reggie.enums.OrderStatus.PENDING_PAYMENT.getValue()) {
            throw new CustomException("订单状态不允许加菜，当前状态: " + order.getStatus());
        }

        // 2. 批量查菜品/套餐价格（防客户端篡改）
        java.util.Map<Long, Dish> dishMap = new java.util.HashMap<>();
        java.util.Map<Long, Setmeal> setmealMap = new java.util.HashMap<>();
        java.util.Set<Long> dishIds = new java.util.HashSet<>();
        java.util.Set<Long> setmealIds = new java.util.HashSet<>();
        for (com.reggie.module.dining.dto.AddItemsToOrderDTO.OrderItem item : items) {
            if (item.getDishId() != null) { dishIds.add(item.getDishId()); }
            if (item.getSetmealId() != null) { setmealIds.add(item.getSetmealId()); }
        }
        if (!dishIds.isEmpty()) {
            List<Dish> dishes = dishService.listByIds(dishIds);
            for (Dish d : dishes) { dishMap.put(d.getId(), d); }
        }
        if (!setmealIds.isEmpty()) {
            List<Setmeal> setmeals = setmealService.listByIds(setmealIds);
            for (Setmeal s : setmeals) { setmealMap.put(s.getId(), s); }
        }

        // 3. 构建 OrderDetail + 扣库存
        List<OrderDetail> newDetails = new java.util.ArrayList<>();
        for (com.reggie.module.dining.dto.AddItemsToOrderDTO.OrderItem item : items) {
            if (item.getDishId() == null && item.getSetmealId() == null) {
                throw new CustomException("菜品ID和套餐ID不能同时为空");
            }
            int qty = item.getNumber() != null && item.getNumber() > 0 ? item.getNumber() : 1;

            OrderDetail detail = new OrderDetail();
            detail.setOrderId(orderId);
            detail.setNumber(qty);
            detail.setDishFlavor(item.getFlavor());
            detail.setRemark(item.getRemark());
            detail.setTenantId(tenantId);

            if (item.getDishId() != null) {
                Dish dish = dishMap.get(item.getDishId());
                if (dish == null) { throw new CustomException("菜品不存在: " + item.getDishId()); }
                detail.setDishId(dish.getId());
                detail.setName(dish.getName());
                detail.setImage(dish.getImage());
                // 服务端价格，防篡改
                detail.setAmount(dish.getPrice());
                // 扣库存
                dishService.deductStock(dish.getId(), BigDecimal.valueOf(qty));
            } else {
                Setmeal setmeal = setmealMap.get(item.getSetmealId());
                if (setmeal == null) { throw new CustomException("套餐不存在: " + item.getSetmealId()); }
                detail.setSetmealId(setmeal.getId());
                detail.setName(setmeal.getName());
                detail.setImage(setmeal.getImage());
                detail.setAmount(setmeal.getPrice());
                // 套餐扣减每个子菜品库存
                LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
                sdWrapper.eq(SetmealDish::getSetmealId, setmeal.getId());
                List<SetmealDish> sdList = setmealDishService.list(sdWrapper);
                for (SetmealDish sd : sdList) {
                    int copies = sd.getCopies() != null ? sd.getCopies() : 1;
                    dishService.deductStock(sd.getDishId(),
                            BigDecimal.valueOf((long) copies * qty));
                }
            }
            newDetails.add(detail);
        }
        orderDetailService.saveBatch(newDetails);

        // 4. 重算订单总额：SELECT SUM(amount * number) FROM order_detail WHERE order_id = ?
        BigDecimal newTotal = BigDecimal.ZERO;
        LambdaQueryWrapper<OrderDetail> sumQw = new LambdaQueryWrapper<>();
        sumQw.eq(OrderDetail::getOrderId, orderId)
             .select(OrderDetail::getAmount, OrderDetail::getNumber);
        List<OrderDetail> allDetails = orderDetailService.list(sumQw);
        for (OrderDetail d : allDetails) {
            BigDecimal lineTotal = d.getAmount().multiply(BigDecimal.valueOf(d.getNumber()));
            newTotal = newTotal.add(lineTotal);
        }
        Orders update = new Orders();
        update.setId(orderId);
        update.setAmount(newTotal);
        updateById(update);

        log.info("[加菜] orderId={}, 新增{}个菜品, 新总额={}", orderId, newDetails.size(), newTotal);
    }
}






