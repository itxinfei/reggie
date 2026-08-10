package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.model.RiderLocationRecord;
import com.reggie.module.delivery.model.DeliveryTimeRecord;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Delivery Tracking Service Interface
 * 
 * @author reggie
 * @since 2026-08-11
 */
public interface DeliveryTrackingService extends IService<Rider> {

    // ==================== Rider Management ====================

    /**
     * Get rider list
     *
     * @param status   Status filter
     * @param tenantId Tenant ID
     * @return Rider list
     */
    List<Rider> getRiderList(Integer status, Long tenantId);

    /**
     * Get rider by ID
     *
     * @param id Rider ID
     * @return Rider
     */
    Rider getRiderById(Long id);

    /**
     * Save or update rider
     *
     * @param rider Rider
     * @return Success or not
     */
    boolean saveOrUpdateRider(Rider rider);

    /**
     * Delete rider
     *
     * @param id Rider ID
     * @return Success or not
     */
    boolean deleteRider(Long id);

    /**
     * Update rider status
     *
     * @param id     Rider ID
     * @param status New status
     * @return Success or not
     */
    boolean updateRiderStatus(Long id, Integer status);

    // ==================== Location Tracking ====================

    /**
     * Update rider location
     *
     * @param riderId   Rider ID
     * @param longitude Longitude
     * @param latitude  Latitude
     * @param speed     Speed
     * @param direction Direction
     * @return Success or not
     */
    boolean updateRiderLocation(Long riderId, BigDecimal longitude, BigDecimal latitude, 
                                BigDecimal speed, BigDecimal direction);

    /**
     * Get rider location history
     *
     * @param riderId   Rider ID
     * @param startTime Start time
     * @param endTime   End time
     * @return Location records
     */
    List<RiderLocationRecord> getRiderLocationHistory(Long riderId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Get order delivery tracking
     *
     * @param orderId Order ID
     * @return Tracking info
     */
    Map<String, Object> getOrderDeliveryTracking(Long orderId);

    /**
     * Get rider current location
     *
     * @param riderId Rider ID
     * @return Current location
     */
    Map<String, Object> getRiderCurrentLocation(Long riderId);

    // ==================== Delivery Time Management ====================

    /**
     * Create delivery time record
     *
     * @param record Delivery time record
     * @return Success or not
     */
    boolean createDeliveryTimeRecord(DeliveryTimeRecord record);

    /**
     * Update delivery time record
     *
     * @param record Delivery time record
     * @return Success or not
     */
    boolean updateDeliveryTimeRecord(DeliveryTimeRecord record);

    /**
     * Get delivery time record by order ID
     *
     * @param orderId Order ID
     * @return Delivery time record
     */
    DeliveryTimeRecord getDeliveryTimeByOrderId(Long orderId);

    /**
     * Estimate delivery time
     *
     * @param distance Distance (meters)
     * @param riderId  Rider ID (optional)
     * @return Estimated minutes
     */
    int estimateDeliveryTime(BigDecimal distance, Long riderId);

    /**
     * Get delivery time statistics
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Statistics
     */
    Map<String, Object> getDeliveryTimeStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    // ==================== Statistics ====================

    /**
     * Get rider statistics
     *
     * @param riderId  Rider ID
     * @param startDate Start date
     * @param endDate   End date
     * @return Statistics
     */
    Map<String, Object> getRiderStatistics(Long riderId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get delivery overview
     *
     * @param tenantId Tenant ID
     * @return Overview
     */
    Map<String, Object> getDeliveryOverview(Long tenantId);
}
