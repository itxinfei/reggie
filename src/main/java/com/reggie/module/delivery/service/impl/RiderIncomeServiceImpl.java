package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.delivery.dto.RiderIncomeRecordVO;
import com.reggie.module.delivery.dto.RiderIncomeSummaryVO;
import com.reggie.module.delivery.mapper.DeliveryTimeRecordMapper;
import com.reggie.module.delivery.model.DeliveryTimeRecord;
import com.reggie.module.delivery.service.RiderIncomeService;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.Orders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 骑手收入服务实现（只读聚合，不记账）。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Service
public class RiderIncomeServiceImpl implements RiderIncomeService {

    private static final BigDecimal METER_TO_KM = new BigDecimal("1000");

    @Autowired
    private OrderMapper ordersMapper;

    @Autowired
    private DeliveryTimeRecordMapper timeRecordMapper;

    @Override
    public Map<String, RiderIncomeSummaryVO> getRiderIncomeSummary(Long riderId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        // 本周窗口以「近 7 天」口径：now-6 天 00:00 ~ now，可同时覆盖今日/本月
        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        List<Orders> orders = completedOrders(riderId, weekStart, now);
        Map<Long, DeliveryTimeRecord> recByOrder = timeRecordsByOrder(riderId, weekStart, now);

        Map<String, RiderIncomeSummaryVO> result = new HashMap<>(4);
        result.put("today", summarize(inBucket(orders, todayStart, now), recByOrder));
        result.put("week", summarize(inBucket(orders, weekStart, now), recByOrder));
        result.put("month", summarize(inBucket(orders, monthStart, now), recByOrder));
        return result;
    }

    @Override
    public Page<RiderIncomeRecordVO> getRiderIncomeRecords(Long riderId, String range, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = rangeStart(range);

        List<Orders> orders = completedOrders(riderId, start, now);
        Map<Long, DeliveryTimeRecord> recByOrder = timeRecordsByOrder(riderId, start, now);
        List<RiderIncomeRecordVO> all = buildRecords(orders, recByOrder);

        int safeSize = PageUtils.cap(size);
        Page<RiderIncomeRecordVO> p = PageUtils.of(page, safeSize);
        int total = all.size();
        int from = Math.max(0, (p.getCurrent() <= 0 ? 1 : (int) p.getCurrent()) - 1) * safeSize;
        if (from >= total) {
            p.setRecords(new ArrayList<RiderIncomeRecordVO>());
        } else {
            int to = Math.min(from + safeSize, total);
            p.setRecords(new ArrayList<RiderIncomeRecordVO>(all.subList(from, to)));
        }
        p.setTotal(total);
        return p;
    }

    // ==================== 私有聚合方法 ====================

    /**
     * 查询骑手已完成（status=4）且在时间窗口内的订单，按下单时间倒序。
     */
    private List<Orders> completedOrders(Long riderId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
        qw.eq(Orders::getRiderId, riderId)
                .eq(Orders::getStatus, Orders.STATUS_COMPLETED)
                .ge(Orders::getOrderTime, start)
                .le(Orders::getOrderTime, end)
                .orderByDesc(Orders::getOrderTime);
        return ordersMapper.selectList(qw);
    }

    /**
     * 查询骑手配送时效记录，按 orderId 建立索引（仅取窗口内）。
     */
    private Map<Long, DeliveryTimeRecord> timeRecordsByOrder(Long riderId, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<DeliveryTimeRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(DeliveryTimeRecord::getRiderId, riderId)
                .ge(DeliveryTimeRecord::getCreateTime, start)
                .le(DeliveryTimeRecord::getCreateTime, end);
        List<DeliveryTimeRecord> records = timeRecordMapper.selectList(qw);
        Map<Long, DeliveryTimeRecord> map = new HashMap<>(records.size());
        for (DeliveryTimeRecord r : records) {
            if (r.getOrderId() != null) {
                map.put(r.getOrderId(), r);
            }
        }
        return map;
    }

