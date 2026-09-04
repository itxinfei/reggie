package com.reggie.module.finance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finance Service Implementation
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

    /**
     * 按 tenantId+platform+date 串行化对账生成请求，防止并发重复生成（TOCTOU）
     */
    private final ConcurrentHashMap<String, Object> reconciliationLock = new ConcurrentHashMap<>();

    /**
     * 按 tenantId+date 串行化利润分析生成请求，防止并发重复生成（TOCTOU）
     */
    private final ConcurrentHashMap<String, Object> profitAnalysisLock = new ConcurrentHashMap<>();

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
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null) {
            return null;
        }
        // 租户隔离校验：防止跨租户读取提现单敏感信息（租户缺失时 fail-closed）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new IllegalArgumentException("租户信息缺失，无法查看提现单");
        }
        if (!currentTenantId.equals(application.getTenantId())) {
            throw new IllegalArgumentException("无权查看其他租户的提现单");
        }
        return application;
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
        // 租户隔离校验：禁止跨租户审批（租户缺失时 fail-closed）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new IllegalArgumentException("租户信息缺失，无法审批提现申请");
        }
        if (!currentTenantId.equals(application.getTenantId())) {
            throw new IllegalArgumentException("无权审批其他租户的提现申请");
        }
        // 状态机校验：仅待审批状态可审批
        if (application.getStatus() != WithdrawalApplication.STATUS_PENDING) {
            throw new IllegalArgumentException("仅待审批状态的提现申请可审批");
        }
        // 审批结果校验：仅允许审批通过或拒绝
        if (status != WithdrawalApplication.STATUS_APPROVED && status != WithdrawalApplication.STATUS_REJECTED) {
            throw new IllegalArgumentException("无效的审批结果");
        }

        // 并发防护（P1）：先 CAS 抢占 PENDING -> 目标状态，仅 affected rows=1 的事务可完成审批。
        // 此前 SELECT 校验 + updateById 无 CAS：并发双审批可同时通过 PENDING 校验并各自覆盖更新，
        // 且 approve 后资金扣减/出账无状态互斥。现条件更新互斥，第二个请求 rows=0 返回失败。
        int claimed = withdrawalMapper.update(null, new LambdaUpdateWrapper<WithdrawalApplication>()
                .eq(WithdrawalApplication::getId, id)
                .eq(WithdrawalApplication::getStatus, WithdrawalApplication.STATUS_PENDING)
                .set(WithdrawalApplication::getStatus, status)
                .set(WithdrawalApplication::getReviewerId, reviewerId)
                .set(WithdrawalApplication::getReviewerName, reviewerName)
                .set(WithdrawalApplication::getReviewTime, LocalDateTime.now())
                .set(WithdrawalApplication::getReviewRemark, remark)
                .set(WithdrawalApplication::getUpdateTime, LocalDateTime.now()));
        if (claimed == 0) {
            throw new IllegalArgumentException("提现申请状态已变更，请勿重复审批");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean processWithdrawalPayment(Long id, String paymentNo) {
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null) {
            return false;
        }
        // 租户隔离校验：禁止跨租户付款（租户缺失时 fail-closed）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new IllegalArgumentException("租户信息缺失，无法操作提现单");
        }
        if (!currentTenantId.equals(application.getTenantId())) {
            throw new IllegalArgumentException("无权操作其他租户的提现单");
        }
        // 状态机校验：仅已审批状态可付款
        if (application.getStatus() != WithdrawalApplication.STATUS_APPROVED) {
            throw new IllegalArgumentException("仅已审批状态的提现申请可付款");
        }

        // 并发防护（P1）：先 CAS 抢占 APPROVED -> PAID，仅 affected rows=1 的事务可完成付款。
        // 此前 SELECT 校验 + updateById 无 CAS：并发双付款（或付款+取消竞态）可同时通过校验并各自
        // 覆盖更新，导致同一提现单被重复出账。现条件更新互斥，第二个请求 rows=0 直接拒绝。
        int claimed = withdrawalMapper.update(null, new LambdaUpdateWrapper<WithdrawalApplication>()
                .eq(WithdrawalApplication::getId, id)
                .eq(WithdrawalApplication::getStatus, WithdrawalApplication.STATUS_APPROVED)
                .set(WithdrawalApplication::getStatus, WithdrawalApplication.STATUS_PAID)
                .set(WithdrawalApplication::getPaymentTime, LocalDateTime.now())
                .set(WithdrawalApplication::getPaymentNo, paymentNo)
                .set(WithdrawalApplication::getUpdateTime, LocalDateTime.now()));
        if (claimed == 0) {
            throw new IllegalArgumentException("提现申请状态已变更，请勿重复付款");
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelWithdrawal(Long id) {
        WithdrawalApplication application = withdrawalMapper.selectById(id);
        if (application == null) {
            return false;
        }
        // 租户隔离校验：禁止跨租户取消（租户缺失时 fail-closed）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new IllegalArgumentException("租户信息缺失，无法操作提现单");
        }
        if (!currentTenantId.equals(application.getTenantId())) {
            throw new IllegalArgumentException("无权操作其他租户的提现单");
        }
        if (application.getStatus() != WithdrawalApplication.STATUS_PENDING) {
            throw new IllegalArgumentException("仅待审批状态的提现申请可取消");
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
        ReconciliationStatement statement = reconciliationMapper.selectById(id);
        if (statement == null) {
            return null;
        }
        // 租户归属校验：租户缺失时 fail-closed（禁止跨租户越权查看对账单）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new CustomException("租户信息缺失，无法查看对账单");
        }
        if (!currentTenantId.equals(statement.getTenantId())) {
            throw new CustomException("无权查看其他租户的对账单");
        }
        return statement;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationStatement generateReconciliation(LocalDate date, String platform, Long tenantId) {
        // 按 tenantId+platform+date 串行化对账生成请求，防止并发重复生成（TOCTOU）
        String lockKey = (tenantId != null ? tenantId.toString() : "0") + ":" + platform + ":" + date;
        Object lock = reconciliationLock.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean confirmReconciliation(Long id, Long userId, String userName) {
        ReconciliationStatement statement = reconciliationMapper.selectById(id);
        if (statement == null) {
            return false;
        }
        // 租户归属校验：租户缺失时 fail-closed（不允许确认他人对账单）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new CustomException("租户信息缺失，无法操作对账单");
        }
        if (!currentTenantId.equals(statement.getTenantId())) {
            throw new CustomException("无权确认其他租户的对账单");
        }

        // Calculate difference
        // 防御性 null 检查：systemAmount/platformAmount 可能在数据库中为 null（历史数据）
        BigDecimal systemAmount = statement.getSystemAmount() != null ? statement.getSystemAmount() : BigDecimal.ZERO;
        BigDecimal platformAmount = statement.getPlatformAmount() != null ? statement.getPlatformAmount() : BigDecimal.ZERO;
        BigDecimal difference = systemAmount.subtract(platformAmount);
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
        ReconciliationStatement statement = reconciliationMapper.selectById(id);
        if (statement == null) {
            return false;
        }
        // 租户归属校验：租户缺失时 fail-closed
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            throw new CustomException("租户信息缺失，无法操作对账单");
        }
        if (!currentTenantId.equals(statement.getTenantId())) {
            throw new CustomException("无权删除其他租户的对账单");
        }
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
        // 按 tenantId+date 串行化利润分析生成请求，防止并发重复生成（TOCTOU）
        String lockKey = (tenantId != null ? tenantId.toString() : "0") + ":" + date;
        Object lock = profitAnalysisLock.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
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
            // 类型安全取值：getOrDefault 若命中 key 但 value 非 BigDecimal（如 Integer/Long），
            // 直接强转 ClassCastException。此处走安全转换路径，并对 null 兜底。
            BigDecimal foodCost = toBigDecimal(costSummary.get("materialCost"));
            BigDecimal laborCost = toBigDecimal(costSummary.get("laborCost"));
            BigDecimal otherCost = toBigDecimal(costSummary.get("otherCost"));
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

    /**
     * 类型安全的 BigDecimal 取值：null 返回 ZERO；BigDecimal 直接返回；其他 Number 走 toString 构造，
     * 避免 (BigDecimal) 强转 ClassCastException 与 new BigDecimal(doubleValue()) 精度陷阱。
     */
    private BigDecimal toBigDecimal(Object val) {
        if (val == null) {
            return BigDecimal.ZERO;
        }
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            try {
                return new BigDecimal(val.toString());
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }
}


