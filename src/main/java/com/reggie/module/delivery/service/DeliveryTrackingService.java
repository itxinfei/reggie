package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
     * 后台骑手分页查询（姓名/手机号模糊、状态精确，租户隔离）。
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param name     姓名（可选）
     * @param phone    手机号（可选）
     * @param status   状态（可选）：0-离线 1-在线 2-忙碌
     * @param tenantId 租户ID
     * @return 骑手分页
     */
    Page<Rider> pageRiders(int page, int pageSize, String name, String phone, Integer status, Long tenantId);

    /**
     * 后台新增骑手：BCrypt 加密初始密码，同门店手机号唯一校验。
     *
     * @param name        姓名
     * @param phone       手机号
     * @param rawPassword 初始明文密码（6-20 位）
     * @return 新建骑手（不含密码）
     */
    Rider createRider(String name, String phone, String rawPassword);

    /**
     * 后台编辑骑手资料（不含密码、不改统计/定位字段）。
     *
     * @param id     骑手ID
     * @param name   姓名
     * @param phone  手机号
     * @param avatar 头像（可选）
     * @return 更新后的骑手（不含密码）
     */
    Rider updateRiderProfile(Long id, String name, String phone, String avatar);

    /**
     * 后台重置骑手密码。
     *
     * @param id             骑手ID
     * @param newRawPassword 新明文密码（6-20 位）
     * @return 是否成功
     */
    boolean resetRiderPassword(Long id, String newRawPassword);

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
