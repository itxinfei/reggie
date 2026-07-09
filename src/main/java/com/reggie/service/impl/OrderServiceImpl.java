package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.dto.OrderDto;
import com.reggie.entity.AddressBook;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.entity.ShoppingCart;
import com.reggie.entity.User;
import com.reggie.mapper.OrderMapper;
import com.reggie.service.AddressBookService;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import com.reggie.service.ShoppingCartService;
import com.reggie.service.UserService;
import com.reggie.module.printer.service.PrinterService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 打印服务（可选注入，无打印机配置时降级跳过）
     */
    @Autowired(required = false)
    private PrinterService printerService;

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

    @Override
    public Page<Orders> orderPage(int page, int pageSize, String number, String beginTime, String endTime) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.like(StringUtils.isNotBlank(number), Orders::getNumber, number);

        if (StringUtils.isNotBlank(beginTime)) {
            queryWrapper.ge(Orders::getOrderTime, beginTime);
        }
        if (StringUtils.isNotBlank(endTime)) {
            queryWrapper.le(Orders::getOrderTime, endTime);
        }

        queryWrapper.orderByDesc(Orders::getOrderTime);
        this.page(pageInfo, queryWrapper);
        return pageInfo;
    }

    @Override
    public Page<?> userPage(int page, int pageSize, Integer status) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        // 修改点：支持按订单状态筛选，不传则查全部
        if (status != null) {
            queryWrapper.eq(Orders::getStatus, status);
        }
        queryWrapper.orderByDesc(Orders::getOrderTime);
        this.page(pageInfo, queryWrapper);

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

    @Override
    public List<Orders> userList() {
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
        queryWrapper.orderByDesc(Orders::getOrderTime);
        return this.list(queryWrapper);
    }

    @Override
    public void again(Long orderId) {
        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderDetail::getOrderId, orderId);
        List<OrderDetail> details = orderDetailService.list(wrapper);

        if (details.isEmpty()) {
            return;
        }

        Long userId = BaseContext.getCurrentId();

        // 修改点：先查询用户购物车中已存在的商品，避免重复添加
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
}