package com.reggie.module.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.cashier.model.CashierRecord;
import com.reggie.module.cashier.model.DailySettlement;
import com.reggie.module.order.model.Orders;
import com.reggie.module.payment.model.PaymentOrder;
import com.reggie.module.cashier.mapper.DailySettlementMapper;
import com.reggie.module.cashier.mapper.CashierRecordMapper;
import com.reggie.module.cashier.service.CashierService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.payment.service.PaymentOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收银服务实现
 *
 * @author reggie
 * @since 2026-08-10
 */
@Slf4j
/**
 * Cashier service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class CashierServiceImpl extends ServiceImpl<CashierRecordMapper, CashierRecord> implements CashierService {

    @Autowired
    private CashierRecordMapper cashierRecordMapper;

    @Autowired
    private DailySettlementMapper dailySettlementMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentOrderService paymentOrderService;

    // ==================== 收银记录管理 ====================

    @Override
    public List<CashierRecord> getCashierRecordList(Integer payType, LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
        if (payType != null) {
            qw.eq(CashierRecord::getPayType, payType);
        }
        if (startDate != null) {
            qw.ge(CashierRecord::getCashierTime, startDate);
        }
        if (endDate != null) {
            qw.le(CashierRecord::getCashierTime, endDate);
        }
        if (tenantId != null) {
            qw.eq(CashierRecord::getTenantId, tenantId);
        }
        qw.orderByDesc(CashierRecord::getCashierTime);
        return cashierRecordMapper.selectList(qw);
    }

    @Override
    public CashierRecord getCashierRecordByOrderId(Long orderId, Long tenantId) {
        LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(CashierRecord::getOrderId, orderId);
        if (tenantId != null) {
            qw.eq(CashierRecord::getTenantId, tenantId);
        }
        return cashierRecordMapper.selectOne(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCashierRecord(CashierRecord cashierRecord) {
        cashierRecord.setCreateTime(LocalDateTime.now());
        return cashierRecordMapper.insert(cashierRecord) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CashierRecord cashPayment(Long orderId, String orderNumber, BigDecimal amount, BigDecimal actualAmount,
                                     Long cashierId, String cashierName, String remark) {
        // 1. 计算找零
        BigDecimal changeAmount = BigDecimal.ZERO;
        if (actualAmount.compareTo(amount) > 0) {
            changeAmount = actualAmount.subtract(amount);
        }

        // 2. 创建收银记录
        CashierRecord cashierRecord = new CashierRecord();
        cashierRecord.setOrderId(orderId);
        cashierRecord.setOrderNumber(orderNumber);
        cashierRecord.setPayType(1); // 现金
        cashierRecord.setAmount(amount);
        cashierRecord.setActualAmount(actualAmount);
        cashierRecord.setChangeAmount(changeAmount);
        cashierRecord.setCashierTime(LocalDateTime.now());
        cashierRecord.setCashierId(cashierId);
        cashierRecord.setCashierName(cashierName);
        cashierRecord.setRemark(remark);
        cashierRecord.setTenantId(BaseContext.getCurrentTenantId());
        cashierRecord.setCreateTime(LocalDateTime.now());
        cashierRecord.setCreateUser(cashierId);

        // 3. 保存收银记录
        cashierRecordMapper.insert(cashierRecord);

        // 4. 更新订单状态为已支付
        Orders order = orderService.getById(orderId);
        if (order != null) {
            order.setStatus(Orders.STATUS_ORDERED); // 待接单
            order.setPayMethod(1); // 现金支付
            order.setCheckoutTime(LocalDateTime.now());
            orderService.updateById(order);
        }

        // 5. 创建支付记录
        PaymentOrder paymentOrder = new PaymentOrder();
        paymentOrder.setOrderId(orderId);
        paymentOrder.setTenantId(BaseContext.getCurrentTenantId());
        paymentOrder.setTradeNo("CASH" + System.currentTimeMillis());
        paymentOrder.setChannel("CASH");
        paymentOrder.setAmount(amount);
        paymentOrder.setStatus(PaymentOrder.STATUS_SUCCESS);
        paymentOrder.setPaidTime(LocalDateTime.now());
        paymentOrder.setCreatedTime(LocalDateTime.now());
        paymentOrder.setUpdateTime(LocalDateTime.now());
        paymentOrderService.save(paymentOrder);

        return cashierRecord;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCashierRecord(Long id) {
        return cashierRecordMapper.deleteById(id) > 0;
    }

    // ==================== 日结管理 ====================

    @Override
    public List<DailySettlement> getDailySettlementList(LocalDate startDate, LocalDate endDate, Long tenantId) {
        LambdaQueryWrapper<DailySettlement> qw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            qw.ge(DailySettlement::getSettlementDate, startDate);
        }
        if (endDate != null) {
            qw.le(DailySettlement::getSettlementDate, endDate);
        }
        if (tenantId != null) {
            qw.eq(DailySettlement::getTenantId, tenantId);
        }
        qw.orderByDesc(DailySettlement::getSettlementDate);
        return dailySettlementMapper.selectList(qw);
    }

    @Override
    public DailySettlement getDailySettlementByDate(LocalDate settlementDate, Long tenantId) {
        LambdaQueryWrapper<DailySettlement> qw = new LambdaQueryWrapper<>();
        qw.eq(DailySettlement::getSettlementDate, settlementDate);
        if (tenantId != null) {
            qw.eq(DailySettlement::getTenantId, tenantId);
        }
        return dailySettlementMapper.selectOne(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DailySettlement executeDailySettlement(LocalDate settlementDate, Long userId, String userName, Long tenantId) {
        // 1. 检查是否已日结
        DailySettlement existing = getDailySettlementByDate(settlementDate, tenantId);
        if (existing != null && existing.getStatus() == 1) {
            throw new RuntimeException("该日期已日结，不能重复日结");
        }

        // 2. 查询当日订单
        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.ge(Orders::getOrderTime, settlementDate.atStartOfDay());
        orderQw.le(Orders::getOrderTime, settlementDate.atTime(LocalTime.MAX));
        if (tenantId != null) {
            orderQw.eq(Orders::getTenantId, tenantId);
        }
        List<Orders> orders = orderService.list(orderQw);

        // 3. 统计营业额
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal cashIncome = BigDecimal.ZERO;
        BigDecimal wechatIncome = BigDecimal.ZERO;
        BigDecimal alipayIncome = BigDecimal.ZERO;
        BigDecimal bankcardIncome = BigDecimal.ZERO;
        BigDecimal otherIncome = BigDecimal.ZERO;
        int orderCount = 0;
        BigDecimal refundAmount = BigDecimal.ZERO;
        int refundCount = 0;

        for (Orders order : orders) {
            if (order.getStatus() == Orders.STATUS_COMPLETED) {
                orderCount++;
                BigDecimal amount = order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO;
                totalRevenue = totalRevenue.add(amount);

                // 按支付方式统计
                Integer payMethod = order.getPayMethod();
                if (payMethod == null) {
                    otherIncome = otherIncome.add(amount);
                } else if (payMethod == 1) {
                    cashIncome = cashIncome.add(amount);
                } else if (payMethod == 2) {
                    wechatIncome = wechatIncome.add(amount);
                } else if (payMethod == 3) {
                    alipayIncome = alipayIncome.add(amount);
                } else if (payMethod == 4) {
                    bankcardIncome = bankcardIncome.add(amount);
                } else {
                    otherIncome = otherIncome.add(amount);
                }
            } else if (order.getStatus() == Orders.STATUS_REFUNDED) {
                refundCount++;
                refundAmount = refundAmount.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
            }
        }

        // 4. 计算净收入
        BigDecimal netIncome = totalRevenue.subtract(refundAmount);

        // 5. 查询成本（这里简化处理，实际需要查询成本模块）
        BigDecimal materialCost = BigDecimal.ZERO;
        BigDecimal laborCost = BigDecimal.ZERO;
        BigDecimal otherCost = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;

        // 6. 计算毛利润
        BigDecimal grossProfit = netIncome.subtract(totalCost);
        BigDecimal profitRate = BigDecimal.ZERO;
        if (netIncome.compareTo(BigDecimal.ZERO) > 0) {
            profitRate = grossProfit.divide(netIncome, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        // 7. 创建或更新日结记录
        DailySettlement settlement;
        if (existing != null) {
            settlement = existing;
        } else {
            settlement = new DailySettlement();
            settlement.setSettlementDate(settlementDate);
            settlement.setTenantId(tenantId);
        }

        settlement.setTotalRevenue(totalRevenue);
        settlement.setCashIncome(cashIncome);
        settlement.setWechatIncome(wechatIncome);
        settlement.setAlipayIncome(alipayIncome);
        settlement.setBankcardIncome(bankcardIncome);
        settlement.setOtherIncome(otherIncome);
        settlement.setOrderCount(orderCount);
        settlement.setRefundAmount(refundAmount);
        settlement.setRefundCount(refundCount);
        settlement.setNetIncome(netIncome);
        settlement.setMaterialCost(materialCost);
        settlement.setLaborCost(laborCost);
        settlement.setOtherCost(otherCost);
        settlement.setTotalCost(totalCost);
        settlement.setGrossProfit(grossProfit);
        settlement.setProfitRate(profitRate);
        settlement.setStatus(1); // 已结账
        settlement.setSettlementTime(LocalDateTime.now());
        settlement.setSettlementUserId(userId);
        settlement.setSettlementUserName(userName);

        if (existing != null) {
            settlement.setUpdateTime(LocalDateTime.now());
            settlement.setUpdateUser(userId);
            dailySettlementMapper.updateById(settlement);
        } else {
            settlement.setCreateTime(LocalDateTime.now());
            settlement.setCreateUser(userId);
            dailySettlementMapper.insert(settlement);
        }

        return settlement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelDailySettlement(LocalDate settlementDate, Long tenantId) {
        DailySettlement settlement = getDailySettlementByDate(settlementDate, tenantId);
        if (settlement == null) {
            throw new RuntimeException("日结记录不存在");
        }
        if (settlement.getStatus() == 0) {
            throw new RuntimeException("日结已取消，不能重复取消");
        }

        settlement.setStatus(0); // 未结账
        settlement.setUpdateTime(LocalDateTime.now());
        return dailySettlementMapper.updateById(settlement) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDailySettlement(Long id) {
        return dailySettlementMapper.deleteById(id) > 0;
    }

    // ==================== 统计分析 ====================

    @Override
    public Map<String, Object> getCashierStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查询收银记录
        LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
        qw.ge(CashierRecord::getCashierTime, startDate);
        qw.le(CashierRecord::getCashierTime, endDate);
        if (tenantId != null) {
            qw.eq(CashierRecord::getTenantId, tenantId);
        }
        List<CashierRecord> records = cashierRecordMapper.selectList(qw);

        // 统计
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalCount = 0;
        for (CashierRecord record : records) {
            totalAmount = totalAmount.add(record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO);
            totalCount++;
        }

        result.put("totalAmount", totalAmount);
        result.put("totalCount", totalCount);
        result.put("avgAmount", totalCount > 0 ? totalAmount.divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        return result;
    }

    @Override
    public Map<String, Object> getPaymentTypeStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查询收银记录
        LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
        qw.ge(CashierRecord::getCashierTime, startDate);
        qw.le(CashierRecord::getCashierTime, endDate);
        if (tenantId != null) {
            qw.eq(CashierRecord::getTenantId, tenantId);
        }
        List<CashierRecord> records = cashierRecordMapper.selectList(qw);

        // 按支付方式统计
        Map<Integer, BigDecimal> typeAmountMap = new HashMap<>();
        Map<Integer, Integer> typeCountMap = new HashMap<>();

        for (CashierRecord record : records) {
            Integer payType = record.getPayType();
            BigDecimal amount = record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO;

            typeAmountMap.merge(payType, amount, BigDecimal::add);
            typeCountMap.merge(payType, 1, Integer::sum);
        }

        // 构建结果
        List<Map<String, Object>> typeList = new ArrayList<>();
        String[] typeNames = {"现金", "微信", "支付宝", "银行卡", "其他"};

        for (int i = 1; i <= 5; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", i);
            item.put("typeName", typeNames[i - 1]);
            item.put("amount", typeAmountMap.getOrDefault(i, BigDecimal.ZERO));
            item.put("count", typeCountMap.getOrDefault(i, 0));
            typeList.add(item);
        }

        result.put("typeList", typeList);
        return result;
    }

    @Override
    public Map<String, Object> getCashierTrend(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        // 按天统计
        LocalDate current = startDate.toLocalDate();
        LocalDate end = endDate.toLocalDate();

        while (!current.isAfter(end)) {
            dates.add(current.toString());

            // 查询当天收银记录
            LambdaQueryWrapper<CashierRecord> qw = new LambdaQueryWrapper<>();
            qw.ge(CashierRecord::getCashierTime, current.atStartOfDay());
            qw.le(CashierRecord::getCashierTime, current.atTime(LocalTime.MAX));
            if (tenantId != null) {
                qw.eq(CashierRecord::getTenantId, tenantId);
            }
            List<CashierRecord> records = cashierRecordMapper.selectList(qw);

            BigDecimal dayAmount = BigDecimal.ZERO;
            int dayCount = 0;
            for (CashierRecord record : records) {
                dayAmount = dayAmount.add(record.getAmount() != null ? record.getAmount() : BigDecimal.ZERO);
                dayCount++;
            }

            amounts.add(dayAmount);
            counts.add(dayCount);

            current = current.plusDays(1);
        }

        result.put("dates", dates);
        result.put("amounts", amounts);
        result.put("counts", counts);

        return result;
    }

    @Override
    public Map<String, Object> getDailySettlementSummary(LocalDate startDate, LocalDate endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查询日结记录
        LambdaQueryWrapper<DailySettlement> qw = new LambdaQueryWrapper<>();
        qw.ge(DailySettlement::getSettlementDate, startDate);
        qw.le(DailySettlement::getSettlementDate, endDate);
        if (tenantId != null) {
            qw.eq(DailySettlement::getTenantId, tenantId);
        }
        List<DailySettlement> settlements = dailySettlementMapper.selectList(qw);

        // 统计
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal totalNetIncome = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalGrossProfit = BigDecimal.ZERO;
        int totalOrders = 0;

        for (DailySettlement settlement : settlements) {
            totalRevenue = totalRevenue.add(settlement.getTotalRevenue() != null ? settlement.getTotalRevenue() : BigDecimal.ZERO);
            totalRefund = totalRefund.add(settlement.getRefundAmount() != null ? settlement.getRefundAmount() : BigDecimal.ZERO);
            totalNetIncome = totalNetIncome.add(settlement.getNetIncome() != null ? settlement.getNetIncome() : BigDecimal.ZERO);
            totalCost = totalCost.add(settlement.getTotalCost() != null ? settlement.getTotalCost() : BigDecimal.ZERO);
            totalGrossProfit = totalGrossProfit.add(settlement.getGrossProfit() != null ? settlement.getGrossProfit() : BigDecimal.ZERO);
            totalOrders += settlement.getOrderCount() != null ? settlement.getOrderCount() : 0;
        }

        // 计算平均毛利率
        BigDecimal avgProfitRate = BigDecimal.ZERO;
        if (totalNetIncome.compareTo(BigDecimal.ZERO) > 0) {
            avgProfitRate = totalGrossProfit.divide(totalNetIncome, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }

        result.put("totalRevenue", totalRevenue);
        result.put("totalRefund", totalRefund);
        result.put("totalNetIncome", totalNetIncome);
        result.put("totalCost", totalCost);
        result.put("totalGrossProfit", totalGrossProfit);
        result.put("avgProfitRate", avgProfitRate);
        result.put("totalOrders", totalOrders);
        result.put("settlementCount", settlements.size());

        return result;
    }
}






