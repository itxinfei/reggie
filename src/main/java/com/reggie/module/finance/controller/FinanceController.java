package com.reggie.module.finance.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.finance.model.WithdrawalApplication;
import com.reggie.module.finance.model.ReconciliationStatement;
import com.reggie.module.finance.model.ProfitAnalysis;
import com.reggie.module.finance.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Finance Controller
 *
 * @author reggie
 * @since 2026-08-11
 */
@RestController
@RequestMapping("/finance")
@Tag(name = "Finance Management")
@RequireEmployee
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    // ==================== Withdrawal Management ====================

    @GetMapping("/withdrawal/list")
    @Operation(summary = "Get withdrawal list")
    public R<List<WithdrawalApplication>> getWithdrawalList(
                        @Parameter(description = "Status") @RequestParam(required = false) Integer status,
            @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<WithdrawalApplication> list = financeService.getWithdrawalList(status, startDate, endDate, tenantId);
        return R.success(list);
    }

    @GetMapping("/withdrawal/{id}")
    @Operation(summary = "Get withdrawal by ID")
    public R<WithdrawalApplication> getWithdrawalById(@PathVariable Long id) {
        WithdrawalApplication application = financeService.getWithdrawalById(id);
        return R.success(application);
    }

    @PostMapping("/withdrawal")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Create withdrawal application")
    public R<String> createWithdrawal(@RequestBody WithdrawalApplication application) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        application.setTenantId(tenantId);
        application.setApplicantId(userId);
        boolean success = financeService.createWithdrawal(application);
        return success ? R.success("Created successfully") : R.error("Creation failed");
    }

    @PostMapping("/withdrawal/{id}/review")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Review withdrawal application")
    public R<String> reviewWithdrawal(
            @Parameter(description = "ID")
            @PathVariable Long id,
            @Parameter(description = "Status (1=approved, 3=rejected)") @RequestParam Integer status,
            @Parameter(description = "Review remark") @RequestParam(required = false) String remark) {
        Long userId = BaseContext.getCurrentId();
        String userName = "Admin"; // Should get from user service
        boolean success = financeService.reviewWithdrawal(id, status, userId, userName, remark);
        return success ? R.success("Reviewed successfully") : R.error("Review failed");
    }

    @PostMapping("/withdrawal/{id}/payment")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Process withdrawal payment")
    public R<String> processWithdrawalPayment(
            @Parameter(description = "ID")
            @PathVariable Long id,
            @Parameter(description = "Payment number") @RequestParam String paymentNo) {
        boolean success = financeService.processWithdrawalPayment(id, paymentNo);
        return success ? R.success("Payment processed") : R.error("Payment failed");
    }

    @PostMapping("/withdrawal/{id}/cancel")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Cancel withdrawal")
    public R<String> cancelWithdrawal(@PathVariable Long id) {
        boolean success = financeService.cancelWithdrawal(id);
        return success ? R.success("Cancelled") : R.error("Cancellation failed");
    }

    @DeleteMapping("/withdrawal/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Delete withdrawal")
    public R<String> deleteWithdrawal(@PathVariable Long id) {
        boolean success = financeService.deleteWithdrawal(id);
        return success ? R.success("Deleted") : R.error("Deletion failed");
    }

    // ==================== Reconciliation Management ====================

    @GetMapping("/reconciliation/list")
    @Operation(summary = "Get reconciliation list")
    public R<List<ReconciliationStatement>> getReconciliationList(
                        @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "Platform") @RequestParam(required = false) String platform) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<ReconciliationStatement> list = financeService.getReconciliationList(startDate, endDate, platform, tenantId);
        return R.success(list);
    }

    @GetMapping("/reconciliation/{id}")
    @Operation(summary = "Get reconciliation by ID")
    public R<ReconciliationStatement> getReconciliationById(@PathVariable Long id) {
        ReconciliationStatement statement = financeService.getReconciliationById(id);
        return R.success(statement);
    }

    @PostMapping("/reconciliation/generate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Generate reconciliation")
    public R<ReconciliationStatement> generateReconciliation(
                        @Parameter(description = "Date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @Parameter(description = "Platform") @RequestParam(defaultValue = "all") String platform) {
        Long tenantId = BaseContext.getCurrentTenantId();
        ReconciliationStatement statement = financeService.generateReconciliation(date, platform, tenantId);
        return R.success(statement);
    }

    @PostMapping("/reconciliation/{id}/confirm")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Confirm reconciliation")
    public R<String> confirmReconciliation(@PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        String userName = "Admin";
        boolean success = financeService.confirmReconciliation(id, userId, userName);
        return success ? R.success("Confirmed") : R.error("Confirmation failed");
    }

    @DeleteMapping("/reconciliation/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Delete reconciliation")
    public R<String> deleteReconciliation(@PathVariable Long id) {
        boolean success = financeService.deleteReconciliation(id);
        return success ? R.success("Deleted") : R.error("Deletion failed");
    }

    // ==================== Profit Analysis ====================

    @GetMapping("/profit/list")
    @Operation(summary = "Get profit analysis list")
    public R<List<ProfitAnalysis>> getProfitAnalysisList(
                        @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<ProfitAnalysis> list = financeService.getProfitAnalysisList(startDate, endDate, tenantId);
        return R.success(list);
    }

    @GetMapping("/profit/date/{date}")
    @Operation(summary = "Get profit analysis by date")
    @Parameter(description = "Date")
    public R<ProfitAnalysis> getProfitAnalysisByDate(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long tenantId = BaseContext.getCurrentTenantId();
        ProfitAnalysis analysis = financeService.getProfitAnalysisByDate(date, tenantId);
        return R.success(analysis);
    }

    @PostMapping("/profit/generate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Generate profit analysis")
    public R<ProfitAnalysis> generateProfitAnalysis(
                        @Parameter(description = "Date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long tenantId = BaseContext.getCurrentTenantId();
        ProfitAnalysis analysis = financeService.generateProfitAnalysis(date, tenantId);
        return R.success(analysis);
    }

    @GetMapping("/profit/trend")
    @Operation(summary = "Get profit trend")
    public R<Map<String, Object>> getProfitTrend(
                        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = financeService.getProfitTrend(startDate, endDate, tenantId);
        return R.success(trend);
    }

    @GetMapping("/profit/structure")
    @Operation(summary = "Get profit structure")
    public R<Map<String, Object>> getProfitStructure(
                        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> structure = financeService.getProfitStructure(startDate, endDate, tenantId);
        return R.success(structure);
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    @Operation(summary = "Get finance statistics")
    public R<Map<String, Object>> getFinanceStatistics(
                        @Parameter(description = "Start date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = financeService.getFinanceStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    @GetMapping("/withdrawal/statistics")
    @Operation(summary = "Get withdrawal statistics")
    public R<Map<String, Object>> getWithdrawalStatistics() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = financeService.getWithdrawalStatistics(tenantId);
        return R.success(statistics);
    }
}



