package com.reggie.module.delivery.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.model.RiderLocationRecord;
import com.reggie.module.delivery.model.DeliveryTimeRecord;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Delivery Tracking Controller
 * 
 * @author reggie
 * @since 2026-08-11
 */
@RestController
@RequestMapping("/delivery/tracking")
@Tag(name = "配送跟踪管理")
@RequireEmployee
public class DeliveryTrackingController {

    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    // ==================== Rider Management ====================

    /**
     * 获取 rider list。
     * @param status 参数 status
     * @return 返回结果
     */
    @GetMapping("/rider/list")
    @Operation(summary = "骑手列表")
    public R<List<Rider>> getRiderList(
                        @Parameter(description = "状态（可选）：1-空闲，2-配送中") @RequestParam(required = false) Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Rider> list = deliveryTrackingService.getRiderList(status, tenantId);
        return R.success(list);
    }

    /**
     * 获取 rider by id。
     * @param id 参数 id
     * @return 返回结果
     */
    @GetMapping("/rider/{id}")
    @Operation(summary = "查询骑手详情")
    public R<Rider> getRiderById(@Parameter(description = "骑手ID", required = true) @PathVariable Long id) {
        Rider rider = deliveryTrackingService.getRiderById(id);
        return R.success(rider);
    }

    /**
     * 保存 rider。
     * @param rider 参数 rider
     * @return 返回结果
     */
    @PostMapping("/rider")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增骑手")
    public R<String> saveRider(@Parameter(description = "骑手信息", required = true) @RequestBody Rider rider) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rider.setTenantId(tenantId);
        boolean success = deliveryTrackingService.saveOrUpdateRider(rider);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    /**
     * 更新 rider。
     * @param rider 参数 rider
     * @return 返回结果
     */
    @PutMapping("/rider")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改骑手")
    public R<String> updateRider(@Parameter(description = "骑手信息（含ID）", required = true) @RequestBody Rider rider) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rider.setTenantId(tenantId);
        boolean success = deliveryTrackingService.saveOrUpdateRider(rider);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    /**
     * 删除 rider。
     * @param id 参数 id
     * @return 返回结果
     */
    @DeleteMapping("/rider/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除骑手")
    public R<String> deleteRider(@Parameter(description = "骑手ID", required = true) @PathVariable Long id) {
        boolean success = deliveryTrackingService.deleteRider(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    /**
     * 更新 rider status。
     * @param id 参数 id
     * @param status 参数 status
     * @return 返回结果
     */
    @PostMapping("/rider/{id}/status")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "更新骑手状态")
    public R<String> updateRiderStatus(
            @Parameter(description = "骑手ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "状态", required = true) @RequestParam Integer status) {
        boolean success = deliveryTrackingService.updateRiderStatus(id, status);
        return success ? R.success("Status updated") : R.error("Update failed");
    }

    // ==================== Location Tracking ====================

    /**
     * 更新 rider location。
     * @param riderId 参数 riderId
     * @param longitude 参数 longitude
     * @param latitude 参数 latitude
     * @param speed 参数 speed
     * @param direction 参数 direction
     * @return 返回结果
     */
    @PostMapping("/location/update")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "上报骑手位置")
    public R<String> updateRiderLocation(
                        @Parameter(description = "骑手ID", required = true) @RequestParam Long riderId,
            @Parameter(description = "经度", required = true) @RequestParam BigDecimal longitude,
            @Parameter(description = "纬度", required = true) @RequestParam BigDecimal latitude,
            @Parameter(description = "速度（可选）") @RequestParam(required = false) BigDecimal speed,
            @Parameter(description = "方向（可选）") @RequestParam(required = false) BigDecimal direction) {
        boolean success = deliveryTrackingService.updateRiderLocation(riderId, longitude, latitude, speed, direction);
        return success ? R.success("Location updated") : R.error("Update failed");
    }

    /**
     * 获取 rider location history。
     * @param riderId 参数 riderId
     * @param startTime 参数 startTime
     * @param endTime 参数 endTime
     * @return 返回结果
     */
    @GetMapping("/location/history")
    @Operation(summary = "查询骑手位置轨迹")
    public R<List<RiderLocationRecord>> getRiderLocationHistory(
                        @Parameter(description = "骑手ID", required = true) @RequestParam Long riderId,
            @Parameter(description = "开始时间", required = true) @RequestParam @DateTimeFormat(pattern =
                    "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "结束时间", required = true) @RequestParam @DateTimeFormat(pattern =
                    "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<RiderLocationRecord> history = deliveryTrackingService.getRiderLocationHistory(riderId, startTime,
                endTime);
        return R.success(history);
    }

    /**
     * 获取 order delivery tracking。
     * @param orderId 参数 orderId
     * @return 返回结果
     */
    @GetMapping("/tracking/order/{orderId}")
    @Operation(summary = "查询订单配送轨迹")
    public R<Map<String, Object>> getOrderDeliveryTracking(@Parameter(description = "订单ID", required =
            true) @PathVariable Long orderId) {
        Map<String, Object> tracking = deliveryTrackingService.getOrderDeliveryTracking(orderId);
        return R.success(tracking);
    }

    /**
     * 获取 rider current location。
     * @param riderId 参数 riderId
     * @return 返回结果
     */
    @GetMapping("/tracking/rider/{riderId}")
    @Operation(summary = "查询骑手当前位置")
    public R<Map<String, Object>> getRiderCurrentLocation(@Parameter(description = "骑手ID", required =
            true) @PathVariable Long riderId) {
        Map<String, Object> location = deliveryTrackingService.getRiderCurrentLocation(riderId);
        return R.success(location);
    }

    // ==================== Delivery Time Management ====================

    /**
     * 创建 delivery time record。
     * @param record 参数 record
     * @return 返回结果
     */
    @PostMapping("/time/record")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增配送时效记录")
    public R<String> createDeliveryTimeRecord(@Parameter(description = "配送时效记录信息", required =
            true) @RequestBody DeliveryTimeRecord record) {
        Long tenantId = BaseContext.getCurrentTenantId();
        record.setTenantId(tenantId);
        boolean success = deliveryTrackingService.createDeliveryTimeRecord(record);
        return success ? R.success("Record created") : R.error("Creation failed");
    }

    /**
     * 更新 delivery time record。
     * @param record 参数 record
     * @return 返回结果
     */
    @PutMapping("/time/record")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改配送时效记录")
    public R<String> updateDeliveryTimeRecord(@Parameter(description = "配送时效记录信息（含ID）", required =
            true) @RequestBody DeliveryTimeRecord record) {
        boolean success = deliveryTrackingService.updateDeliveryTimeRecord(record);
        return success ? R.success("Record updated") : R.error("Update failed");
    }

    /**
     * 获取 delivery time by order id。
     * @param orderId 参数 orderId
     * @return 返回结果
     */
    @GetMapping("/time/order/{orderId}")
    @Operation(summary = "按订单查询配送时效")
    public R<DeliveryTimeRecord> getDeliveryTimeByOrderId(@Parameter(description = "订单ID", required =
            true) @PathVariable Long orderId) {
        DeliveryTimeRecord record = deliveryTrackingService.getDeliveryTimeByOrderId(orderId);
        return R.success(record);
    }

    /**
     * 处理 estimate delivery time。
     * @param distance 参数 distance
     * @param riderId 参数 riderId
     * @return 返回结果
     */
    @PostMapping("/time/estimate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "预估配送时长")
    public R<Integer> estimateDeliveryTime(
                        @Parameter(description = "距离（米）", required = true) @RequestParam BigDecimal distance,
            @Parameter(description = "骑手ID（可选）") @RequestParam(required = false) Long riderId) {
        int minutes = deliveryTrackingService.estimateDeliveryTime(distance, riderId);
        return R.success(minutes);
    }

    /**
     * 获取 delivery time statistics。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/time/statistics")
    @Operation(summary = "配送时效统计")
    public R<Map<String, Object>> getDeliveryTimeStatistics(
                        @Parameter(description = "开始日期", required = true) @RequestParam @DateTimeFormat(pattern =
                                "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam @DateTimeFormat(pattern =
                    "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = deliveryTrackingService.getDeliveryTimeStatistics(startDate, endDate,
                tenantId);
        return R.success(statistics);
    }

    // ==================== Statistics ====================

    /**
     * 获取 rider statistics。
     * @param riderId 参数 riderId
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/rider/{riderId}/statistics")
    @Operation(summary = "骑手配送统计")
    public R<Map<String, Object>> getRiderStatistics(
            @Parameter(description = "骑手ID", required = true)
            @PathVariable Long riderId,
            @Parameter(description = "开始日期", required = true) @RequestParam @DateTimeFormat(pattern =
                    "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam @DateTimeFormat(pattern =
                    "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Map<String, Object> statistics = deliveryTrackingService.getRiderStatistics(riderId, startDate, endDate);
        return R.success(statistics);
    }

    /**
     * 获取 delivery overview。
     * @return 返回结果
     */
    @GetMapping("/overview")
    @Operation(summary = "配送总览")
    public R<Map<String, Object>> getDeliveryOverview() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> overview = deliveryTrackingService.getDeliveryOverview(tenantId);
        return R.success(overview);
    }
}



