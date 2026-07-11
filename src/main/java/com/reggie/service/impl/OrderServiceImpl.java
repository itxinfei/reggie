package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.dto.OrderDto;
import com.reggie.entity.AddressBook;
import com.reggie.entity.Dish;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.entity.SetmealDish;
import com.reggie.entity.ShoppingCart;
import com.reggie.entity.User;
import com.reggie.mapper.OrderMapper;
import com.reggie.service.AddressBookService;
import com.reggie.service.DishService;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import com.reggie.service.SetmealDishService;
import com.reggie.service.ShoppingCartService;
import com.reggie.service.UserService;
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
     * 用户下单
     *
     * @param orders
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
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(Orders.STATUS_ORDERED);
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
        this.save(orders);

        //向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);

        this.deductStockForOrder(shoppingCarts);

        //清空购物车数据
        shoppingCartService.remove(wrapper);

        // 自动触发打印（异步，不影响下单主流程）
        if (printerService != null) {
            final long finalOrderId = orderId;
            try {
                printerService.printOrder(finalOrderId, "BILL");
                printerService.printOrder(finalOrderId, "KITCHEN");
            } catch (Exception e) {
                // 打印失败不影响下单结果
                log.warn("[打印] 自动打印触发失败，订单ID={}, 原因={}", finalOrderId, e.getMessage());
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

        // 查询桌台信息（用于填充桌台名称）
        if (diningTableService != null) {
            com.reggie.module.dining.model.DiningTable table = diningTableService.getById(tableId);
            if (table != null) {
                orders.setTableName(table.getName());
            }
        }

        Long userId = BaseContext.getCurrentId();
        long orderId = IdWorker.getId();

        // 计算总金额
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderDetail detail : orderDetails) {
            detail.setOrderId(orderId);
            BigDecimal amt = detail.getAmount() != null ? detail.getAmount() : BigDecimal.ZERO;
            Integer num = detail.getNumber() != null ? detail.getNumber() : 0;
            totalAmount = totalAmount.add(amt.multiply(new BigDecimal(num)));
        }

        // 设置订单字段
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(Orders.STATUS_ORDERED);
        orders.setAmount(totalAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setIdempotencyKey(generateIdempotencyKey(userId));
        orders.setUserName(orders.getUserName() != null ? orders.getUserName() : "堂食顾客");
        orders.setConsignee(orders.getConsignee() != null ? orders.getConsignee() : orders.getUserName());
        orders.setPhone(orders.getPhone() != null ? orders.getPhone() : "");
        orders.setAddress(orders.getTableName() != null ? "堂食-" + orders.getTableName() : "堂食");
        orders.setAddressBookId(null); // 堂食无地址簿

        this.save(orders);
        orderDetailService.saveBatch(orderDetails);

        // 扣减库存
        this.deductStockForOrderDetails(orderDetails);

        // 更新桌台状态为占用
        if (diningTableService != null) {
            try {
                diningTableService.changeStatus(tableId, com.reggie.enums.DiningTableStatus.OCCUPIED.getValue());
                log.info("[堂食] 桌台已标记为占用: tableId={}, orderId={}", tableId, orderId);
            } catch (Exception e) {
                log.warn("[堂食] 更新桌台状态失败: tableId={}, error={}", tableId, e.getMessage());
            }
        }

        // 自动触发打印（异步）
        if (printerService != null) {
            final long finalOrderId = orderId;
            try {
                printerService.printOrder(finalOrderId, "BILL");
                printerService.printOrder(finalOrderId, "KITCHEN");
            } catch (Exception e) {
                log.warn("[打印] 堂食订单打印触发失败，订单ID={}, 原因={}", finalOrderId, e.getMessage());
            }
        }
    }

    /**
     * 根据订单明细扣减菜品库存
     */
    private void deductStockForOrderDetails(List<OrderDetail> orderDetails) {
        for (OrderDetail detail : orderDetails) {
            int number = detail.getNumber() != null ? detail.getNumber() : 1;

            // 单品菜品
            if (detail.getDishId() != null) {
                Dish dish = dishService.getById(detail.getDishId());
                if (dish != null && dish.getStockQty() != null) {
                    BigDecimal newStock = dish.getStockQty().subtract(new BigDecimal(number));
                    if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                        throw new CustomException("菜品「" + dish.getName() + "」库存不足，当前库存：" + dish.getStockQty());
                    }
                    dish.setStockQty(newStock);
                    if (newStock.compareTo(BigDecimal.ZERO) == 0) {
                        dish.setStatus(0);
                    }
                    dishService.updateById(dish);
                }
            }

            // 套餐菜品
            if (detail.getSetmealId() != null) {
                LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
                sdWrapper.eq(SetmealDish::getSetmealId, detail.getSetmealId());
                List<SetmealDish> setmealDishes = setmealDishService.list(sdWrapper);
                for (SetmealDish sd : setmealDishes) {
                    Dish dish = dishService.getById(sd.getDishId());
                    if (dish != null && dish.getStockQty() != null) {
                        int qty = sd.getCopies() != null ? sd.getCopies() * number : number;
                        BigDecimal newStock = dish.getStockQty().subtract(new BigDecimal(qty));
                        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                            throw new CustomException("菜品「" + dish.getName() + "」库存不足");
                        }
                        dish.setStockQty(newStock);
                        if (newStock.compareTo(BigDecimal.ZERO) == 0) {
                            dish.setStatus(0);
                        }
                        dishService.updateById(dish);
                    }
                }
            }
        }
    }

    /**
     * 遍历购物车中的菜品/套餐，扣减对应库存
     * 库存不足时抛出异常，触发事务回滚
     */
    private void deductStockForOrder(List<ShoppingCart> shoppingCarts) {
        for (ShoppingCart item : shoppingCarts) {
            int number = item.getNumber() != null ? item.getNumber() : 1;

            // 单品菜品：直接扣减
            if (item.getDishId() != null) {
                Dish dish = dishService.getById(item.getDishId());
                if (dish != null && dish.getStockQty() != null) {
                    BigDecimal newStock = dish.getStockQty().subtract(new BigDecimal(number));
                    if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                        throw new CustomException("菜品「" + dish.getName() + "」库存不足，当前库存：" + dish.getStockQty());
                    }
                    dish.setStockQty(newStock);
                    // 库存为0时自动停售
                    if (newStock.compareTo(BigDecimal.ZERO) == 0) {
                        dish.setStatus(0);
                        log.info("[库存] 菜品「{}」已售罄，自动停售", dish.getName());
                    }
                    dishService.updateById(dish);
                    log.info("[库存扣减] 菜品{} 扣减{}份，剩余库存{}", dish.getName(), number, newStock);
                }
            }

            // 套餐：扣减套餐内所有菜品的库存
            if (item.getSetmealId() != null) {
                LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
                sdWrapper.eq(SetmealDish::getSetmealId, item.getSetmealId());
                List<SetmealDish> setmealDishes = setmealDishService.list(sdWrapper);
                for (SetmealDish sd : setmealDishes) {
                    Dish dish = dishService.getById(sd.getDishId());
                    if (dish != null && dish.getStockQty() != null) {
                        int qty = sd.getCopies() != null ? sd.getCopies() * number : number;
                        BigDecimal newStock = dish.getStockQty().subtract(new BigDecimal(qty));
                        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
                            throw new CustomException("菜品「" + dish.getName() + "」库存不足，当前库存：" + dish.getStockQty());
                        }
                        dish.setStockQty(newStock);
                        if (newStock.compareTo(BigDecimal.ZERO) == 0) {
                            dish.setStatus(0);
                            log.info("[库存] 菜品「{}」已售罄，自动停售", dish.getName());
                        }
                        dishService.updateById(dish);
                        log.info("[库存扣减] 套餐菜品{} 扣减{}份，剩余库存{}", dish.getName(), qty, newStock);
                    }
                }
            }
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
    public Page<Orders> orderPage(int page, int pageSize, String number, String beginTime, String endTime, Integer status) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();

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
        Page<Orders> pageInfo = new Page<>(page, pageSize);
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
            Page<OrderDto> dtoPage = new Page<>(page, pageSize, pageInfo.getTotal());
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

    /**
     * 再来一单（将历史订单商品添加到购物车）
     *
     * @param orderId 原订单ID
     */
    @Override
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

        for (OrderDetail d : details) {
            String key = d.getDishId() != null
                ? "dishId:" + d.getDishId()
                : "setmealId:" + d.getSetmealId();

            ShoppingCart existing = existingMap.get(key);
            if (existing != null) {
                // 已存在，累加数量
                existing.setNumber(existing.getNumber() + (d.getNumber() != null ? d.getNumber() : 0));
                toUpdate.add(existing);
            } else {
                // 不存在，新增
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

    /**
     * 更新订单状态
     *
     * @param status 目标状态
     * @param id 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer status, Long id) {
        Orders orders = this.getById(id);
        if (orders != null) {
            orders.setStatus(status);
            this.updateById(orders);
        }
    }

    // ==================== 后台订单管理 ====================

    /**
     * 接单：待接单(2) → 配送中(3)
     */
    @Override
    public void confirmOrder(Long id) {
        Orders order = this.getById(id);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_ORDERED)) {
            throw new CustomException("订单状态不正确，当前状态：" + getStatusName(order.getStatus()) + "，无法接单");
        }
        order.setStatus(Orders.STATUS_DELIVERING);
        this.updateById(order);
        log.info("订单已接单: id={}, number={}", id, order.getNumber());
    }

    /**
     * 拒单：待接单(2) → 已取消(5)
     */
    @Override
    public void rejectOrder(Long id) {
        Orders order = this.getById(id);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_ORDERED)) {
            throw new CustomException("订单状态不正确，当前状态：" + getStatusName(order.getStatus()) + "，无法拒单");
        }
        order.setStatus(Orders.STATUS_CANCELLED);
        this.updateById(order);
        log.warn("订单已拒单: id={}, number={}", id, order.getNumber());
    }

    /**
     * 完成订单：配送中(3) → 已完成(4)
     */
    @Override
    public void completeOrder(Long id) {
        Orders order = this.getById(id);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (!Objects.equals(order.getStatus(), Orders.STATUS_DELIVERING)) {
            throw new CustomException("订单状态不正确，当前状态：" + getStatusName(order.getStatus()) + "，无法完成");
        }
        order.setStatus(Orders.STATUS_COMPLETED);
        order.setCheckoutTime(LocalDateTime.now());
        this.updateById(order);
        log.info("订单已完成: id={}, number={}", id, order.getNumber());
    }

    /**
     * 取消订单：任意非完成/取消状态 → 已取消(5)
     * @param id 订单ID
     * @param reason 取消原因
     */
    @Override
    public void cancelOrder(Long id, String reason) {
        Orders order = this.getById(id);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (Objects.equals(order.getStatus(), Orders.STATUS_COMPLETED)) {
            throw new CustomException("订单已完成，无法取消");
        }
        if (Objects.equals(order.getStatus(), Orders.STATUS_CANCELLED)) {
            throw new CustomException("订单已取消，无需重复操作");
        }
        order.setStatus(Orders.STATUS_CANCELLED);
        if (reason != null && !reason.trim().isEmpty()) {
            order.setRemark(reason);
        }
        this.updateById(order);
        log.warn("订单已取消: id={}, number={}, reason={}", id, order.getNumber(), reason);
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

        // 今日营业额（已完成订单）
        LambdaQueryWrapper<Orders> revenueWrapper = new LambdaQueryWrapper<>();
        revenueWrapper.eq(Orders::getTenantId, tenantId)
                      .eq(Orders::getStatus, Orders.STATUS_COMPLETED)
                      .ge(Orders::getOrderTime, todayStart);
        List<Orders> completedOrders = this.list(revenueWrapper);
        java.math.BigDecimal totalRevenue = completedOrders.stream()
            .map(o -> o.getAmount() != null ? o.getAmount() : java.math.BigDecimal.ZERO)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        stats.put("todayRevenue", totalRevenue);

        return stats;
    }

    /**
     * 订单状态中文名称
     */
    private String getStatusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待付款";
            case 2: return "待接单";
            case 3: return "配送中";
            case 4: return "已完成";
            case 5: return "已取消";
            case 6: return "已退款";
            default: return "其他(" + status + ")";
        }
    }

    // ==================== 幂等性保护 ====================

    /**
     * 生成幂等令牌：userId_timestamp_random6
     */
    private String generateIdempotencyKey(Long userId) {
        return userId + "_" + System.currentTimeMillis() + "_"
            + String.format("%06d", (int)(Math.random() * 1000000));
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
}