    /**
     * 按窗口过滤订单（orderTime ∈ [start, end]）。
     */
    private List<Orders> inBucket(List<Orders> orders, LocalDateTime start, LocalDateTime end) {
        List<Orders> bucket = new ArrayList<>();
        for (Orders o : orders) {
            if (o.getOrderTime() != null
                    && !o.getOrderTime().isBefore(start)
                    && !o.getOrderTime().isAfter(end)) {
                bucket.add(o);
            }
        }
        return bucket;
    }

    /**
     * 聚合一个窗口的汇总指标。
     */
    private RiderIncomeSummaryVO summarize(List<Orders> bucket, Map<Long, DeliveryTimeRecord> recByOrder) {
        RiderIncomeSummaryVO vo = new RiderIncomeSummaryVO();
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal meters = BigDecimal.ZERO;
        long sumMinutes = 0;
        int durCount = 0;
        int onTime = 0;
        int estCount = 0;

        for (Orders o : bucket) {
            if (o.getDeliveryFee() != null) {
                income = income.add(o.getDeliveryFee());
            }
            DeliveryTimeRecord r = recByOrder.get(o.getId());
            if (r != null && r.getStatus() != null && r.getStatus() == 4) {
                if (r.getDistance() != null) {
                    meters = meters.add(r.getDistance());
                }
                if (r.getActualMinutes() != null) {
                    sumMinutes += r.getActualMinutes();
                    durCount++;
                }
                if (r.getActualMinutes() != null && r.getEstimatedMinutes() != null) {
                    estCount++;
                    if (r.getActualMinutes() <= r.getEstimatedMinutes()) {
                        onTime++;
                    }
                }
            }
        }

        vo.setIncome(income);
        vo.setOrderCount(bucket.size());
        vo.setMileage(meters.divide(METER_TO_KM, 2, RoundingMode.HALF_UP));
        vo.setAvgMinutes(durCount > 0
                ? new BigDecimal(sumMinutes).divide(new BigDecimal(durCount), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        vo.setOnTimeRate(estCount > 0
                ? new BigDecimal(onTime).divide(new BigDecimal(estCount), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO);
        return vo;
    }

    /**
     * 构建单笔收入明细视图列表（保持 orderTime 倒序）。
     */
    private List<RiderIncomeRecordVO> buildRecords(List<Orders> orders, Map<Long, DeliveryTimeRecord> recByOrder) {
        List<RiderIncomeRecordVO> list = new ArrayList<>(orders.size());
        for (Orders o : orders) {
            RiderIncomeRecordVO vo = new RiderIncomeRecordVO();
            vo.setOrderNumber(o.getNumber());
            vo.setOrderTime(o.getOrderTime());
            vo.setIncome(o.getDeliveryFee() != null ? o.getDeliveryFee() : BigDecimal.ZERO);
            vo.setAmount(o.getAmount());
            DeliveryTimeRecord r = recByOrder.get(o.getId());
            if (r != null) {
                vo.setDistance(r.getDistance() != null
                        ? r.getDistance().divide(METER_TO_KM, 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO);
                vo.setDurationMin(r.getActualMinutes());
                vo.setOnTime(r.getActualMinutes() != null
                        && r.getEstimatedMinutes() != null
                        && r.getActualMinutes() <= r.getEstimatedMinutes());
            } else {
                vo.setDistance(BigDecimal.ZERO);
                vo.setOnTime(false);
            }
            list.add(vo);
        }
        return list;
    }

    /**
     * 解析时间窗口起始点。
     */
    private LocalDateTime rangeStart(String range) {
        LocalDate today = LocalDate.now();
        if ("today".equals(range)) {
            return today.atStartOfDay();
        }
        if ("week".equals(range)) {
            return today.minusDays(6).atStartOfDay();
        }
        // 默认本月
        return today.withDayOfMonth(1).atStartOfDay();
    }
}
