package com.reggie.module.delivery.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.dto.AcceptOrderDTO;
import com.reggie.dto.SyncMenuDTO;
import com.reggie.dto.SyncStockDTO;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.address.service.AddressBookService;
import com.reggie.module.delivery.model.DeliveryOrder;
import com.reggie.module.delivery.model.DeliveryTimeRecord;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.service.DeliveryService;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private AddressBookService addressBookService;

    // ==================== 订单查询 ====================

    /**
     * 查询外卖订单详情
     * @param id 订单主键ID
     * @return 配送订单详情
     */
    @GetMapping("/orders/{id}")
    @RequireEmployee
    @Operation(summary = "查询外卖订单详情", description = "根据主键ID查询配送订单完整信息")
    public R<DeliveryOrder> getOrderDetail(@Parameter(description = "配送订单主键ID", required =
            true) @PathVariable Long id) {
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
        Page<DeliveryOrder> pageInfo = deliveryService.pageOrders(page, PageUtils.cap(pageSize), platform, status,
                startDate, endDate);
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
    public R<String> acceptOrder(@Parameter(description = "接单请求（平台、平台订单号）", required =
            true) @Valid @RequestBody AcceptOrderDTO dto) {
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
            @Parameter(description = "目标状态", required = true) @RequestParam @NotBlank(message =
                    "目标状态不能为空") String status,
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
    public R<String> syncMenu(@Parameter(description = "菜品同步请求（平台、菜品列表）", required =
            true) @Valid @RequestBody SyncMenuDTO dto) {
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
    public R<String> syncStock(@Parameter(description = "库存同步请求（平台、库存列表）", required =
            true) @Valid @RequestBody SyncStockDTO dto) {
        boolean result = deliveryService.syncStock(dto.getPlatform(), dto.getStock());
        return result ? R.success("库存同步成功") : R.error("库存同步失败");
    }

    // ==================== 配送追踪 ====================

    /**
     * 查询配送状态（供前端配送追踪页面使用）。
     * <p>支持两种入参：平台单号（第三方平台配送单）或本地订单ID（自有骑手配送单），
     * 平台单查不到时自动回退本地订单查询。</p>
     *
     * @param orderId 平台订单号或本地订单ID
     * @return 配送订单展示信息（含骑手字段）
     */
    @GetMapping("/tracking/{orderId}")
    @Operation(summary = "查询配送追踪", description = "根据平台订单号或本地订单ID查询配送详情，供前端追踪页面使用")
    public R<Map<String, Object>> tracking(@Parameter(description = "平台订单号或本地订单ID", required =
            true) @PathVariable String orderId) {
        DeliveryOrder platformOrder = deliveryService.getByPlatformOrderId(orderId);
        if (platformOrder != null) {
            return R.success(buildPlatformTracking(platformOrder));
        }
        // 平台单查不到：按本地自有骑手订单 ID 查询
        Long localId = parseLongId(orderId);
        if (localId == null) {
            return R.error("配送订单不存在");
        }
        Orders localOrder = orderService.getById(localId);
        // 堂食单（tableId != null）或无收货地址的订单不属于配送追踪范围
        if (localOrder == null || localOrder.getTableId() != null
                || localOrder.getAddressBookId() == null) {
            return R.error("配送订单不存在");
        }
        // IDOR 归属校验：仅下单用户本人可查自己的配送单
        if (!Objects.equals(localOrder.getUserId(), BaseContext.getCurrentId())) {
            return R.error("无权查看该订单");
        }
        return R.success(buildLocalTracking(localOrder));
    }

    /**
     * 组装第三方平台配送单的追踪数据。
     * <p>仅返回展示所需字段，并校验当前登录用户是否归属该订单（防止枚举 platformOrderId 获取他人 PII）。</p>
     */
    private Map<String, Object> buildPlatformTracking(DeliveryOrder order) {
        // 归属校验：防止同租户下任意登录用户通过枚举 platformOrderId 获取他人顾客 PII（IDOR）
        if (order.getPhone() != null) {
            User user = userService.getById(BaseContext.getCurrentId());
            if (user != null && user.getPhone() != null && !user.getPhone().equals(order.getPhone())) {
                throw new CustomException("无权查看该订单");
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("platform", order.getPlatform());
        data.put("platformOrderId", order.getPlatformOrderId());
        data.put("dishSummary", order.getDishSummary());
        data.put("amount", order.getAmount());
        data.put("status", order.getStatus());
        data.put("orderTime", order.getOrderTime());
        data.put("updateTime", order.getUpdateTime());
        data.put("address", order.getAddress());
        // 尽力而为补充骑手信息：仅当本地订单有关联配送时效记录时才有真实骑手数据
        if (order.getOrderId() != null) {
            DeliveryTimeRecord record = deliveryTrackingService.getDeliveryTimeByOrderId(order.getOrderId());
            if (record != null) {
                data.put("riderName", record.getRiderName());
                Rider rider = record.getRiderId() == null ? null
                        : deliveryTrackingService.getById(record.getRiderId());
                if (rider != null) {
                    data.put("riderPhone", rider.getPhone());
                }
            }
        }
        return data;
    }

    /**
     * 组装自有骑手配送单的追踪数据（结构与平台单同构，tracking.html 无需区分来源）。
     * <p>状态映射：本地 2待接单→PENDING；3配送中按是否已取餐→ACCEPTED/DELIVERING；
     * 4已完成→DELIVERED；5已取消/6已退款→CANCELLED。</p>
     */
    private Map<String, Object> buildLocalTracking(Orders order) {
        DeliveryTimeRecord record = deliveryTrackingService.getDeliveryTimeByOrderId(order.getId());
        String trackingStatus = mapLocalTrackingStatus(order.getStatus(), record);

        Map<String, Object> data = new HashMap<>();
        data.put("dishSummary", buildDishSummary(order.getId()));
        data.put("amount", order.getAmount());
        data.put("status", trackingStatus);
        data.put("orderTime", order.getOrderTime());
        data.put("updateTime", resolveCurrentStepTime(trackingStatus, order, record));
        data.put("address", order.getAddress());

        // 骑手信息与实时位置
        Long riderId = order.getRiderId();
        Rider rider = riderId == null ? null : deliveryTrackingService.getRiderById(riderId);
        if (rider != null) {
            data.put("riderName", rider.getName());
            data.put("riderPhone", rider.getPhone());
            Map<String, Object> riderLocation = new HashMap<>();
            riderLocation.put("longitude", rider.getCurrentLongitude());
            riderLocation.put("latitude", rider.getCurrentLatitude());
            riderLocation.put("lastUpdate", rider.getLastLocationTime());
            data.put("riderLocation", riderLocation);

            // 骑手与收货点的实时距离（km）
            AddressBook addressBook = addressBookService.getById(order.getAddressBookId());
            BigDecimal distanceKm = haversineKm(rider.getCurrentLongitude(), rider.getCurrentLatitude(),
                    addressBook == null ? null : addressBook.getLongitude(),
                    addressBook == null ? null : addressBook.getLatitude());
            if (distanceKm != null) {
                data.put("distance", distanceKm);
            }
        }

        // 预计送达分钟：时效记录已有则直接用，否则按距离估算
        Integer estimatedMinutes = record == null ? null : record.getEstimatedMinutes();
        if (estimatedMinutes == null && data.get("distance") != null) {
            BigDecimal distanceMeters = ((BigDecimal) data.get("distance"))
                    .multiply(new BigDecimal("1000"));
            estimatedMinutes = deliveryTrackingService.estimateDeliveryTime(distanceMeters, riderId);
        }
        if (estimatedMinutes != null) {
            data.put("estimatedMinutes", estimatedMinutes);
        }
        return data;
    }

    /**
     * 本地订单状态 → tracking.html 状态字符串。
     */
    private String mapLocalTrackingStatus(Integer status, DeliveryTimeRecord record) {
        if (status == null) {
            return "PENDING";
        }
        switch (status) {
            case Orders.STATUS_ORDERED:
                return "PENDING";
            case Orders.STATUS_DELIVERING:
                // 骑手已确认接单但未取餐→ACCEPTED；已取餐→DELIVERING
                return record != null && record.getPickupTime() != null ? "DELIVERING" : "ACCEPTED";
            case Orders.STATUS_COMPLETED:
                return "DELIVERED";
            case Orders.STATUS_CANCELLED:
            case Orders.STATUS_REFUNDED:
                return "CANCELLED";
            default:
                return "PENDING";
        }
    }

    /**
     * 当前时间线节点对应的时间（tracking.html 只展示当前节点时间，不虚构历史节点）。
     */
    private LocalDateTime resolveCurrentStepTime(String trackingStatus, Orders order,
                                                 DeliveryTimeRecord record) {
        switch (trackingStatus) {
            case "ACCEPTED":
                if (record != null && record.getAcceptTime() != null) {
                    return record.getAcceptTime();
                }
                return order.getOrderTime();
            case "DELIVERING":
                return record == null ? null : record.getPickupTime();
            case "DELIVERED":
                if (record != null && record.getDeliverTime() != null) {
                    return record.getDeliverTime();
                }
                return record == null ? order.getCheckoutTime() : order.getUpdateTime();
            case "CANCELLED":
                return order.getUpdateTime();
            default:
                return order.getOrderTime();
        }
    }

    /**
     * 组装商品摘要：首件商品名 + 件数（单件即商品名）。
     */
    private String buildDishSummary(Long orderId) {
        List<OrderDetail> details = orderDetailService.list(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OrderDetail>()
                        .eq(OrderDetail::getOrderId, orderId));
        if (details.isEmpty()) {
            return null;
        }
        int totalCount = 0;
        for (OrderDetail detail : details) {
            totalCount += detail.getNumber() == null ? 1 : detail.getNumber();
        }
        String firstName = details.get(0).getName();
        if (details.size() == 1 && totalCount == 1) {
            return firstName;
        }
        return firstName + " 等" + totalCount + "件商品";
    }

    /**
     * Haversine 公式计算两点间距离（km，保留 2 位小数）；任一点坐标缺失返回 null。
     */
    private BigDecimal haversineKm(BigDecimal lng1, BigDecimal lat1, BigDecimal lng2, BigDecimal lat2) {
        if (lng1 == null || lat1 == null || lng2 == null || lat2 == null) {
            return null;
        }
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double km = earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return new BigDecimal(km).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 安全解析 Long（本地订单ID）；非数字返回 null。
     */
    private Long parseLongId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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



