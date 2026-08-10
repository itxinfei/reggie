package com.reggie.module.finance.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.finance.model.WithdrawalApplication;
import com.reggie.module.finance.model.ReconciliationStatement;
import com.reggie.module.finance.model.ProfitAnalysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Finance Service Interface
 *
 * @author reggie
 * @since 2026-08-11
 */
public interface FinanceService extends IService<WithdrawalApplication> {

    // ==================== Withdrawal Management ====================

    /**
     * Get withdrawal application list
     *
     * @param status   Status filter
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Withdrawal application list
     */
    List<WithdrawalApplication> getWithdrawalList(Integer status, LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * Get withdrawal application by ID
     *
     * @param id Application ID
     * @return Withdrawal application
     */
    WithdrawalApplication getWithdrawalById(Long id);

    /**
     * Create withdrawal application
     *
     * @param application Withdrawal application
     * @return Success or not
     */
    boolean createWithdrawal(WithdrawalApplication application);

    /**
     * Review withdrawal application
     *
     * @param id         Application ID
     * @param status     New status (approved/rejected)
     * @param reviewerId Reviewer ID
     * @param reviewerName Reviewer name
     * @param remark     Review remark
     * @return Success or not
     */
    boolean reviewWithdrawal(Long id, Integer status, Long reviewerId, String reviewerName, String remark);

    /**
     * Process withdrawal payment
     *
     * @param id        Application ID
     * @param paymentNo Payment number
     * @return Success or not
     */
    boolean processWithdrawalPayment(Long id, String paymentNo);

    /**
     * Cancel withdrawal application
     *
     * @param id Application ID
     * @return Success or not
     */
    boolean cancelWithdrawal(Long id);

    /**
     * Delete withdrawal application
     *
     * @param id Application ID
     * @return Success or not
     */
    boolean deleteWithdrawal(Long id);

    // ==================== Reconciliation Management ====================

    /**
     * Get reconciliation statement list
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param platform  Platform filter
     * @param tenantId  Tenant ID
     * @return Reconciliation statement list
     */
    List<ReconciliationStatement> getReconciliationList(LocalDate startDate, LocalDate endDate, String platform, Long tenantId);

    /**
     * Get reconciliation statement by ID
     *
     * @param id Statement ID
     * @return Reconciliation statement
     */
    ReconciliationStatement getReconciliationById(Long id);

    /**
     * Generate reconciliation statement
     *
     * @param date     Statement date
     * @param platform Platform
     * @param tenantId Tenant ID
     * @return Reconciliation statement
     */
    ReconciliationStatement generateReconciliation(LocalDate date, String platform, Long tenantId);

    /**
     * Confirm reconciliation
     *
     * @param id       Statement ID
     * @param userId   User ID
     * @param userName User name
     * @return Success or not
     */
    boolean confirmReconciliation(Long id, Long userId, String userName);

    /**
     * Delete reconciliation statement
     *
     * @param id Statement ID
     * @return Success or not
     */
    boolean deleteReconciliation(Long id);

    // ==================== Profit Analysis ====================

    /**
     * Get profit analysis list
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Profit analysis list
     */
    List<ProfitAnalysis> getProfitAnalysisList(LocalDate startDate, LocalDate endDate, Long tenantId);

    /**
     * Get profit analysis by date
     *
     * @param date     Analysis date
     * @param tenantId Tenant ID
     * @return Profit analysis
     */
    ProfitAnalysis getProfitAnalysisByDate(LocalDate date, Long tenantId);

    /**
     * Generate profit analysis
     *
     * @param date     Analysis date
     * @param tenantId Tenant ID
     * @return Profit analysis
     */
    ProfitAnalysis generateProfitAnalysis(LocalDate date, Long tenantId);

    /**
     * Get profit trend
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Profit trend data
     */
    Map<String, Object> getProfitTrend(LocalDate startDate, LocalDate endDate, Long tenantId);

    /**
     * Get profit structure
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Profit structure data
     */
    Map<String, Object> getProfitStructure(LocalDate startDate, LocalDate endDate, Long tenantId);

    // ==================== Statistics ====================

    /**
     * Get finance statistics
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Finance statistics
     */
    Map<String, Object> getFinanceStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * Get withdrawal statistics
     *
     * @param tenantId Tenant ID
     * @return Withdrawal statistics
     */
    Map<String, Object> getWithdrawalStatistics(Long tenantId);
}
