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
import javax.validation.Valid;

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
@Tag(name = "财务管理")
@RequireEmployee
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    // ==================== Withdrawal Management ====================

    /**
     * 获取 withdrawal list。
     * @param status 参数 status
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/withdrawal/list")
    @Operation(summary = "提现申请分页查询")
    public R<List<WithdrawalApplication>> getWithdrawalList(
                        @Parameter(description = "提现状态（可选）") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern =
                    "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern =
                    "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<WithdrawalApplication> list = financeService.getWithdrawalList(status, startDate, endDate, tenantId);
        return R.success(list);
    }

    /**
     * 获取 withdrawal by id。
     * @param id 参数 id
     * @return 返回结果
     */
    @GetMapping("/withdrawal/{id}")
    @Operation(summary = "查询提现申请详情")
    public R<WithdrawalApplication> getWithdrawalById(@Parameter(description = "提现申请ID", required =
            true) @PathVariable Long id) {
        WithdrawalApplication application = financeService.getWithdrawalById(id);
        return R.success(application);
    }

    /**
     * 创建 withdrawal。
     * @param application 参数 application
     * @return 返回结果
     */
    @PostMapping("/withdrawal")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "提交提现申请")
    public R<String> createWithdrawal(@Parameter(description = "提现申请信息", required =
            true) @Valid @RequestBody WithdrawalApplication application) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        application.setTenantId(tenantId);
        application.setApplicantId(userId);
        boolean success = financeService.createWithdrawal(application);
        return success ? R.success("Created successfully") : R.error("Creation failed");
    }

    /**
     * 审核 withdrawal。
     * @param id 参数 id
     * @param status 参数 status
     * @param remark 参数 remark
     * @return 返回结果
     */
    @PostMapping("/withdrawal/{id}/review")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "审核提现申请")
    public R<String> reviewWithdrawal(
            @Parameter(description = "提现申请ID", required = true) @PathVariable Long id,
            @Parameter(description = "审核状态（1=通过，3=驳回）", required = true) @RequestParam Integer status,
            @Parameter(description = "审核备注（可选）") @RequestParam(required = false) String remark) {
        Long userId = BaseContext.getCurrentId();
        String userName = "Admin"; // Should get from user service
        boolean success = financeService.reviewWithdrawal(id, status, userId, userName, remark);
        return success ? R.success("Reviewed successfully") : R.error("Review failed");
    }

    /**
     * 处理 withdrawal payment。
     * @param id 参数 id
     * @param paymentNo 参数 paymentNo
     * @return 返回结果
     */
    @PostMapping("/withdrawal/{id}/payment")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "确认打款")
    public R<String> processWithdrawalPayment(
            @Parameter(description = "提现申请ID", required = true) @PathVariable Long id,
            @Parameter(description = "支付单号", required = true) @RequestParam String paymentNo) {
        boolean success = financeService.processWithdrawalPayment(id, paymentNo);
        return success ? R.success("Payment processed") : R.error("Payment failed");
    }

    /**
     * 取消 withdrawal。
     * @param id 参数 id
     * @return 返回结果
     */
    @PostMapping("/withdrawal/{id}/cancel")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "取消提现申请")
    public R<String> cancelWithdrawal(@Parameter(description = "提现申请ID", required = true) @PathVariable Long id) {
        boolean success = financeService.cancelWithdrawal(id);
        return success ? R.success("Cancelled") : R.error("Cancellation failed");
    }

    /**
     * 删除 withdrawal。
     * @param id 参数 id
     * @return 返回结果
     */
    @DeleteMapping("/withdrawal/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除提现申请")
    public R<String> deleteWithdrawal(@Parameter(description = "提现申请ID", required = true) @PathVariable Long id) {
        boolean success = financeService.deleteWithdrawal(id);
        return success ? R.success("Deleted") : R.error("Deletion failed");
    }

    // ==================== Reconciliation Management ====================

    /**
     * 获取 reconciliation list。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param platform 参数 platform
     * @return 返回结果
     */
    @GetMapping("/reconciliation/list")
    @Operation(summary = "对账单分页查询")
    public R<List<ReconciliationStatement>> getReconciliationList(
                        @Parameter(description = "开始日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern =
                                "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern =
                    "yyyy-MM-dd") LocalDate endDate,
            @Parameter(description = "平台（可选）") @RequestParam(required = false) String platform) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<ReconciliationStatement> list = financeService.getReconciliationList(startDate, endDate, platform,
                tenantId);
        return R.success(list);
    }

    /**
     * 获取 reconciliation by id。
     * @param id 参数 id
     * @return 返回结果
     */
    @GetMapping("/reconciliation/{id}")
    @Operation(summary = "查询对账单详情")
    public R<ReconciliationStatement> getReconciliationById(@Parameter(description = "对账单ID", required =
            true) @PathVariable Long id) {
        ReconciliationStatement statement = financeService.getReconciliationById(id);
        return R.success(statement);
    }

    /**
     * 生成 reconciliation。
     * @param date 参数 date
     * @param platform 参数 platform
     * @return 返回结果
     */
    @PostMapping("/reconciliation/generate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "生成对账单")
    public R<ReconciliationStatement> generateReconciliation(
                        @Parameter(description = "对账日期，格式yyyy-MM-dd", required =
                                true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @Parameter(description = "平台（默认all）", required = false) @RequestParam(defaultValue =
                    "all") String platform) {
        Long tenantId = BaseContext.getCurrentTenantId();
        ReconciliationStatement statement = financeService.generateReconciliation(date, platform, tenantId);
        return R.success(statement);
    }

    /**
     * 确认 reconciliation。
     * @param id 参数 id
     * @return 返回结果
     */
    @PostMapping("/reconciliation/{id}/confirm")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "确认对账单")
    public R<String> confirmReconciliation(@Parameter(description = "对账单ID", required = true) @PathVariable Long id) {
        Long userId = BaseContext.getCurrentId();
        String userName = "Admin";
        boolean success = financeService.confirmReconciliation(id, userId, userName);
        return success ? R.success("Confirmed") : R.error("Confirmation failed");
    }

    /**
     * 删除 reconciliation。
     * @param id 参数 id
     * @return 返回结果
     */
    @DeleteMapping("/reconciliation/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除对账单")
    public R<String> deleteReconciliation(@Parameter(description = "对账单ID", required = true) @PathVariable Long id) {
        boolean success = financeService.deleteReconciliation(id);
        return success ? R.success("Deleted") : R.error("Deletion failed");
    }

    // ==================== Profit Analysis ====================

    /**
     * 获取 profit analysis list。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/profit/list")
    @Operation(summary = "利润分析分页查询")
    public R<List<ProfitAnalysis>> getProfitAnalysisList(
                        @Parameter(description = "开始日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern =
                                "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern =
                    "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<ProfitAnalysis> list = financeService.getProfitAnalysisList(startDate, endDate, tenantId);
        return R.success(list);
    }

    /**
     * 获取 profit analysis by date。
     * @param date 参数 date
     * @return 返回结果
     */
    @GetMapping("/profit/date/{date}")
    @Operation(summary = "按日期查询利润分析")
    public R<ProfitAnalysis> getProfitAnalysisByDate(@Parameter(description = "利润日期，格式yyyy-MM-dd", required =
            true) @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long tenantId = BaseContext.getCurrentTenantId();
        ProfitAnalysis analysis = financeService.getProfitAnalysisByDate(date, tenantId);
        return R.success(analysis);
    }

    /**
     * 生成 profit analysis。
     * @param date 参数 date
     * @return 返回结果
     */
    @PostMapping("/profit/generate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "生成利润分析")
    public R<ProfitAnalysis> generateProfitAnalysis(
                        @Parameter(description = "利润日期，格式yyyy-MM-dd", required =
                                true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long tenantId = BaseContext.getCurrentTenantId();
        ProfitAnalysis analysis = financeService.generateProfitAnalysis(date, tenantId);
        return R.success(analysis);
    }

    /**
     * 获取 profit trend。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/profit/trend")
    @Operation(summary = "利润趋势")
    public R<Map<String, Object>> getProfitTrend(
                        @Parameter(description = "开始日期，格式yyyy-MM-dd", required =
                                true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式yyyy-MM-dd", required = true) @RequestParam @DateTimeFormat(pattern =
                    "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = financeService.getProfitTrend(startDate, endDate, tenantId);
        return R.success(trend);
    }

    /**
     * 获取 profit structure。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/profit/structure")
    @Operation(summary = "利润结构")
    public R<Map<String, Object>> getProfitStructure(
                        @Parameter(description = "开始日期，格式yyyy-MM-dd", required =
                                true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期，格式yyyy-MM-dd", required = true) @RequestParam @DateTimeFormat(pattern =
                    "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> structure = financeService.getProfitStructure(startDate, endDate, tenantId);
        return R.success(structure);
    }

    // ==================== Statistics ====================

    /**
     * 获取 finance statistics。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @GetMapping("/statistics")
    @Operation(summary = "财务统计")
    public R<Map<String, Object>> getFinanceStatistics(
                        @Parameter(description = "开始日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern =
                                "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期（可选）") @RequestParam(required = false) @DateTimeFormat(pattern =
                    "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = financeService.getFinanceStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    /**
     * 获取 withdrawal statistics。
     * @return 返回结果
     */
    @GetMapping("/withdrawal/statistics")
    @Operation(summary = "提现统计")
    public R<Map<String, Object>> getWithdrawalStatistics() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = financeService.getWithdrawalStatistics(tenantId);
        return R.success(statistics);
    }
}



