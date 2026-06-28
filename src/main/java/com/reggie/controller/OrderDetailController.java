package com.reggie.controller;

import com.reggie.common.R;
import com.reggie.entity.OrderDetail;
import com.reggie.service.OrderDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单明细
 */
@Slf4j
@RestController
@RequestMapping("/orderDetail")
public class OrderDetailController {

    @Autowired
    private OrderDetailService orderDetailService;

    @GetMapping("/{id}")
    public R<OrderDetail> get(@PathVariable Long id) {
        OrderDetail orderDetail = orderDetailService.getById(id);
        if (orderDetail != null) {
            return R.success(orderDetail);
        }
        return R.error("没有找到该对象");
    }
}