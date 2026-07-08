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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
    public Page<?> userPage(int page, int pageSize) {
        Page<Orders> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getUserId, BaseContext.getCurrentId());
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

        Long userId = BaseContext.getCurrentId();
        List<ShoppingCart> cartItems = details.stream().map(d -> {
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
            return cart;
        }).collect(Collectors.toList());

        shoppingCartService.saveBatch(cartItems);
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
}