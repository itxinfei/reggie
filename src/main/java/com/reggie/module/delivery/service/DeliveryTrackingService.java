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
     * 原子调整骑手在途单量（避免读-改-写并发丢失更新）。
     * <p>delta=+1 接单：在途 +1 并置忙碌；delta=-1 送达：在途 -1（不小于 0）、
     * 累计单量 +1，在途归零且仍忙碌时回到在线。</p>
     *
     * @param riderId 骑手ID
     * @param delta   负载变化（+1 / -1）
     * @return 调整后的在途单量
     */
    int adjustRiderLoad(Long riderId, int delta);

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

    /** 骑手动作：接单 */
    String ACTION_ACCEPT = "ACCEPT";
    /** 骑手动作：取餐 */
    String ACTION_PICKUP = "PICKUP";
    /** 骑手动作：送达 */
    String ACTION_DELIVER = "DELIVER";

    /**
     * 记录骑手动作时间戳（接单/取餐/送达），按 orderId upsert delivery_time_record。
     * <p>
     * 供订单状态流调用：不存在记录则新建并绑定骑手，存在则补对应时间；
     * 送达时回填实际耗时 actualMinutes。
     * </p>
     *
     * @param orderId     订单ID
     * @param orderNumber 订单号
     * @param orderTime   下单时间
     * @param riderId     骑手ID
     * @param riderName   骑手姓名
     * @param action      动作：{@link #ACTION_ACCEPT} / {@link #ACTION_PICKUP} / {@link #ACTION_DELIVER}
     * @return 是否成功
     */
    boolean recordRiderAction(Long orderId, String orderNumber, LocalDateTime orderTime,
                              Long riderId, String riderName, String action);

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
