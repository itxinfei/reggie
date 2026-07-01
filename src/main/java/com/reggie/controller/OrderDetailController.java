package com.reggie.controller;

import com.reggie.common.R;
import com.reggie.entity.OrderDetail;
import com.reggie.service.OrderDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单明细
 */
@Slf4j
@RestController
@RequestMapping("/orderDetail")
@Tag(name = "订单明细", description = "订单明细查询接口")
public class OrderDetailController {

    @Autowired
    private OrderDetailService orderDetailService;

    @GetMapping("/{id}")
    @Operation(summary = "查询订单明细", description = "根据ID查询订单明细详情")
    @Parameter(name = "id", description = "订单明细ID", required = true)
    public R<OrderDetail> get(@PathVariable Long id) {
        OrderDetail orderDetail = orderDetailService.getById(id);
        if (orderDetail != null) {
            return R.success(orderDetail);
        }
        return R.error("没有找到该对象");
    }
}