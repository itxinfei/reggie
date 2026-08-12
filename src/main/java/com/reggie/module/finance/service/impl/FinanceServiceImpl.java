package com.reggie.module.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.finance.mapper.WithdrawalApplicationMapper;
import com.reggie.module.finance.mapper.ReconciliationStatementMapper;
import com.reggie.module.finance.mapper.ProfitAnalysisMapper;
import com.reggie.module.finance.model.WithdrawalApplication;
import com.reggie.module.finance.model.ReconciliationStatement;
import com.reggie.module.finance.model.ProfitAnalysis;
import com.reggie.module.finance.service.FinanceService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.cost.service.CostService;
import com.reggie.module.order.model.Orders;
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

/**
 * Finance Service Implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Slf4j
/**
 * Finance service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class FinanceServiceImpl extends ServiceImpl<WithdrawalApplicationMapper, WithdrawalApplication> implements FinanceService {

    @Autowired
    private WithdrawalApplicationMapper withdrawalMapper;

    @Autowired
    private ReconciliationStatementMapper reconciliationMapper;

    @Autowired
    private ProfitAnalysisMapper profitAnalysisMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private CostService costService;

    // ==================== Withdrawal Management ====================

    @Override
    public List<WithdrawalApplication> getWithdrawalList(Integer status, LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        LambdaQueryWrapper<WithdrawalApplication> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(WithdrawalApplication::getStatus, status);
        }
        if (startDate != null) {
            qw.ge(WithdrawalApplication::getCreateTime, startDate);
        }
        if (endDate != null) {
            qw.le(WithdrawalApplication::getCreateTime, endDate);
        }
        if (tenantId != null) {
            qw.eq(WithdrawalApplication::getTenantId, tenantId);
        }
        qw.orderByDesc(WithdrawalApplication::getCreateTime);
        return withdrawalMapper.selectList(qw);
    }

    @Override
    public WithdrawalApplication getWithdrawalById(Long id) {
        return withdrawalMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createWithdrawal(WithdrawalApplication application) {
        application.setApplicationNo(generateApplicationNo());
        application.setStatus(WithdrawalApplication.STATUS_PENDING);
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        return withdrawalMapper.insert(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewWithdrawal(Long id, Integer status, Long reviewerId, String reviewerName, String remark) {
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null) {
            return false;
        }

        application.setStatus(status);
        application.setReviewerId(reviewerId);
        application.setReviewerName(reviewerName);
        application.setReviewTime(LocalDateTime.now());
        application.setReviewRemark(remark);
        application.setUpdateTime(LocalDateTime.now());

        return withdrawalMapper.updateById(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processWithdrawalPayment(Long id, String paymentNo) {
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null || application.getStatus() != WithdrawalApplication.STATUS_APPROVED) {
            return false;
        }

        application.setStatus(WithdrawalApplication.STATUS_PAID);
        application.setPaymentTime(LocalDateTime.now());
        application.setPaymentNo(paymentNo);
        application.setUpdateTime(LocalDateTime.now());

        return withdrawalMapper.updateById(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelWithdrawal(Long id) {
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null || application.getStatus() != WithdrawalApplication.STATUS_PENDING) {
            return false;
        }

        application.setStatus(WithdrawalApplication.STATUS_CANCELLED);
        application.setUpdateTime(LocalDateTime.now());

        return withdrawalMapper.updateById(application) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWithdrawal(Long id) {
        return withdrawalMapper.deleteById(id) > 0;
    }

    // ==================== Reconciliation Management ====================

    @Override
    public List<ReconciliationStatement> getReconciliationList(LocalDate startDate, LocalDate endDate, String platform, Long tenantId) {
        LambdaQueryWrapper<ReconciliationStatement> qw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            qw.ge(ReconciliationStatement::getStatementDate, startDate);
        }
        if (endDate != null) {
            qw.le(ReconciliationStatement::getStatementDate, endDate);
        }
        if (platform != null && !platform.isEmpty()) {
            qw.eq(ReconciliationStatement::getPlatform, platform);
        }
        if (tenantId != null) {
            qw.eq(ReconciliationStatement::getTenantId, tenantId);
        }
        qw.orderByDesc(ReconciliationStatement::getStatementDate);
        return reconciliationMapper.selectList(qw);
    }

    @Override
    public ReconciliationStatement getReconciliationById(Long id) {
        return reconciliationMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationStatement generateReconciliation(LocalDate date, String platform, Long tenantId) {
        // Check if already exists
        LambdaQueryWrapper<ReconciliationStatement> qw = new LambdaQueryWrapper<>();
        qw.eq(ReconciliationStatement::getStatementDate, date);
        qw.eq(ReconciliationStatement::getPlatform, platform);
        if (tenantId != null) {
            qw.eq(ReconciliationStatement::getTenantId, tenantId);
        }
        ReconciliationStatement existing = reconciliationMapper.selectOne(qw);
        if (existing != null) {
            return existing;
        }

        // Query orders for the date
        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.ge(Orders::getOrderTime, date.atStartOfDay());
        orderQw.le(Orders::getOrderTime, date.atTime(LocalTime.MAX));
        if (tenantId != null) {
            orderQw.eq(Orders::getTenantId, tenantId);
        }
        List<Orders> orders = orderService.list(orderQw);

        BigDecimal systemAmount = BigDecimal.ZERO;
        int orderCount = 0;
        BigDecimal refundAmount = BigDecimal.ZERO;
        int refundCount = 0;

        for (Orders order : orders) {
            if (order.getStatus() == Orders.STATUS_COMPLETED) {
                orderCount++;
                systemAmount = systemAmount.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
            } else if (order.getStatus() == Orders.STATUS_REFUNDED) {
                refundCount++;
                refundAmount = refundAmount.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
            }
        }

        // Create reconciliation statement
        ReconciliationStatement statement = new ReconciliationStatement();
        statement.setStatementNo("RC" + date.toString().replace("-", "") + System.currentTimeMillis() % 10000);
        statement.setStatementDate(date);
        statement.setPlatform(platform);
        statement.setSystemAmount(systemAmount);
        statement.setPlatformAmount(BigDecimal.ZERO); // To be filled manually
        statement.setDifferenceAmount(BigDecimal.ZERO);
        statement.setOrderCount(orderCount);
        statement.setRefundAmount(refundAmount);
        statement.setRefundCount(refundCount);
        statement.setFeeAmount(BigDecimal.ZERO);
        statement.setNetAmount(systemAmount.subtract(refundAmount));
        statement.setStatus(ReconciliationStatement.STATUS_UNRECONCILED);
        statement.setTenantId(tenantId);
        statement.setCreateTime(LocalDateTime.now());
        statement.setUpdateTime(LocalDateTime.now());

        reconciliationMapper.insert(statement);
        return statement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReconciliation(Long id, Long userId, String userName) {
        ReconciliationStatement statement = reconciliationMapper.selectById(id);
        if (statement == null) {
            return false;
        }

        // Calculate difference
        BigDecimal difference = statement.getSystemAmount().subtract(statement.getPlatformAmount());
        statement.setDifferenceAmount(difference);
        statement.setStatus(Math.abs(difference.doubleValue()) < 0.01 ?
                ReconciliationStatement.STATUS_RECONCILED : ReconciliationStatement.STATUS_DISCREPANCY);
        statement.setReconcileTime(LocalDateTime.now());
        statement.setReconcileUserId(userId);
        statement.setReconcileUserName(userName);
        statement.setUpdateTime(LocalDateTime.now());

        return reconciliationMapper.updateById(statement) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteReconciliation(Long id) {
        return reconciliationMapper.deleteById(id) > 0;
    }

    // ==================== Profit Analysis ====================

    @Override
    public List<ProfitAnalysis> getProfitAnalysisList(LocalDate startDate, LocalDate endDate, Long tenantId) {
        LambdaQueryWrapper<ProfitAnalysis> qw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            qw.ge(ProfitAnalysis::getAnalysisDate, startDate);
        }
        if (endDate != null) {
            qw.le(ProfitAnalysis::getAnalysisDate, endDate);
        }
        if (tenantId != null) {
            qw.eq(ProfitAnalysis::getTenantId, tenantId);
        }
        qw.orderByDesc(ProfitAnalysis::getAnalysisDate);
        return profitAnalysisMapper.selectList(qw);
    }

    @Override
    public ProfitAnalysis getProfitAnalysisByDate(LocalDate date, Long tenantId) {
        LambdaQueryWrapper<ProfitAnalysis> qw = new LambdaQueryWrapper<>();
        qw.eq(ProfitAnalysis::getAnalysisDate, date);
        if (tenantId != null) {
            qw.eq(ProfitAnalysis::getTenantId, tenantId);
        }
        return profitAnalysisMapper.selectOne(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProfitAnalysis generateProfitAnalysis(LocalDate date, Long tenantId) {
        // Check if already exists
        ProfitAnalysis existing = getProfitAnalysisByDate(date, tenantId);
        if (existing != null) {
            return existing;
        }

        // Query orders
        LambdaQueryWrapper<Orders> orderQw = new LambdaQueryWrapper<>();
        orderQw.ge(Orders::getOrderTime, date.atStartOfDay());
        orderQw.le(Orders::getOrderTime, date.atTime(LocalTime.MAX));
        if (tenantId != null) {
            orderQw.eq(Orders::getTenantId, tenantId);
        }
        List<Orders> orders = orderService.list(orderQw);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        int orderCount = 0;
        Set<Long> uniqueCustomers = new HashSet<>();

        for (Orders order : orders) {
            if (order.getStatus() == Orders.STATUS_COMPLETED) {
                orderCount++;
                totalRevenue = totalRevenue.add(order.getAmount() != null ? order.getAmount() : BigDecimal.ZERO);
                if (order.getUserId() != null) {
                    uniqueCustomers.add(order.getUserId());
                }
            }
        }

        // Query costs
        Map<String, Object> costSummary = costService.getCostSummary(date, date, tenantId);
        BigDecimal foodCost = (BigDecimal) costSummary.getOrDefault("materialCost", BigDecimal.ZERO);
        BigDecimal laborCost = (BigDecimal) costSummary.getOrDefault("laborCost", BigDecimal.ZERO);
        BigDecimal otherCost = (BigDecimal) costSummary.getOrDefault("otherCost", BigDecimal.ZERO);
        BigDecimal totalCost = foodCost.add(laborCost).add(otherCost);

        // Calculate profit
        BigDecimal grossProfit = totalRevenue.subtract(totalCost);
        BigDecimal grossProfitRate = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                grossProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : BigDecimal.ZERO;

        BigDecimal operatingExpense = BigDecimal.ZERO; // Simplified
        BigDecimal netProfit = grossProfit.subtract(operatingExpense);
        BigDecimal netProfitRate = totalRevenue.compareTo(BigDecimal.ZERO) > 0 ?
                netProfit.divide(totalRevenue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : BigDecimal.ZERO;

        BigDecimal averageOrderValue = orderCount > 0 ?
                totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Create profit analysis
        ProfitAnalysis analysis = new ProfitAnalysis();
        analysis.setAnalysisDate(date);
        analysis.setTotalRevenue(totalRevenue);
        analysis.setFoodCost(foodCost);
        analysis.setLaborCost(laborCost);
        analysis.setOtherCost(otherCost);
        analysis.setTotalCost(totalCost);
        analysis.setGrossProfit(grossProfit);
        analysis.setGrossProfitRate(grossProfitRate);
        analysis.setOperatingExpense(operatingExpense);
        analysis.setNetProfit(netProfit);
        analysis.setNetProfitRate(netProfitRate);
        analysis.setOrderCount(orderCount);
        analysis.setCustomerCount(uniqueCustomers.size());
        analysis.setAverageOrderValue(averageOrderValue);
        analysis.setTenantId(tenantId);
        analysis.setCreateTime(LocalDateTime.now());
        analysis.setUpdateTime(LocalDateTime.now());

        profitAnalysisMapper.insert(analysis);
        return analysis;
    }

    @Override
    public Map<String, Object> getProfitTrend(LocalDate startDate, LocalDate endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> revenues = new ArrayList<>();
        List<BigDecimal> costs = new ArrayList<>();
        List<BigDecimal> profits = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current.toString());

            ProfitAnalysis analysis = getProfitAnalysisByDate(current, tenantId);
            if (analysis != null) {
                revenues.add(analysis.getTotalRevenue());
                costs.add(analysis.getTotalCost());
                profits.add(analysis.getGrossProfit());
            } else {
                revenues.add(BigDecimal.ZERO);
                costs.add(BigDecimal.ZERO);
                profits.add(BigDecimal.ZERO);
            }

            current = current.plusDays(1);
        }

        result.put("dates", dates);
        result.put("revenues", revenues);
        result.put("costs", costs);
        result.put("profits", profits);

        return result;
    }

    @Override
    public Map<String, Object> getProfitStructure(LocalDate startDate, LocalDate endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        List<ProfitAnalysis> analyses = getProfitAnalysisList(startDate, endDate, tenantId);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalFoodCost = BigDecimal.ZERO;
        BigDecimal totalLaborCost = BigDecimal.ZERO;
        BigDecimal totalOtherCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        for (ProfitAnalysis analysis : analyses) {
            totalRevenue = totalRevenue.add(analysis.getTotalRevenue() != null ? analysis.getTotalRevenue() : BigDecimal.ZERO);
            totalFoodCost = totalFoodCost.add(analysis.getFoodCost() != null ? analysis.getFoodCost() : BigDecimal.ZERO);
            totalLaborCost = totalLaborCost.add(analysis.getLaborCost() != null ? analysis.getLaborCost() : BigDecimal.ZERO);
            totalOtherCost = totalOtherCost.add(analysis.getOtherCost() != null ? analysis.getOtherCost() : BigDecimal.ZERO);
            totalProfit = totalProfit.add(analysis.getGrossProfit() != null ? analysis.getGrossProfit() : BigDecimal.ZERO);
        }

        result.put("totalRevenue", totalRevenue);
        result.put("totalFoodCost", totalFoodCost);
        result.put("totalLaborCost", totalLaborCost);
        result.put("totalOtherCost", totalOtherCost);
        result.put("totalProfit", totalProfit);

        return result;
    }

    // ==================== Statistics ====================

    @Override
    public Map<String, Object> getFinanceStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // Withdrawal statistics
        LambdaQueryWrapper<WithdrawalApplication> withdrawalQw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            withdrawalQw.ge(WithdrawalApplication::getCreateTime, startDate);
        }
        if (endDate != null) {
            withdrawalQw.le(WithdrawalApplication::getCreateTime, endDate);
        }
        if (tenantId != null) {
            withdrawalQw.eq(WithdrawalApplication::getTenantId, tenantId);
        }
        List<WithdrawalApplication> withdrawals = withdrawalMapper.selectList(withdrawalQw);

        BigDecimal totalWithdrawal = BigDecimal.ZERO;
        int pendingCount = 0;
        int approvedCount = 0;
        int paidCount = 0;

        for (WithdrawalApplication withdrawal : withdrawals) {
            totalWithdrawal = totalWithdrawal.add(withdrawal.getAmount() != null ? withdrawal.getAmount() : BigDecimal.ZERO);
            if (withdrawal.getStatus() == WithdrawalApplication.STATUS_PENDING) pendingCount++;
            if (withdrawal.getStatus() == WithdrawalApplication.STATUS_APPROVED) approvedCount++;
            if (withdrawal.getStatus() == WithdrawalApplication.STATUS_PAID) paidCount++;
        }

        result.put("totalWithdrawal", totalWithdrawal);
        result.put("totalApplications", withdrawals.size());
        result.put("pendingCount", pendingCount);
        result.put("approvedCount", approvedCount);
        result.put("paidCount", paidCount);

        return result;
    }

    @Override
    public Map<String, Object> getWithdrawalStatistics(Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<WithdrawalApplication> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(WithdrawalApplication::getTenantId, tenantId);
        }
        List<WithdrawalApplication> withdrawals = withdrawalMapper.selectList(qw);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal paidAmount = BigDecimal.ZERO;
        Map<Integer, Integer> statusCountMap = new HashMap<>();

        for (WithdrawalApplication withdrawal : withdrawals) {
            totalAmount = totalAmount.add(withdrawal.getAmount() != null ? withdrawal.getAmount() : BigDecimal.ZERO);
            if (withdrawal.getStatus() == WithdrawalApplication.STATUS_PAID) {
                paidAmount = paidAmount.add(withdrawal.getAmount() != null ? withdrawal.getAmount() : BigDecimal.ZERO);
            }
            statusCountMap.merge(withdrawal.getStatus(), 1, Integer::sum);
        }

        result.put("totalAmount", totalAmount);
        result.put("paidAmount", paidAmount);
        result.put("statusCount", statusCountMap);

        return result;
    }

    // ==================== Private Methods ====================

    private String generateApplicationNo() {
        return "WD" + System.currentTimeMillis();
    }
}


