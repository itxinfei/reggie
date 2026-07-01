package com.reggie.module.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import com.reggie.module.report.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ReportServiceImpl implements ReportService {

    private static final int SLOT_COUNT = 5;
    private static final String[] SLOT_NAMES = {"早市(6-10)", "午市(10-14)", "下午茶(14-17)", "晚市(17-21)", "夜市(21-6)"};

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Override
    public Map<String, Object> getDailyReport(String date) {
        Map<String, Object> result = new HashMap<>();
        LocalDate reportDate = LocalDate.parse(date);
        LocalDateTime start = reportDate.atStartOfDay();
        LocalDateTime end = reportDate.atTime(LocalTime.MAX);

        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.between(Orders::getOrderTime, start, end);
        List<Orders> orders = orderService.list(orderQw);

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalOrders = 0;
        int completedOrders = 0;
        int cancelledOrders = 0;

        for (Orders o : orders) {
            totalOrders++;
            totalAmount = totalAmount.add(o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO);
            if (o.getStatus() != null && o.getStatus() == Orders.STATUS_COMPLETED) completedOrders++;
            if (o.getStatus() != null && o.getStatus() == Orders.STATUS_CANCELLED) cancelledOrders++;
        }

        result.put("totalOrders", totalOrders);
        result.put("totalAmount", totalAmount);
        result.put("completedOrders", completedOrders);
        result.put("cancelledOrders", cancelledOrders);
        result.put("avgAmount", totalOrders > 0 ? totalAmount.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        return result;
    }

    @Override
    public List<Map<String, Object>> getDishRanking(String startDate, String endDate, int limit) {
        List<Map<String, Object>> ranking = new ArrayList<>();
        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.between(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay(),
                LocalDate.parse(endDate).atTime(LocalTime.MAX));
        orderQw.select(Orders::getId);
        List<Orders> orders = orderService.list(orderQw);
        if (orders.isEmpty()) return ranking;

        List<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());
        LambdaQueryWrapper<OrderDetail> detailQw = new LambdaQueryWrapper<>();
        detailQw.in(OrderDetail::getOrderId, orderIds);
        List<OrderDetail> details = orderDetailService.list(detailQw);

        Map<String, Integer> dishCount = new LinkedHashMap<>();
        for (OrderDetail d : details) {
            if (d.getName() != null) {
                dishCount.merge(d.getName(), d.getNumber() != null ? d.getNumber() : 0, Integer::sum);
            }
        }

        dishCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .forEach(e -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", e.getKey());
                    item.put("count", e.getValue());
                    ranking.add(item);
                });

        return ranking;
    }

    @Override
    public List<Map<String, Object>> getTimeSlotAnalysis(String startDate, String endDate) {
        List<Map<String, Object>> slots = new ArrayList<>();


        LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
        qw.between(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay(),
                LocalDate.parse(endDate).atTime(LocalTime.MAX));
        List<Orders> orders = orderService.list(qw);

        int[] counts = new int[SLOT_COUNT];
        BigDecimal[] amounts = new BigDecimal[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) amounts[i] = BigDecimal.ZERO;

        for (Orders o : orders) {
            if (o.getOrderTime() == null) continue;
            int hour = o.getOrderTime().getHour();
            int idx;
            if (hour >= 6 && hour < 10) idx = 0;
            else if (hour >= 10 && hour < 14) idx = 1;
            else if (hour >= 14 && hour < 17) idx = 2;
            else if (hour >= 17 && hour < 21) idx = 3;
            else idx = 4;
            counts[idx]++;
            amounts[idx] = amounts[idx].add(o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO);
        }

        for (int i = 0; i < SLOT_COUNT; i++) {
            Map<String, Object> slot = new HashMap<>();
            slot.put("name", SLOT_NAMES[i]);
            slot.put("count", counts[i]);
            slot.put("amount", amounts[i]);
            slots.add(slot);
        }
        return slots;
    }

    @Override
    public Map<String, Object> getPaymentAnalysis(String startDate, String endDate) {
        LambdaQueryWrapper<Orders> qw = new LambdaQueryWrapper<>();
        qw.between(Orders::getOrderTime, LocalDate.parse(startDate).atStartOfDay(),
                LocalDate.parse(endDate).atTime(LocalTime.MAX));
        List<Orders> orders = orderService.list(qw);

        int wechatCount = 0, alipayCount = 0, balanceCount = 0, otherCount = 0;
        BigDecimal wechatAmount = BigDecimal.ZERO, alipayAmount = BigDecimal.ZERO;

        for (Orders o : orders) {
            BigDecimal amt = o.getAmount() != null ? o.getAmount() : BigDecimal.ZERO;
            if (o.getPayMethod() != null) {
                switch (o.getPayMethod()) {
                    case 1: wechatCount++; wechatAmount = wechatAmount.add(amt); break;
                    case 2: alipayCount++; alipayAmount = alipayAmount.add(amt); break;
                    case 3: balanceCount++; break;
                    default: otherCount++; break;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> wechat = new HashMap<>();
        wechat.put("count", wechatCount);
        wechat.put("amount", wechatAmount);
        result.put("wechat", wechat);
        Map<String, Object> alipay = new HashMap<>();
        alipay.put("count", alipayCount);
        alipay.put("amount", alipayAmount);
        result.put("alipay", alipay);
        Map<String, Object> balance = new HashMap<>();
        balance.put("count", balanceCount);
        result.put("balance", balance);
        Map<String, Object> other = new HashMap<>();
        other.put("count", otherCount);
        result.put("other", other);
        return result;
    }

    @Override
    public byte[] exportDailyReport(String startDate, String endDate) {
        StringBuilder csv = new StringBuilder();
        csv.append("日期,订单数,总金额,已完成,已取消\n");

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            Map<String, Object> report = getDailyReport(date.toString());
            csv.append(date).append(",")
                    .append(report.get("totalOrders")).append(",")
                    .append(report.get("totalAmount")).append(",")
                    .append(report.get("completedOrders")).append(",")
                    .append(report.get("cancelledOrders")).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
