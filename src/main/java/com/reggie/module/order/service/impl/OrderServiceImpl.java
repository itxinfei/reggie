package com.reggie.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.dto.OrderDto;
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
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.setmeal.service.SetmealDishService;
import com.reggie.module.setmeal.service.SetmealService;
import com.reggie.module.shopping.service.ShoppingCartService;
import com.reggie.module.user.service.UserService;
import com.reggie.module.printer.service.PrinterService;
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
     * 收银记录 Mapper（用于查询订单是否已有收银记录，判断待收银状态）
     */
    @Autowired(required = false)
    private CashierRecordMapper cashierRecordMapper;

    /**
     * Redis 模板（可选，用于下单幂等性 SETNX 抢占，防止 check-then-act 竞态）
     */
    @Autowired(required = false)
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

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

        //查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(wrapper);

        if (shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new CustomException("购物车为空，不能下单");
        }

        //查询用户数据
        User user = userService.getById(userId);
        if (user == null) {
            throw new CustomException("用户信息不存在，不能下单");
        }

        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        if (addressBookId == null) {
            throw new CustomException("请选择收货地址");
        }
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if (addressBook == null) {
            throw new CustomException("用户地址信息有误，不能下单");
        }

        long orderId = IdWorker.getId();//订单号

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            return orderDetail;
        }).collect(Collectors.toList());

        // 使用 BigDecimal 精确计算总金额，避免 intValue() 精度丢失
        for (ShoppingCart item : shoppingCarts) {
            BigDecimal itemAmount = item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO;
            BigDecimal itemNumber = new BigDecimal(item.getNumber() != null ? item.getNumber() : 0);
            totalAmount = totalAmount.add(itemAmount.multiply(itemNumber));
        }


        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        // 修复：下单状态按支付方式分流，避免支付主链路断裂。
        // - COD(货到付款, payMethod=3)：无需线上支付，直接 STATUS_ORDERED + 立即接单处理
        // - WeChat/Alipay(payMethod=1/2) 或未传 payMethod：需线上支付，STATUS_PENDING_PAY，
        //   支付成功后由 PaymentOrderServiceImpl.handlePaymentSuccess() 流转为 ORDERED 并回填 checkoutTime
        // 此前无条件设 STATUS_ORDERED 导致 PaymentController.pay() 拒绝支付（要求 PENDING_PAY）
        if (orders.getPayMethod() != null && orders.getPayMethod() == 3) {
            orders.setCheckoutTime(LocalDateTime.now());
            orders.setStatus(Orders.STATUS_ORDERED);
        } else {
            orders.setCheckoutTime(null);
            orders.setStatus(Orders.STATUS_PENDING_PAY);
        }
        orders.setAmount(totalAmount.setScale(2, java.math.RoundingMode.HALF_UP));//总金额（精确计算）
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
        //向订单表插入数据，一条数据
        // 修复 check-then-act 竞态：用 Redis SETNX 原子抢占幂等令牌，防止并发重复下单
        String idempotencyKey = orders.getIdempotencyKey();
        String lockKey = "order:idem:" + idempotencyKey;
        boolean lockAcquired = false;
        if (redisTemplate != null && idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", IDEMPOTENCY_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
            lockAcquired = ok != null && ok;
            if (!lockAcquired) {
                // 并发请求或已下单：查询既有订单并回填，避免重复落库
                Orders existing = checkIdempotency(idempotencyKey);
                if (existing != null) {
                    orders.setId(existing.getId());
                    orders.setNumber(existing.getNumber());
                    orders.setAmount(existing.getAmount());
                    orders.setStatus(existing.getStatus());
                    return;
                }
                // 锁存在但订单未落库（并发处理中），拒绝重复提交
                throw new CustomException("订单正在处理中，请勿重复提交");
            }
        }

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

        // 自动触发打印（异步，不影响下单主流程）
        if (printerService != null) {
            final long finalOrderId = orderId;
            try {
                printerService.printOrder(finalOrderId, "BILL");
                printerService.printOrder(finalOrderId, "KITCHEN");
            } catch (Exception e) {
                // 打印失败不影响下单结果
                log.warn("[打印] 自动打印触发失败，订单ID={}, 原因={}", finalOrderId, e.getMessage(), e);
            }
        }
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

        // 设置堂食来源
        orders.setSource(com.reggie.enums.OrderSource.EAT_IN.getValue());
        Long tableId = orders.getTableId();
        if (tableId == null) {
            throw new CustomException("桌台信息缺失，请重新扫码");
        }

        // 修复堂食并发重复下单：用 Redis SETNX 原子抢占幂等令牌，防止同一桌台并发扫码重复下单
        // 修复 P0-3：幂等键去掉 UUID（原实现拼入随机值导致每次 key 不同，Redis SETNX 防重完全失效）
        Long eatInUserId = BaseContext.getCurrentId();
        String eatInIdemKey = (eatInUserId != null ? eatInUserId.toString() : "unknown")
                + "_" + (tableId != null ? tableId.toString() : "unknown");
        orders.setIdempotencyKey(eatInIdemKey);
        String lockKey = "order:eatin:idem:" + tableId + ":" + (eatInUserId != null ? eatInUserId : "unknown");
        boolean eatInLockAcquired = false;
        if (redisTemplate != null) {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, eatInIdemKey, IDEMPOTENCY_TTL_MINUTES, java.util.concurrent.TimeUnit.MINUTES);
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

        // 计算总金额（价格从菜品/套餐表服务端查询，防止客户端篡改）
        BigDecimal totalAmount = BigDecimal.ZERO;
        Long currentTenantId = BaseContext.getCurrentTenantId();

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

        // 设置订单字段
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        // 修复：堂食订单按支付方式分流
        // - payMethod=3(COD/线下收银)：已"付过款"，直接 STATUS_ORDERED
        // - payMethod=1(微信)/2(支付宝) 或未传：需线上支付，STATUS_PENDING_PAY
        if (orders.getPayMethod() != null && orders.getPayMethod() == 3) {
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

        // 更新桌台状态为占用
        if (diningTableService != null) {
            try {
                diningTableService.changeStatus(tableId, com.reggie.enums.DiningTableStatus.OCCUPIED.getValue());
                log.info("[堂食] 桌台已标记为占用: tableId={}, orderId={}", tableId, orderId);
            } catch (Exception e) {
                log.warn("[堂食] 更新桌台状态失败: tableId={}, error={}", tableId, e.getMessage(), e);
            }
        }

        // 自动触发打印（异步）
        if (printerService != null) {
            final long finalOrderId = orderId;
            try {
                printerService.printOrder(finalOrderId, "BILL");
                printerService.printOrder(finalOrderId, "KITCHEN");
            } catch (Exception e) {
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
    public Page<Orders> orderPage(int page, int pageSize, String number, String beginTime, String endTime, Integer status) {
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
    public Page<?> userPage(int page, int pageSize, Integer status) {
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
        return pageInfo;
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

        // 构建 Map 方便查找：key = "dishId:xxx" 或 "setmealId:xxx"
        java.util.Map<String, ShoppingCart> existingMap = new java.util.HashMap<>();
        for (ShoppingCart cart : existingCarts) {
            String key = cart.getDishId() != null
                ? "dishId:" + cart.getDishId()
                : "setmealId:" + cart.getSetmealId();
            existingMap.put(key, cart);
        }

        java.util.List<ShoppingCart> toAdd = new java.util.ArrayList<>();
        java.util.List<ShoppingCart> toUpdate = new java.util.ArrayList<>();

        // N+1 修复：批量预加载菜品和套餐，避免循环内 4 次 getById
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

        for (OrderDetail d : details) {
            String key = d.getDishId() != null
                ? "dishId:" + d.getDishId()
                : "setmealId:" + d.getSetmealId();

            ShoppingCart existing = existingMap.get(key);
            if (existing != null) {
                // 已存在，累加数量，并刷新为最新价格（修复 toUpdate 分支未刷新价格，防止历史购物车项沿用旧价）
                existing.setNumber(existing.getNumber() + (d.getNumber() != null ? d.getNumber() : 0));
                if (d.getDishId() != null) {
                    Dish dish = batchDishMap.get(d.getDishId());
                    if (dish != null && dish.getPrice() != null) {
                        existing.setAmount(dish.getPrice());
                        existing.setName(dish.getName());
                        existing.setImage(dish.getImage());
                    }
                } else if (d.getSetmealId() != null) {
                    Setmeal setmeal = batchSetmealMap.get(d.getSetmealId());
                    if (setmeal != null && setmeal.getPrice() != null) {
                        existing.setAmount(setmeal.getPrice());
                        existing.setName(setmeal.getName());
                        existing.setImage(setmeal.getImage());
                    }
                }
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
                if (d.getDishId() != null) {
                    Dish dish = batchDishMap.get(d.getDishId());
                    if (dish != null && dish.getPrice() != null) {
                        cart.setAmount(dish.getPrice());
                        cart.setName(dish.getName());
                        cart.setImage(dish.getImage());
                    }
                } else if (d.getSetmealId() != null) {
                    Setmeal setmeal = batchSetmealMap.get(d.getSetmealId());
                    if (setmeal != null && setmeal.getPrice() != null) {
                        cart.setAmount(setmeal.getPrice());
                        cart.setName(setmeal.getName());
                        cart.setImage(setmeal.getImage());
                    }
                }

                toAdd.add(cart);
            }
        }

        if (!toUpdate.isEmpty()) {
            shoppingCartService.updateBatchById(toUpdate);
        }
        if (!toAdd.isEmpty()) {
            shoppingCartService.saveBatch(toAdd);
        }
    }

    @Override
    public void updateStatus(Integer status, Long id) {
        statusFlowService.updateStatus(status, id);
    }

    // ==================== 后台订单管理 ====================

    @Override
    public void confirmOrder(Long id) {
        statusFlowService.confirmOrder(id);
    }

    @Override
    public void rejectOrder(Long id) {
        statusFlowService.rejectOrder(id);
    }

    @Override
    public void completeOrder(Long id) {
        statusFlowService.completeOrder(id);
    }

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

        // 收集需要回填的userIds（userName为空）
        Set<Long> userIds = orders.stream()
                .filter(o -> StringUtils.isBlank(o.getUserName()))
                .map(Orders::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 收集需要回填的addressBookIds（phone/address/consignee任一为空）
        Set<Long> addrIds = orders.stream()
                .filter(o -> StringUtils.isBlank(o.getPhone())
                        || StringUtils.isBlank(o.getAddress())
                        || StringUtils.isBlank(o.getConsignee()))
                .map(Orders::getAddressBookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 批量查询user表
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userService.listByIds(new ArrayList<>(userIds));
            for (User u : users) {
                userMap.put(u.getId(), u);
            }
        }

        // 批量查询address_book表
        Map<Long, AddressBook> addrMap = new HashMap<>();
        if (!addrIds.isEmpty()) {
            List<AddressBook> addrs = addressBookService.listByIds(new ArrayList<>(addrIds));
            for (AddressBook a : addrs) {
                addrMap.put(a.getId(), a);
            }
        }

        // 回填各订单的空字段
        for (Orders order : orders) {
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
                    if (StringUtils.isBlank(order.getPhone()) && StringUtils.isNotBlank(addr.getPhone())) {
                        order.setPhone(addr.getPhone());
                    }
                    if (StringUtils.isBlank(order.getConsignee()) && StringUtils.isNotBlank(addr.getConsignee())) {
                        order.setConsignee(addr.getConsignee());
                    }
                    if (StringUtils.isBlank(order.getAddress())) {
                        String address = (addr.getProvinceName() == null ? "" : addr.getProvinceName())
                                + (addr.getCityName() == null ? "" : addr.getCityName())
                                + (addr.getDistrictName() == null ? "" : addr.getDistrictName())
                                + (addr.getDetail() == null ? "" : addr.getDetail());
                        if (StringUtils.isNotBlank(address)) {
                            order.setAddress(address);
                        }
                    }
                }
            }
        }
    }

    // ==================== 平台订单支持 ====================

    @Override
    public Orders getByPlatformOrder(String platformType, String platformOrderId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getPlatformType, platformType)
                .eq(Orders::getPlatformOrderId, platformOrderId)
                .eq(Orders::getIsDeleted, 0)
                .last("LIMIT 1");
        return this.getOne(wrapper, false);
    }

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
}






