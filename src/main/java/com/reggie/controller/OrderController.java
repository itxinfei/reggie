package com.reggie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.R;
import com.reggie.entity.Orders;
import com.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：手机号={}，地址={}",
            LogMaskUtils.maskPhone(orders.getPhone()),
            LogMaskUtils.maskAddress(orders.getAddress()));
        orderService.submit(orders);
        return R.success("下单成功");
    }

    @GetMapping("/page")
    public R<Page<Orders>> page(int page, int pageSize, String number, String beginTime, String endTime) {
        Page pageInfo = orderService.orderPage(page, pageSize, number, beginTime, endTime);
        return R.success(pageInfo);
    }

    @GetMapping("/list")
    public R<List<Orders>> list() {
        List<Orders> list = orderService.userList();
        return R.success(list);
    }

    @GetMapping("/userPage")
    public R<Page> userPage(int page, int pageSize) {
        Page pageInfo = orderService.userPage(page, pageSize);
        return R.success(pageInfo);
    }

    @PostMapping("/again")
    public R<String> again(@RequestBody Orders orders) {
        orderService.again(orders.getId());
        return R.success("添加购物车成功");
    }

    @PutMapping
    public R<String> updateStatus(@RequestBody Orders orders) {
        orderService.updateStatus(orders.getStatus(), orders.getId());
        return R.success("操作成功");
    }
}