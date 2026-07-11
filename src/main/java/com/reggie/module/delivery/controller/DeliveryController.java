package com.reggie.module.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.AcceptOrderDTO;
import com.reggie.dto.SyncMenuDTO;
import com.reggie.dto.SyncStockDTO;
import com.reggie.module.delivery.model.DeliveryOrder;
import com.reggie.module.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 外卖平台对接控制器
 * 提供外卖订单管理、菜品同步、库存同步、状态流转、筛选选项、统计等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/delivery")
@Validated
@Tag(name = "外卖平台对接")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    // ==================== 订单查询 ====================

    @GetMapping("/orders/{id}")
    @Operation(summary = "查询外卖订单详情", description = "根据主键ID查询配送订单完整信息")
    public R<DeliveryOrder> getOrderDetail(@PathVariable Long id) {
        DeliveryOrder order = deliveryService.getById(String.valueOf(id));
        if (order == null) {
            return R.error("订单不存在");
        }
        return R.success(order);
    }

    @GetMapping("/orders")
    @Operation(summary = "分页查询外卖订单", description = "分页查询外卖平台订单，支持按平台、状态、时间范围筛选")
    public R<Page<DeliveryOrder>> pageOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Page<DeliveryOrder> pageInfo = deliveryService.pageOrders(page, pageSize, platform, status, startDate, endDate);
        return R.success(pageInfo);
    }

    // ==================== 订单操作 ====================

    @PostMapping("/accept")
    @Operation(summary = "接单", description = "确认接单外卖订单（PENDING → ACCEPTED）")
    public R<String> acceptOrder(@Valid @RequestBody AcceptOrderDTO dto) {
        boolean result = deliveryService.acceptOrder(dto.getPlatform(), dto.getPlatformOrderId());
        return result ? R.success("接单成功") : R.error("接单失败");
    }

    @PutMapping("/status")
    @Operation(summary = "更新配送状态", description = "更新配送订单状态，支持完整生命周期：接单→取餐→配送→送达→取消")
    public R<String> updateStatus(
            @RequestParam @NotNull(message = "订单ID不能为空") Long id,
            @RequestParam @NotBlank(message = "目标状态不能为空") String status,
            @RequestParam(required = false) String remark) {
        boolean result = deliveryService.updateOrderStatus(id, status, remark);
        return result ? R.success("状态更新成功") : R.error("状态更新失败");
    }

    // ==================== 筛选选项与统计 ====================

    @GetMapping("/options")
    @Operation(summary = "获取筛选选项", description = "返回平台列表和状态选项，供前端下拉框使用")
    public R<Map<String, Object>> getFilterOptions(
            @RequestParam(required = false) String platform) {
        Map<String, Object> options = deliveryService.getFilterOptions(platform);
        return R.success(options);
    }

    @GetMapping("/stats")
    @Operation(summary = "获取配送统计", description = "获取今日订单数、各状态数量、金额汇总等配送统计数据")
    public R<Map<String, Object>> getStats(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> stats = deliveryService.getDeliveryStats(platform, startDate, endDate);
        return R.success(stats);
    }

    // ==================== 平台同步 ====================

    @PostMapping("/sync/menu")
    @Operation(summary = "同步菜品", description = "同步菜单到外卖平台")
    public R<String> syncMenu(@Valid @RequestBody SyncMenuDTO dto) {
        boolean result = deliveryService.syncMenu(dto.getPlatform(), dto.getDishes());
        return result ? R.success("菜单同步成功") : R.error("菜单同步失败");
    }

    @PostMapping("/sync/stock")
    @Operation(summary = "同步库存", description = "同步库存到外卖平台")
    public R<String> syncStock(@Valid @RequestBody SyncStockDTO dto) {
        boolean result = deliveryService.syncStock(dto.getPlatform(), dto.getStock());
        return result ? R.success("库存同步成功") : R.error("库存同步失败");
    }

    // ==================== 平台回调 ====================

    @PostMapping("/callback/{platform}")
    @Operation(summary = "平台回调", description = "接收外卖平台回调：新订单通知、状态变更、取消通知")
    public R<String> callback(@PathVariable String platform, @RequestBody Map<String, String> params) {
        String result = deliveryService.handleCallback(platform, params);
        return "success".equals(result) ? R.success(result) : R.error(result);
    }
}
