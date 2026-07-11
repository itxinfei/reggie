package com.reggie.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.R;
import com.reggie.dto.OrderDto;
import com.reggie.dto.EatInOrderRequest;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import com.reggie.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/order")
@Tag(name = "订单管理", description = "订单提交、查询及状态管理接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private DashboardService dashboardService;

    /**
     * 用户下单
     * @param orders 订单信息
     * @return 订单关键信息（id, number, amount）
     */
    @PostMapping("/submit")
    @Operation(summary = "提交订单", description = "用户下单，返回订单ID、订单号和金额供前端跳转支付")
    @Parameter(name = "orders", description = "订单信息（含幂等令牌idempotencyKey）", required = true)
    public R<Map<String, Object>> submit(@RequestBody Orders orders){
        log.info("订单数据：手机号={}，地址={}",
            LogMaskUtils.maskPhone(orders.getPhone()),
            LogMaskUtils.maskAddress(orders.getAddress()));

        // 幂等性校验：检查是否重复提交
        String idempotencyKey = orders.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Orders existingOrder = orderService.checkIdempotency(idempotencyKey);
            if (existingOrder != null) {
                log.warn("检测到重复提交订单：idempotencyKey={}, orderId={}", idempotencyKey, existingOrder.getId());
                Map<String, Object> result = new HashMap<>();
                result.put("id", existingOrder.getId());
                result.put("number", existingOrder.getNumber());
                result.put("amount", existingOrder.getAmount());
                result.put("status", existingOrder.getStatus());
                result.put("duplicate", true);
                return R.success(result);
            }
        }

        // 设置租户ID
        orders.setTenantId(BaseContext.getCurrentTenantId());
        orderService.submit(orders);

        // 修改点：下单后清除 Dashboard 缓存，确保今日订单数实时更新
        clearDashboardCache();

        Map<String, Object> result = new HashMap<>();
        result.put("id", orders.getId());
        result.put("number", orders.getNumber());
        result.put("amount", orders.getAmount());
        result.put("status", orders.getStatus());
        result.put("duplicate", false);
        return R.success(result);
    }

    /**
     * 堂食扫码下单（不经过购物车，直接传入菜品列表）
     * @param request 堂食下单请求（含订单信息和明细列表）
     * @return 订单关键信息
     */
    @PostMapping("/eatIn")
    @Operation(summary = "堂食扫码下单", description = "顾客扫码点餐，直接传入菜品列表下单，自动更新桌台状态")
    @Parameter(name = "request", description = "堂食下单请求（订单信息+明细列表）", required = true)
    public R<Map<String, Object>> eatIn(@RequestBody @Validated EatInOrderRequest request) {
        log.info("[堂食] 扫码下单: tableId={}, customerCount={}, items={}",
            request.getOrder().getTableId(),
            request.getOrder().getCustomerCount(),
            request.getOrderDetails() != null ? request.getOrderDetails().size() : 0);

        // 构建 Orders 对象
        EatInOrderRequest.OrderInfo orderInfo = request.getOrder();
        Orders orders = new Orders();
        orders.setTableId(orderInfo.getTableId());
        orders.setTableName(orderInfo.getTableName());
        orders.setUserName(orderInfo.getUserName());
        orders.setPhone(orderInfo.getPhone());
        orders.setRemark(orderInfo.getRemark());
        orders.setPayMethod(orderInfo.getPayMethod());
        orders.setCustomerCount(orderInfo.getCustomerCount());

        // 幂等性校验
        String idempotencyKey = orderInfo.getUserName() + "_" + orderInfo.getTableId() + "_" + System.currentTimeMillis();
        orders.setIdempotencyKey(idempotencyKey);

        orders.setTenantId(BaseContext.getCurrentTenantId());
        orderService.submitEatInOrder(orders, request.getOrderDetails());

        // 清除 Dashboard 缓存
        clearDashboardCache();

        Map<String, Object> result = new HashMap<>();
        result.put("id", orders.getId());
        result.put("number", orders.getNumber());
        result.put("amount", orders.getAmount());
        result.put("status", orders.getStatus());
        result.put("tableId", orders.getTableId());
        result.put("tableName", orders.getTableName());
        return R.success(result);
    }

    /**
     * 根据ID查询订单详情
     * @param id 订单ID
     * @return 订单详情及关联明细
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询订单详情", description = "根据订单ID查询订单基本信息及关联明细信息")
    @Parameter(name = "id", description = "订单ID", required = true)
    public R<OrderDto> getById(@PathVariable Long id){
        Orders orders = orderService.getById(id);
        if (orders == null) {
            return R.error("订单不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(orders.getTenantId())) {
            return R.error("订单不属于当前租户");
        }
        orderService.backfillUserInfo(orders);
        OrderDto orderDto = new OrderDto();
        org.springframework.beans.BeanUtils.copyProperties(orders, orderDto);
        // 查询订单明细
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderDetail> detailWrapper =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        detailWrapper.eq(OrderDetail::getOrderId, id);
        orderDto.setOrderDetails(orderDetailService.list(detailWrapper));
        return R.success(orderDto);
    }

    /**
     * 后台分页查询订单列表
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param number 订单号（可选）
     * @param beginTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "订单分页查询", description = "后台分页查询订单列表")
    @Parameter(name = "page", description = "页码", required = true)
    @Parameter(name = "pageSize", description = "每页数量", required = true)
    @Parameter(name = "number", description = "订单号（可选）")
    @Parameter(name = "beginTime", description = "开始时间（可选）")
    @Parameter(name = "endTime", description = "结束时间（可选）")
    @Parameter(name = "status", description = "订单状态（可选，1=待付款,2=待接单/处理中,3=已接单/派送中,4=已完成,5=已取消,6=已退款）")
    public R<Page<Orders>> page(int page, int pageSize, String number, String beginTime, String endTime, @RequestParam(required = false) Integer status) {
        // 租户ID已由 LoginCheckFilter 设置到 BaseContext
        Page<Orders> pageInfo = orderService.orderPage(page, pageSize, number, beginTime, endTime, status);
        return R.success(pageInfo);
    }

    /**
     * 查询用户的所有订单
     *
     * @return 订单列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询订单列表", description = "查询用户的所有订单")
    public R<List<Orders>> list() {
        // 租户ID已由 LoginCheckFilter 设置到 BaseContext
        List<Orders> list = orderService.userList();
        return R.success(list);
    }

    /**
     * 分页查询当前用户的订单
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 订单状态（可选：1待付款 2派送中 3已派送 4已完成 5已取消，不传则查全部）
     * @return 分页结果
     */
    @GetMapping("/userPage")
    @Operation(summary = "用户订单分页查询", description = "分页查询当前用户的订单，支持按状态筛选")
    @Parameter(name = "page", description = "页码", required = true)
    @Parameter(name = "pageSize", description = "每页数量", required = true)
    @Parameter(name = "status", description = "订单状态（可选：1待付款 2待接单/处理中 3已接单/派送中 4已完成 5已取消 6已退款，不传则查全部）")
    public R<?> userPage(int page, int pageSize,
                         @RequestParam(required = false) Integer status) {
        // 租户ID已由 LoginCheckFilter 设置到 BaseContext
        return R.success(orderService.userPage(page, pageSize, status));
    }

    /**
     * 再来一单，将订单商品重新添加到购物车
     *
     * @param orders 订单信息
     * @return 操作结果
     */
    @PostMapping("/again")
    @Operation(summary = "再来一单", description = "将订单商品重新添加到购物车")
    @Parameter(name = "orders", description = "订单信息", required = true)
    public R<String> again(@RequestBody Orders orders) {
        Orders existing = orderService.getById(orders.getId());
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (existing == null || (currentTenantId != null && !currentTenantId.equals(existing.getTenantId()))) {
            return R.error("订单不存在或不属于当前租户");
        }
        orderService.again(orders.getId());
        return R.success("添加购物车成功");
    }

    /**
     * 更新订单状态
     *
     * @param orders 订单状态信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "更新订单状态", description = "更新订单状态")
    @Parameter(name = "orders", description = "订单状态信息", required = true)
    public R<String> updateStatus(@RequestBody Orders orders) {
        Orders existing = orderService.getById(orders.getId());
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (existing == null || (currentTenantId != null && !currentTenantId.equals(existing.getTenantId()))) {
            return R.error("订单不存在或不属于当前租户");
        }
        orderService.updateStatus(orders.getStatus(), orders.getId());
        // 修改点：订单状态变更后清除 Dashboard 缓存，防止数据不实
        clearDashboardCache();
        return R.success("操作成功");
    }

    // ==================== 后台订单管理 ====================

    /**
     * 接单：待接单(2) → 配送中(3)
     */
    @PutMapping("/confirm")
    @Operation(summary = "接单", description = "后台确认接单，订单状态从待接单变为配送中")
    @Parameter(name = "id", description = "订单ID", required = true)
    public R<String> confirm(@RequestParam Long id) {
        orderService.confirmOrder(id);
        return R.success("接单成功");
    }

    /**
     * 拒单：待接单(2) → 已取消(5)
     */
    @PutMapping("/reject")
    @Operation(summary = "拒单", description = "后台拒单，订单状态变为已取消")
    @Parameter(name = "id", description = "订单ID", required = true)
    public R<String> reject(@RequestParam Long id) {
        orderService.rejectOrder(id);
        return R.success("已拒单");
    }

    /**
     * 完成订单：配送中(3) → 已完成(4)
     */
    @PutMapping("/complete")
    @Operation(summary = "完成订单", description = "标记订单为已完成")
    @Parameter(name = "id", description = "订单ID", required = true)
    public R<String> complete(@RequestParam Long id) {
        orderService.completeOrder(id);
        return R.success("订单已完成");
    }

    /**
     * 取消订单：非完成/取消状态 → 已取消(5)
     */
    @PutMapping("/cancel")
    @Operation(summary = "取消订单", description = "取消订单，需填写取消原因")
    @Parameter(name = "id", description = "订单ID", required = true)
    @Parameter(name = "reason", description = "取消原因", required = false)
    public R<String> cancel(@RequestParam Long id, @RequestParam(required = false) String reason) {
        orderService.cancelOrder(id, reason);
        return R.success("订单已取消");
    }

    /**
     * 订单统计：今日各状态订单数量、营业额
     */
    @GetMapping("/statistics")
    @Operation(summary = "订单统计", description = "获取当前租户的订单统计数据，包含各状态数量和今日营业额")
    public R<Map<String, Object>> statistics() {
        Map<String, Object> stats = orderService.getOrderStatistics();
        return R.success(stats);
    }

    /**
     * 清除 Dashboard 缓存（在订单创建或状态变更后调用，确保概览数据实时准确）
     */
    private void clearDashboardCache() {
        try {
            Long tenantId = BaseContext.getCurrentTenantId();
            if (dashboardService != null && tenantId != null) {
                dashboardService.clearOverviewCache(tenantId);
            }
        } catch (Exception e) {
            log.warn("清除Dashboard缓存失败: {}", e.getMessage());
        }
    }
}