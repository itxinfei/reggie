package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.entity.OrderDetail;
import com.reggie.service.OrderDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单明细管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/orderDetail")
@Tag(name = "订单明细", description = "订单明细查询接口")
public class OrderDetailController {

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 根据ID查询订单明细详情
     */
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

    /**
     * 订单明细分页查询
     */
    @GetMapping("/page")
    @Operation(summary = "订单明细分页", description = "分页查询订单明细列表")
    public R<Page<OrderDetail>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "订单ID") @RequestParam(required = false) Long orderId) {
        Page<OrderDetail> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<OrderDetail> qw = new LambdaQueryWrapper<>();
        if (orderId != null) {
            qw.eq(OrderDetail::getOrderId, orderId);
        }
        qw.orderByDesc(OrderDetail::getId);
        orderDetailService.page(pageInfo, qw);
        return R.success(pageInfo);
    }
}