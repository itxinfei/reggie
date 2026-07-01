package com.reggie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.R;
import com.reggie.entity.Orders;
import com.reggie.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单
 */
@Slf4j
@RestController
@RequestMapping("/order")
@Tag(name = "订单管理", description = "订单提交、查询及状态管理接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param orders
     * @return
     */
    @PostMapping("/submit")
    @Operation(summary = "提交订单", description = "用户下单")
    @Parameter(name = "orders", description = "订单信息", required = true)
    public R<String> submit(@RequestBody Orders orders){
        log.info("订单数据：手机号={}，地址={}",
            LogMaskUtils.maskPhone(orders.getPhone()),
            LogMaskUtils.maskAddress(orders.getAddress()));
        orderService.submit(orders);
        return R.success("下单成功");
    }

    @GetMapping("/page")
    @Operation(summary = "订单分页查询", description = "后台分页查询订单列表")
    @Parameter(name = "page", description = "页码", required = true)
    @Parameter(name = "pageSize", description = "每页数量", required = true)
    @Parameter(name = "number", description = "订单号（可选）")
    @Parameter(name = "beginTime", description = "开始时间（可选）")
    @Parameter(name = "endTime", description = "结束时间（可选）")
    public R<Page<Orders>> page(int page, int pageSize, String number, String beginTime, String endTime) {
        Page<Orders> pageInfo = orderService.orderPage(page, pageSize, number, beginTime, endTime);
        return R.success(pageInfo);
    }

    @GetMapping("/list")
    @Operation(summary = "查询订单列表", description = "查询用户的所有订单")
    public R<List<Orders>> list() {
        List<Orders> list = orderService.userList();
        return R.success(list);
    }

    @GetMapping("/userPage")
    @Operation(summary = "用户订单分页查询", description = "分页查询当前用户的订单")
    @Parameter(name = "page", description = "页码", required = true)
    @Parameter(name = "pageSize", description = "每页数量", required = true)
    public R<?> userPage(int page, int pageSize) {
        return R.success(orderService.userPage(page, pageSize));
    }

    @PostMapping("/again")
    @Operation(summary = "再来一单", description = "将订单商品重新添加到购物车")
    @Parameter(name = "orders", description = "订单信息", required = true)
    public R<String> again(@RequestBody Orders orders) {
        orderService.again(orders.getId());
        return R.success("添加购物车成功");
    }

    @PutMapping
    @Operation(summary = "更新订单状态", description = "更新订单状态")
    @Parameter(name = "orders", description = "订单状态信息", required = true)
    public R<String> updateStatus(@RequestBody Orders orders) {
        orderService.updateStatus(orders.getStatus(), orders.getId());
        return R.success("操作成功");
    }
}