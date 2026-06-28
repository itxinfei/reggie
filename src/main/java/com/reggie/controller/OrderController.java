package com.reggie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.entity.Orders;
import com.reggie.service.OrderService;
import lombok.extern.slf4j.Slf4j;
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
        log.info("订单数据：{}",orders);
        orderService.submit(orders);
        return R.success("下单成功");
    }

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String number, String beginTime, String endTime) {
        Page pageInfo = orderService.orderPage(page, pageSize, number, beginTime, endTime);
        return R.success(pageInfo);
    }

    @PutMapping
    public R<String> updateStatus(@RequestBody Orders orders) {
        orderService.updateStatus(orders.getStatus(), orders.getId());
        return R.success("操作成功");
    }
}