package com.reggie.module.delivery.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.dto.AcceptOrderDTO;
import com.reggie.dto.SyncMenuDTO;
import com.reggie.dto.SyncStockDTO;
import com.reggie.module.delivery.model.DeliveryOrder;
import com.reggie.module.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.Map;

/**
 * 外卖平台对接控制器
 * 提供外卖订单管理、菜品同步、库存同步、状态流转、筛选选项、统计等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/api/delivery")
@Validated
@Tag(name = "外卖平台对接")
public class DeliveryController {

    @Autowired
    private DeliveryService deliveryService;

    // ==================== 订单查询 ====================

    /**
     * 查询外卖订单详情
     * @param id 订单主键ID
     * @return 配送订单详情
     */
    @GetMapping("/orders/{id}")
    @RequireEmployee
    @Operation(summary = "查询外卖订单详情", description = "根据主键ID查询配送订单完整信息")
    public R<DeliveryOrder> getOrderDetail(@PathVariable Long id) {
        DeliveryOrder order = deliveryService.getById(String.valueOf(id));
        if (order == null) {
            return R.error("订单不存在");
        }
        return R.success(order);
    }

    /**
     * 分页查询外卖订单
     * @param page 页码
     * @param pageSize 每页数量
     * @param platform 外卖平台（可选）
     * @param status 订单状态（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 分页结果
     */
    @GetMapping("/orders")
    @RequireEmployee
    @Operation(summary = "分页查询外卖订单", description = "分页查询外卖平台订单，支持按平台、状态、时间范围筛选")
    public R<Page<DeliveryOrder>> pageOrders(
                        @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "外卖平台（可选）") @RequestParam(required = false) String platform,
            @Parameter(description = "状态（可选）") @RequestParam(required = false) String status,
            @Parameter(description = "开始日期（可选）") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) String endDate) {
        Page<DeliveryOrder> pageInfo = deliveryService.pageOrders(page, PageUtils.cap(pageSize), platform, status, startDate, endDate);
        return R.success(pageInfo);
    }

    // ==================== 订单操作 ====================

    /**
     * 接单
     * @param dto 接单请求
     * @return 操作结果
     */
    @PostMapping("/accept")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "接单", description = "确认接单外卖订单（PENDING → ACCEPTED）")
    public R<String> acceptOrder(@Valid @RequestBody AcceptOrderDTO dto) {
        boolean result = deliveryService.acceptOrder(dto.getPlatform(), dto.getPlatformOrderId());
        return result ? R.success("接单成功") : R.error("接单失败");
    }

    /**
     * 更新配送状态
     * @param id 订单ID
     * @param status 目标状态
     * @param remark 备注（可选）
     * @return 操作结果
     */
    @PutMapping("/status")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "更新配送状态", description = "更新配送订单状态，支持完整生命周期：接单->取餐->配送->送达->取消")
    public R<String> updateStatus(
            @Parameter(description = "订单ID", required = true) @RequestParam @NotNull(message = "订单ID不能为空") Long id,
            @Parameter(description = "目标状态", required = true) @RequestParam @NotBlank(message = "目标状态不能为空") String status,
            @Parameter(description = "备注（可选）") @RequestParam(required = false) String remark) {
        boolean result = deliveryService.updateOrderStatus(id, status, remark);
        return result ? R.success("状态更新成功") : R.error("状态更新失败");
    }

    // ==================== 筛选选项与统计 ====================

    /**
     * 获取筛选选项
     * @param platform 外卖平台（可选）
     * @return 平台列表和状态选项
     */
    @GetMapping("/options")
    @RequireEmployee
    @Operation(summary = "筛选选项", description = "返回平台列表和状态选项，供前端下拉框使用")
    public R<Map<String, Object>> getFilterOptions(
                        @Parameter(description = "外卖平台（可选）") @RequestParam(required = false) String platform) {
        Map<String, Object> options = deliveryService.getFilterOptions(platform);
        return R.success(options);
    }

    /**
     * 配送统计
     * @param platform 外卖平台（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @return 配送统计数据
     */
    @GetMapping("/stats")
    @RequireEmployee
    @Operation(summary = "配送统计", description = "获取今日订单数、各状态数量、金额汇总等配送统计数据")
    public R<Map<String, Object>> getStats(
                        @Parameter(description = "外卖平台（可选）") @RequestParam(required = false) String platform,
            @Parameter(description = "开始日期（可选）") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) String endDate) {
        Map<String, Object> stats = deliveryService.getDeliveryStats(platform, startDate, endDate);
        return R.success(stats);
    }

    // ==================== 平台同步 ====================

    /**
     * 同步菜品到外卖平台
     * @param dto 菜品同步请求
     * @return 操作结果
     */
    @PostMapping("/sync/menu")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "同步菜品", description = "同步菜单到外卖平台")
    public R<String> syncMenu(@Valid @RequestBody SyncMenuDTO dto) {
        boolean result = deliveryService.syncMenu(dto.getPlatform(), dto.getDishes());
        return result ? R.success("菜单同步成功") : R.error("菜单同步失败");
    }

    /**
     * 同步库存到外卖平台
     * @param dto 库存同步请求
     * @return 操作结果
     */
    @PostMapping("/sync/stock")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "同步库存", description = "同步库存到外卖平台")
    public R<String> syncStock(@Valid @RequestBody SyncStockDTO dto) {
        boolean result = deliveryService.syncStock(dto.getPlatform(), dto.getStock());
        return result ? R.success("库存同步成功") : R.error("库存同步失败");
    }

    // ==================== 配送追踪 ====================

    /**
     * 根据平台订单号查询配送状态（供前端配送追踪页面使用）
     *
     * @param orderId 平台订单号
     * @return 配送订单详情
     */
    @GetMapping("/tracking/{orderId}")
    @Operation(summary = "查询配送追踪", description = "根据平台订单号查询配送订单详情，供前端追踪页面使用")
    @Parameter(description = "OrderId")
    public R<DeliveryOrder> tracking(@PathVariable String orderId) {
        DeliveryOrder order = deliveryService.getByPlatformOrderId(orderId);
        if (order == null) {
            return R.error("配送订单不存在");
        }
        return R.success(order);
    }

    // ==================== 平台回调 ====================

    /**
     * 接收外卖平台回调通知
     * @param platform 外卖平台标识
     * @param params 回调参数
     * @return 处理结果
     */
    @PostMapping("/callback/{platform}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "平台回调通知", description = "接收外卖平台回调：新订单通知、状态变更、取消通知")
    public R<String> callback(
                        @Parameter(description = "外卖平台标识", required = true) @PathVariable String platform,
            @Parameter(description = "回调参数") @RequestBody Map<String, String> params) {
        String result = deliveryService.handleCallback(platform, params);
        return "success".equals(result) ? R.success(result) : R.error(result);
    }
}



