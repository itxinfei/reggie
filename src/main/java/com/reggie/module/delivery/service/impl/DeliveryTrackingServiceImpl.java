package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import java.time.temporal.ChronoUnit;
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

    @Override
    public Rider getRiderById(Long id) {
        return riderMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateRider(Rider rider) {
        if (rider.getId() == null) {
            rider.setCreateTime(LocalDateTime.now());
            rider.setUpdateTime(LocalDateTime.now());
            rider.setCurrentOrderCount(0);
            rider.setTotalOrderCount(0);
            rider.setRating(new BigDecimal("5.0"));
            return riderMapper.insert(rider) > 0;
        } else {
            rider.setUpdateTime(LocalDateTime.now());
            return riderMapper.updateById(rider) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRider(Long id) {
        return riderMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRiderStatus(Long id, Integer status) {
        Rider rider = riderMapper.selectById(id);
        if (rider == null) {
            return false;
        }
        rider.setStatus(status);
        rider.setUpdateTime(LocalDateTime.now());
        return riderMapper.updateById(rider) > 0;
    }

    // ==================== Location Tracking ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRiderLocation(Long riderId, BigDecimal longitude, BigDecimal latitude,
                                        BigDecimal speed, BigDecimal direction) {
        // Update rider current location
        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) {
            return false;
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

    @Override
    public List<RiderLocationRecord> getRiderLocationHistory(Long riderId, LocalDateTime startTime, LocalDateTime endTime) {
        LambdaQueryWrapper<RiderLocationRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(RiderLocationRecord::getRiderId, riderId);
        if (startTime != null) {
            qw.ge(RiderLocationRecord::getRecordTime, startTime);
        }
        if (endTime != null) {
            qw.le(RiderLocationRecord::getRecordTime, endTime);
        }
        qw.orderByAsc(RiderLocationRecord::getRecordTime);
        return locationRecordMapper.selectList(qw);
    }

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

    @Override
    public Map<String, Object> getRiderCurrentLocation(Long riderId) {
        Map<String, Object> result = new HashMap<>();

        Rider rider = riderMapper.selectById(riderId);
        if (rider == null) {
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDeliveryTimeRecord(DeliveryTimeRecord record) {
        record.setCreateTime(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        record.setStatus(0); // Pending
        return timeRecordMapper.insert(record) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDeliveryTimeRecord(DeliveryTimeRecord record) {
        record.setUpdateTime(LocalDateTime.now());
        return timeRecordMapper.updateById(record) > 0;
    }

    @Override
    public DeliveryTimeRecord getDeliveryTimeByOrderId(Long orderId) {
        LambdaQueryWrapper<DeliveryTimeRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryTimeRecord::getOrderId, orderId);
        return timeRecordMapper.selectOne(qw);
    }

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

    @Override
    public Map<String, Object> getDeliveryTimeStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
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

    private void addTimelineItem(List<Map<String, Object>> timeline, String type, String description, LocalDateTime time) {
        Map<String, Object> item = new HashMap<>();
        item.put("type", type);
        item.put("description", description);
        item.put("time", time.toString());
        timeline.add(item);
    }
}

