package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.delivery.mapper.RiderMapper;
import com.reggie.module.delivery.mapper.RiderLocationRecordMapper;
import com.reggie.module.delivery.mapper.DeliveryTimeRecordMapper;
import com.reggie.module.delivery.model.Rider;
import com.reggie.module.delivery.model.RiderLocationRecord;
import com.reggie.module.delivery.model.DeliveryTimeRecord;
import com.reggie.module.delivery.service.DeliveryTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delivery Tracking Service Implementation
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class DeliveryTrackingServiceImpl extends ServiceImpl<RiderMapper, Rider> 
        implements DeliveryTrackingService {

    @Autowired
    private RiderMapper riderMapper;

    @Autowired
    private RiderLocationRecordMapper locationRecordMapper;

    @Autowired
    private DeliveryTimeRecordMapper timeRecordMapper;

    // ==================== Rider Management ====================

    /**
     * 获取 rider list。
     * @param status 参数 status
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<Rider> getRiderList(Integer status, Long tenantId) {
        LambdaQueryWrapper<Rider> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(Rider::getStatus, status);
        }
        if (tenantId != null) {
            qw.eq(Rider::getTenantId, tenantId);
        }
        qw.orderByDesc(Rider::getUpdateTime);
        return riderMapper.selectList(qw);
    }

    /**
     * 获取 rider by id。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    public Rider getRiderById(Long id) {
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            return riderMapper.selectById(id);
        }
        LambdaQueryWrapper<Rider> qw = new LambdaQueryWrapper<>();
        qw.eq(Rider::getId, id)
                .eq(Rider::getTenantId, currentTenantId);
        return riderMapper.selectOne(qw);
    }

    /**
     * 保存 or update rider。
     * @param rider 参数 rider
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateRider(Rider rider) {
        if (rider.getId() == null) {
            rider.setCreateTime(LocalDateTime.now());
            rider.setUpdateTime(LocalDateTime.now());
            rider.setCurrentOrderCount(0);
            rider.setTotalOrderCount(0);
            rider.setRating(new BigDecimal("5.0"));
            rider.setTenantId(BaseContext.getCurrentTenantId());
            return riderMapper.insert(rider) > 0;
        } else {
            // 租户归属校验：防止跨租户篡改骑手信息
            Long currentTenantId = BaseContext.getCurrentTenantId();
            Rider existing = riderMapper.selectById(rider.getId());
            if (existing == null) {
                return false;
            }
            if (currentTenantId != null && !currentTenantId.equals(existing.getTenantId())) {
                throw new CustomException("无权操作其他门店的骑手");
            }
            rider.setUpdateTime(LocalDateTime.now());
            // 保留原有租户ID，防止越权改写
            rider.setTenantId(existing.getTenantId());
            return riderMapper.updateById(rider) > 0;
        }
    }

    /**
     * 原子调整骑手在途单量。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int adjustRiderLoad(Long riderId, int delta) {
        if (riderId == null) {
            throw new CustomException("骑手ID不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        if (delta > 0) {
            // 在途原子自增，接单后必为忙碌（delta 为内部固定 ±1，非外部输入，setSql 无注入风险）
            riderMapper.update(null, new LambdaUpdateWrapper<Rider>()
                    .eq(Rider::getId, riderId)
                    .setSql("current_order_count = current_order_count + " + delta)
                    .set(Rider::getStatus, Rider.STATUS_BUSY)
                    .set(Rider::getUpdateTime, now));
        } else if (delta < 0) {
            int n = -delta;
            // 在途原子自减（GREATEST 兜底不为负）、累计单量同步增加
            riderMapper.update(null, new LambdaUpdateWrapper<Rider>()
                    .eq(Rider::getId, riderId)
                    .setSql("current_order_count = GREATEST(current_order_count - " + n + ", 0)")
                    .setSql("total_order_count = total_order_count + " + n)
                    .set(Rider::getUpdateTime, now));
            // 仅当在途真正归零且仍忙碌时回到在线（条件更新，并发送达时安全）
            riderMapper.update(null, new LambdaUpdateWrapper<Rider>()
                    .eq(Rider::getId, riderId)
                    .eq(Rider::getStatus, Rider.STATUS_BUSY)
                    .eq(Rider::getCurrentOrderCount, 0)
                    .set(Rider::getStatus, Rider.STATUS_ONLINE)
                    .set(Rider::getUpdateTime, now));
        }
        Rider latest = riderMapper.selectById(riderId);
        return latest != null && latest.getCurrentOrderCount() != null
                ? latest.getCurrentOrderCount() : 0;
    }

    /**
     * 删除 rider。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRider(Long id) {
        Long currentTenantId = BaseContext.getCurrentTenantId();
        Rider rider = riderMapper.selectById(id);
        if (rider == null) {
            return false;
        }
        if (currentTenantId != null && !currentTenantId.equals(rider.getTenantId())) {
            throw new CustomException("无权操作其他门店的骑手");
        }
        return riderMapper.deleteById(id) > 0;
    }

    /**
     * 更新 rider status。
     * @param id 参数 id
     * @param status 参数 status
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRiderStatus(Long id, Integer status) {
        Rider rider = riderMapper.selectById(id);
        if (rider == null) {
            return false;
        }
        // 租户归属校验：防止跨租户篡改骑手状态
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(rider.getTenantId())) {
            throw new CustomException("无权操作其他门店的骑手");
        }
        rider.setStatus(status);
        rider.setUpdateTime(LocalDateTime.now());
        return riderMapper.updateById(rider) > 0;
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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRiderLocation(Long riderId, BigDecimal longitude, BigDecimal latitude,
                                        BigDecimal speed, BigDecimal direction) {
        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            return false;
        }
        // 租户归属校验：防止跨租户伪造骑手位置
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(rider.getTenantId())) {
            throw new CustomException("无权操作其他门店的骑手");
        }

        rider.setCurrentLongitude(longitude);
        rider.setCurrentLatitude(latitude);
        rider.setLastLocationTime(LocalDateTime.now());
        rider.setUpdateTime(LocalDateTime.now());
        riderMapper.updateById(rider);

        // Save location record
        RiderLocationRecord record = new RiderLocationRecord();
        record.setRiderId(riderId);
        record.setLongitude(longitude);
        record.setLatitude(latitude);
        record.setSpeed(speed);
        record.setDirection(direction);
        record.setRecordTime(LocalDateTime.now());
        record.setTenantId(rider.getTenantId());
        record.setCreateTime(LocalDateTime.now());

        return locationRecordMapper.insert(record) > 0;
    }

    /**
     * 获取 rider location history。
     * @param riderId 参数 riderId
     * @param startTime 参数 startTime
     * @param endTime 参数 endTime
     * @return 返回结果
     */
    @Override
    public List<RiderLocationRecord> getRiderLocationHistory(Long riderId, LocalDateTime startTime,
            LocalDateTime endTime) {
        LambdaQueryWrapper<RiderLocationRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderLocationRecord::getRiderId, riderId);
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null) {
            qw.eq(RiderLocationRecord::getTenantId, currentTenantId);
        }
        if (startTime != null) {
            qw.ge(RiderLocationRecord::getRecordTime, startTime);
        }
        if (endTime != null) {
            qw.le(RiderLocationRecord::getRecordTime, endTime);
        }
        qw.orderByAsc(RiderLocationRecord::getRecordTime);
        return locationRecordMapper.selectList(qw);
    }

    /**
     * 获取 order delivery tracking。
     * @param orderId 参数 orderId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getOrderDeliveryTracking(Long orderId) {
        Map<String, Object> result = new HashMap<>();

        DeliveryTimeRecord timeRecord = getDeliveryTimeByOrderId(orderId);
        if (timeRecord == null) {
            result.put("found", false);
            return result;
        }

        result.put("found", true);
        result.put("orderId", orderId);
        result.put("orderNumber", timeRecord.getOrderNumber());
        result.put("riderId", timeRecord.getRiderId());
        result.put("riderName", timeRecord.getRiderName());
        result.put("status", timeRecord.getStatus());
        result.put("statusText", getStatusText(timeRecord.getStatus()));
        result.put("estimatedMinutes", timeRecord.getEstimatedMinutes());
        result.put("actualMinutes", timeRecord.getActualMinutes());
        result.put("distance", timeRecord.getDistance());

        // Get rider current location
        if (timeRecord.getRiderId() != null) {
            Rider rider = riderMapper.selectById(timeRecord.getRiderId());
            if (rider != null) {
                Map<String, Object> riderLocation = new HashMap<>();
                riderLocation.put("longitude", rider.getCurrentLongitude());
                riderLocation.put("latitude", rider.getCurrentLatitude());
                riderLocation.put("lastUpdate", rider.getLastLocationTime());
                result.put("riderLocation", riderLocation);
            }
        }

        // Timeline
        List<Map<String, Object>> timeline = new ArrayList<>();
        if (timeRecord.getOrderTime() != null) {
            addTimelineItem(timeline, "ordered", "Order placed", timeRecord.getOrderTime());
        }
        if (timeRecord.getAcceptTime() != null) {
            addTimelineItem(timeline, "accepted", "Order accepted by rider", timeRecord.getAcceptTime());
        }
        if (timeRecord.getPickupTime() != null) {
            addTimelineItem(timeline, "picked_up", "Food picked up", timeRecord.getPickupTime());
        }
        if (timeRecord.getDeliverTime() != null) {
            addTimelineItem(timeline, "delivered", "Delivered", timeRecord.getDeliverTime());
        }
        result.put("timeline", timeline);

        return result;
    }

    /**
     * 获取 rider current location。
     * @param riderId 参数 riderId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getRiderCurrentLocation(Long riderId) {
        Map<String, Object> result = new HashMap<>();

        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            result.put("found", false);
            return result;
        }
        // 租户归属校验：防止跨租户查询骑手位置
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(rider.getTenantId())) {
            result.put("found", false);
            return result;
        }

        result.put("found", true);
        result.put("riderId", riderId);
        result.put("riderName", rider.getName());
        result.put("longitude", rider.getCurrentLongitude());
        result.put("latitude", rider.getCurrentLatitude());
        result.put("status", rider.getStatus());
        result.put("lastUpdate", rider.getLastLocationTime());
        result.put("currentOrderCount", rider.getCurrentOrderCount());

        return result;
    }

    // ==================== Delivery Time Management ====================

    /**
     * 创建 delivery time record。
     * @param record 参数 record
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDeliveryTimeRecord(DeliveryTimeRecord record) {
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        record.setStatus(0); // Pending
        return timeRecordMapper.insert(record) > 0;
    }

    /**
     * 更新 delivery time record。
     * @param record 参数 record
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDeliveryTimeRecord(DeliveryTimeRecord record) {
        record.setUpdateTime(LocalDateTime.now());
        return timeRecordMapper.updateById(record) > 0;
    }

    /**
     * 获取 delivery time by order id。
     * @param orderId 参数 orderId
     * @return 返回结果
     */
    @Override
    public DeliveryTimeRecord getDeliveryTimeByOrderId(Long orderId) {
        LambdaQueryWrapper<DeliveryTimeRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryTimeRecord::getOrderId, orderId);
        return timeRecordMapper.selectOne(qw);
    }

    /**
     * 记录骑手动作（接单/取餐/送达），按 orderId upsert。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean recordRiderAction(Long orderId, String orderNumber, LocalDateTime orderTime,
                                     Long riderId, String riderName, String action) {
        LocalDateTime now = LocalDateTime.now();
        DeliveryTimeRecord rec = getDeliveryTimeByOrderId(orderId);
        boolean isNew = false;
        if (rec == null) {
            rec = new DeliveryTimeRecord();
            rec.setOrderId(orderId);
            rec.setOrderNumber(orderNumber);
            rec.setOrderTime(orderTime);
            rec.setCreateTime(now);
            isNew = true;
        }
        // 无论新建还是已存在，都把骑手绑定关系补齐（兜底派单时未建记录的情况）
        rec.setRiderId(riderId);
        rec.setRiderName(riderName);

        if (ACTION_ACCEPT.equals(action)) {
            rec.setAcceptTime(now);
            rec.setStatus(1);
        } else if (ACTION_PICKUP.equals(action)) {
            rec.setPickupTime(now);
            rec.setStatus(2);
        } else if (ACTION_DELIVER.equals(action)) {
            rec.setDeliverTime(now);
            rec.setStatus(4);
            if (rec.getOrderTime() != null) {
                long mins = java.time.Duration.between(rec.getOrderTime(), now).toMinutes();
                rec.setActualMinutes((int) Math.max(0L, mins));
            }
        } else {
            throw new CustomException("未知骑手动作：" + action);
        }

        rec.setUpdateTime(now);
        return isNew ? timeRecordMapper.insert(rec) > 0 : timeRecordMapper.updateById(rec) > 0;
    }

    /**
     * 处理 estimate delivery time。
     * @param distance 参数 distance
     * @param riderId 参数 riderId
     * @return 返回结果
     */
    @Override
    public int estimateDeliveryTime(BigDecimal distance, Long riderId) {
        // Base time: 10 minutes
        int baseTime = 10;

        // Add time based on distance (assuming 30km/h average speed)
        // distance in meters, speed in km/h
        if (distance != null && distance.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal distanceKm = distance.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
            BigDecimal speedKmh = new BigDecimal("30"); // 30 km/h
            BigDecimal timeHours = distanceKm.divide(speedKmh, 2, RoundingMode.HALF_UP);
            int distanceMinutes = timeHours.multiply(new BigDecimal("60")).intValue();
            baseTime += distanceMinutes;
        }

        // Add buffer time (5 minutes)
        baseTime += 5;

        // Round to nearest 5 minutes
        return ((baseTime + 4) / 5) * 5;
    }

    /**
     * 获取 delivery time statistics。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getDeliveryTimeStatistics(LocalDateTime startDate, LocalDateTime endDate,
            Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<DeliveryTimeRecord> qw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            qw.ge(DeliveryTimeRecord::getCreateTime, startDate);
        }
        if (endDate != null) {
            qw.le(DeliveryTimeRecord::getCreateTime, endDate);
        }
        if (tenantId != null) {
            qw.eq(DeliveryTimeRecord::getTenantId, tenantId);
        }
        List<DeliveryTimeRecord> records = timeRecordMapper.selectList(qw);

        int totalOrders = records.size();
        int deliveredOrders = 0;
        int cancelledOrders = 0;
        long totalMinutes = 0;
        int onTimeOrders = 0;

        for (DeliveryTimeRecord record : records) {
            if (record.getStatus() == 4) { // Delivered
                deliveredOrders++;
                if (record.getActualMinutes() != null) {
                    totalMinutes += record.getActualMinutes();
                    if (record.getEstimatedMinutes() != null && 
                        record.getActualMinutes() <= record.getEstimatedMinutes()) {
                        onTimeOrders++;
                    }
                }
            } else if (record.getStatus() == 5) { // Cancelled
                cancelledOrders++;
            }
        }

        BigDecimal avgMinutes = deliveredOrders > 0 ?
                new BigDecimal(totalMinutes).divide(new BigDecimal(deliveredOrders), 1, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        BigDecimal onTimeRate = deliveredOrders > 0 ?
                new BigDecimal(onTimeOrders).divide(new BigDecimal(deliveredOrders), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")) :
                BigDecimal.ZERO;

        result.put("totalOrders", totalOrders);
        result.put("deliveredOrders", deliveredOrders);
        result.put("cancelledOrders", cancelledOrders);
        result.put("avgDeliveryMinutes", avgMinutes);
        result.put("onTimeRate", onTimeRate);

        return result;
    }

    // ==================== Statistics ====================

    /**
     * 获取 rider statistics。
     * @param riderId 参数 riderId
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getRiderStatistics(Long riderId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<DeliveryTimeRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryTimeRecord::getRiderId, riderId);
        if (startDate != null) {
            qw.ge(DeliveryTimeRecord::getCreateTime, startDate);
        }
        if (endDate != null) {
            qw.le(DeliveryTimeRecord::getCreateTime, endDate);
        }
        List<DeliveryTimeRecord> records = timeRecordMapper.selectList(qw);

        int totalDeliveries = 0;
        int completedDeliveries = 0;
        long totalMinutes = 0;

        for (DeliveryTimeRecord record : records) {
            totalDeliveries++;
            if (record.getStatus() == 4) {
                completedDeliveries++;
                if (record.getActualMinutes() != null) {
                    totalMinutes += record.getActualMinutes();
                }
            }
        }

        BigDecimal avgMinutes = completedDeliveries > 0 ?
                new BigDecimal(totalMinutes).divide(new BigDecimal(completedDeliveries), 1, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        Rider rider = riderMapper.selectById(riderId);
        String riderName = rider != null ? rider.getName() : "";

        result.put("riderId", riderId);
        result.put("riderName", riderName);
        result.put("totalDeliveries", totalDeliveries);
        result.put("completedDeliveries", completedDeliveries);
        result.put("avgDeliveryMinutes", avgMinutes);
        result.put("rating", rider != null ? rider.getRating() : BigDecimal.ZERO);

        return result;
    }

    /**
     * 获取 delivery overview。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getDeliveryOverview(Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // Rider counts
        LambdaQueryWrapper<Rider> riderQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            riderQw.eq(Rider::getTenantId, tenantId);
        }
        List<Rider> allRiders = riderMapper.selectList(riderQw);

        int totalRiders = allRiders.size();
        int onlineRiders = 0;
        int busyRiders = 0;
        int availableRiders = 0;

        for (Rider rider : allRiders) {
            if (rider.getStatus() == Rider.STATUS_ONLINE) {
                onlineRiders++;
                if (rider.getCurrentOrderCount() != null && rider.getCurrentOrderCount() < 3) {
                    availableRiders++;
                }
            } else if (rider.getStatus() == Rider.STATUS_BUSY) {
                busyRiders++;
            }
        }

        // Today's delivery statistics
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LambdaQueryWrapper<DeliveryTimeRecord> todayQw = new LambdaQueryWrapper<>();
        todayQw.ge(DeliveryTimeRecord::getCreateTime, todayStart);
        if (tenantId != null) {
            todayQw.eq(DeliveryTimeRecord::getTenantId, tenantId);
        }
        List<DeliveryTimeRecord> todayRecords = timeRecordMapper.selectList(todayQw);

        int todayOrders = todayRecords.size();
        int todayDelivered = 0;
        int todayPending = 0;

        for (DeliveryTimeRecord record : todayRecords) {
            if (record.getStatus() == 4) {
                todayDelivered++;
            } else if (record.getStatus() < 4) {
                todayPending++;
            }
        }

        result.put("totalRiders", totalRiders);
        result.put("onlineRiders", onlineRiders);
        result.put("busyRiders", busyRiders);
        result.put("availableRiders", availableRiders);
        result.put("todayOrders", todayOrders);
        result.put("todayDelivered", todayDelivered);
        result.put("todayPending", todayPending);

        return result;
    }

    // ==================== Helper Methods ====================

    private String getStatusText(Integer status) {
        if (status == null) return "Unknown";
        switch (status) {
            case 0: return "Pending";
            case 1: return "Accepted";
            case 2: return "Picked up";
            case 3: return "Delivering";
            case 4: return "Delivered";
            case 5: return "Cancelled";
            default: return "Unknown";
        }
    }

    private void addTimelineItem(List<Map<String, Object>> timeline, String type, String description,
            LocalDateTime time) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", type);
        item.put("description", description);
        item.put("time", time.toString());
        timeline.add(item);
    }
}

