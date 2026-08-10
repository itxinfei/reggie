package com.reggie.module.delivery.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.model.RiderLocationRecord;
import com.reggie.module.delivery.model.DeliveryTimeRecord;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

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
@Slf4j
@RestController
@RequestMapping("/delivery/tracking")
@Tag(name = "Delivery Tracking Management")
public class DeliveryTrackingController {

    @Autowired
    private DeliveryTrackingService deliveryTrackingService;

    // ==================== Rider Management ====================

    @GetMapping("/rider/list")
    @Operation(summary = "Get rider list")
    public R<List<Rider>> getRiderList(
            @Parameter(description = "S t a t u s")
            @Parameter(description = "Status") @RequestParam(required = false) Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Rider> list = deliveryTrackingService.getRiderList(status, tenantId);
        return R.success(list);
    }

    @GetMapping("/rider/{id}")
    @Operation(summary = "Get rider by ID")
    @Parameter(description = "I d")
    public R<Rider> getRiderById(@PathVariable Long id) {
        Rider rider = deliveryTrackingService.getRiderById(id);
        return R.success(rider);
    }

    @PostMapping("/rider")
    @Operation(summary = "Save rider")
    public R<String> saveRider(@RequestBody Rider rider) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rider.setTenantId(tenantId);
        boolean success = deliveryTrackingService.saveOrUpdateRider(rider);
        return success ? R.success("Saved successfully") : R.error("Save failed");
    }

    @PutMapping("/rider")
    @Operation(summary = "Update rider")
    public R<String> updateRider(@RequestBody Rider rider) {
        Long tenantId = BaseContext.getCurrentTenantId();
        rider.setTenantId(tenantId);
        boolean success = deliveryTrackingService.saveOrUpdateRider(rider);
        return success ? R.success("Updated successfully") : R.error("Update failed");
    }

    @DeleteMapping("/rider/{id}")
    @Operation(summary = "Delete rider")
    @Parameter(description = "I d")
    public R<String> deleteRider(@PathVariable Long id) {
        boolean success = deliveryTrackingService.deleteRider(id);
        return success ? R.success("Deleted successfully") : R.error("Delete failed");
    }

    @PostMapping("/rider/{id}/status")
    @Operation(summary = "Update rider status")
    public R<String> updateRiderStatus(
            @Parameter(description = "ID")
            @PathVariable Long id,
            @Parameter(description = "Status") @RequestParam Integer status) {
        boolean success = deliveryTrackingService.updateRiderStatus(id, status);
        return success ? R.success("Status updated") : R.error("Update failed");
    }

    // ==================== Location Tracking ====================

    @PostMapping("/location/update")
    @Operation(summary = "Update rider location")
    public R<String> updateRiderLocation(
            @Parameter(description = "R i d e r I d")
            @Parameter(description = "Rider ID") @RequestParam Long riderId,
            @Parameter(description = "Longitude") @RequestParam BigDecimal longitude,
            @Parameter(description = "Latitude") @RequestParam BigDecimal latitude,
            @Parameter(description = "Speed") @RequestParam(required = false) BigDecimal speed,
            @Parameter(description = "Direction") @RequestParam(required = false) BigDecimal direction) {
        boolean success = deliveryTrackingService.updateRiderLocation(riderId, longitude, latitude, speed, direction);
        return success ? R.success("Location updated") : R.error("Update failed");
    }

    @GetMapping("/location/history")
    @Operation(summary = "Get rider location history")
    public R<List<RiderLocationRecord>> getRiderLocationHistory(
            @Parameter(description = "R i d e r I d")
            @Parameter(description = "Rider ID") @RequestParam Long riderId,
            @Parameter(description = "Start time") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "End time") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        List<RiderLocationRecord> history = deliveryTrackingService.getRiderLocationHistory(riderId, startTime, endTime);
        return R.success(history);
    }

    @GetMapping("/tracking/order/{orderId}")
    @Operation(summary = "Get order delivery tracking")
    @Parameter(description = "O r d e r I d")
    public R<Map<String, Object>> getOrderDeliveryTracking(@PathVariable Long orderId) {
        Map<String, Object> tracking = deliveryTrackingService.getOrderDeliveryTracking(orderId);
        return R.success(tracking);
    }

    @GetMapping("/tracking/rider/{riderId}")
    @Operation(summary = "Get rider current location")
    @Parameter(description = "R i d e r I d")
    public R<Map<String, Object>> getRiderCurrentLocation(@PathVariable Long riderId) {
        Map<String, Object> location = deliveryTrackingService.getRiderCurrentLocation(riderId);
        return R.success(location);
    }

    // ==================== Delivery Time Management ====================

    @PostMapping("/time/record")
    @Operation(summary = "Create delivery time record")
    public R<String> createDeliveryTimeRecord(@RequestBody DeliveryTimeRecord record) {
        Long tenantId = BaseContext.getCurrentTenantId();
        record.setTenantId(tenantId);
        boolean success = deliveryTrackingService.createDeliveryTimeRecord(record);
        return success ? R.success("Record created") : R.error("Creation failed");
    }

    @PutMapping("/time/record")
    @Operation(summary = "Update delivery time record")
    public R<String> updateDeliveryTimeRecord(@RequestBody DeliveryTimeRecord record) {
        boolean success = deliveryTrackingService.updateDeliveryTimeRecord(record);
        return success ? R.success("Record updated") : R.error("Update failed");
    }

    @GetMapping("/time/order/{orderId}")
    @Operation(summary = "Get delivery time by order ID")
    @Parameter(description = "O r d e r I d")
    public R<DeliveryTimeRecord> getDeliveryTimeByOrderId(@PathVariable Long orderId) {
        DeliveryTimeRecord record = deliveryTrackingService.getDeliveryTimeByOrderId(orderId);
        return R.success(record);
    }

    @PostMapping("/time/estimate")
    @Operation(summary = "Estimate delivery time")
    public R<Integer> estimateDeliveryTime(
            @Parameter(description = "D i s t a n c e")
            @Parameter(description = "Distance (meters)") @RequestParam BigDecimal distance,
            @Parameter(description = "Rider ID") @RequestParam(required = false) Long riderId) {
        int minutes = deliveryTrackingService.estimateDeliveryTime(distance, riderId);
        return R.success(minutes);
    }

    @GetMapping("/time/statistics")
    @Operation(summary = "Get delivery time statistics")
    public R<Map<String, Object>> getDeliveryTimeStatistics(
            @Parameter(description = "S t a r t D a t e")
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = deliveryTrackingService.getDeliveryTimeStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    // ==================== Statistics ====================

    @GetMapping("/rider/{riderId}/statistics")
    @Operation(summary = "Get rider statistics")
    public R<Map<String, Object>> getRiderStatistics(
            @Parameter(description = "riderId")
            @PathVariable Long riderId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Map<String, Object> statistics = deliveryTrackingService.getRiderStatistics(riderId, startDate, endDate);
        return R.success(statistics);
    }

    @GetMapping("/overview")
    @Operation(summary = "Get delivery overview")
    public R<Map<String, Object>> getDeliveryOverview() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> overview = deliveryTrackingService.getDeliveryOverview(tenantId);
        return R.success(overview);
    }
}


