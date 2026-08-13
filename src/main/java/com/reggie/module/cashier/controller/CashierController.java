package com.reggie.module.cashier.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.cashier.model.CashierRecord;
import com.reggie.module.cashier.model.DailySettlement;
import com.reggie.module.cashier.service.CashierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 收银控制器
 *
 * @author reggie
 * @since 2026-08-10
 */
@Slf4j
@RestController
@RequestMapping("/cashier")
@Tag(name = "收银管理")
public class CashierController {

    @Autowired
    private CashierService cashierService;

    // ==================== 收银记录管理 ====================

    @GetMapping("/record/list")
    @Operation(summary = "获取收银记录列表")
    public R<List<CashierRecord>> getCashierRecordList(
                        @Parameter(description = "支付类型") @RequestParam(required = false) Integer payType,
            @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<CashierRecord> list = cashierService.getCashierRecordList(payType, startDate, endDate, tenantId);
        return R.success(list);
    }

    @GetMapping("/record/order/{orderId}")
    @Operation(summary = "根据订单ID获取收银记录")
    @Parameter(description = "OrderId")
    public R<CashierRecord> getCashierRecordByOrderId(@PathVariable Long orderId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        CashierRecord record = cashierService.getCashierRecordByOrderId(orderId, tenantId);
        return R.success(record);
    }

    @PostMapping("/record")
    @Operation(summary = "保存收银记录")
    public R<String> saveCashierRecord(@RequestBody CashierRecord cashierRecord) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        cashierRecord.setTenantId(tenantId);
        cashierRecord.setCreateUser(userId);
        boolean success = cashierService.saveCashierRecord(cashierRecord);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @PostMapping("/cash-payment")
    @Operation(summary = "现金收银")
    public R<CashierRecord> cashPayment(
                        @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "订单号") @RequestParam String orderNumber,
            @Parameter(description = "收银金额") @RequestParam BigDecimal amount,
            @Parameter(description = "实收金额") @RequestParam BigDecimal actualAmount,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();

        try {
            CashierRecord record = cashierService.cashPayment(orderId, orderNumber, amount, actualAmount,
                    userId, "收银员", remark);
            return R.success(record);
        } catch (Exception e) {
            log.error("现金收银失败", e);
            return R.error(e.getMessage());
        }
    }

    @DeleteMapping("/record/{id}")
    @Operation(summary = "删除收银记录")
    @Parameter(description = "I d")
    public R<String> deleteCashierRecord(@PathVariable Long id) {
        boolean success = cashierService.deleteCashierRecord(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    // ==================== 日结管理 ====================

    @GetMapping("/settlement/list")
    @Operation(summary = "获取日结列表")
    public R<List<DailySettlement>> getDailySettlementList(
                        @Parameter(description = "开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<DailySettlement> list = cashierService.getDailySettlementList(startDate, endDate, tenantId);
        return R.success(list);
    }

    @GetMapping("/settlement/date/{date}")
    @Operation(summary = "根据日期获取日结")
    @Parameter(description = "Date")
    public R<DailySettlement> getDailySettlementByDate(@PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long tenantId = BaseContext.getCurrentTenantId();
        DailySettlement settlement = cashierService.getDailySettlementByDate(date, tenantId);
        return R.success(settlement);
    }

    @PostMapping("/settlement/execute")
    @Operation(summary = "执行日结")
    public R<DailySettlement> executeDailySettlement(
                        @Parameter(description = "结算日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate settlementDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();

        try {
            DailySettlement settlement = cashierService.executeDailySettlement(settlementDate, userId, "操作员", tenantId);
            return R.success(settlement);
        } catch (Exception e) {
            log.error("日结失败", e);
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/settlement/cancel")
    @Operation(summary = "取消日结")
    public R<String> cancelDailySettlement(
                        @Parameter(description = "结算日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate settlementDate) {
        Long tenantId = BaseContext.getCurrentTenantId();

        try {
            boolean success = cashierService.cancelDailySettlement(settlementDate, tenantId);
            return success ? R.success("取消成功") : R.error("取消失败");
        } catch (Exception e) {
            log.error("取消日结失败", e);
            return R.error(e.getMessage());
        }
    }

    @DeleteMapping("/settlement/{id}")
    @Operation(summary = "删除日结")
    @Parameter(description = "I d")
    public R<String> deleteDailySettlement(@PathVariable Long id) {
        boolean success = cashierService.deleteDailySettlement(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

    // ==================== 统计分析 ====================

    @GetMapping("/statistics")
    @Operation(summary = "获取收银统计")
    public R<Map<String, Object>> getCashierStatistics(
                        @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = cashierService.getCashierStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    @GetMapping("/statistics/payment-type")
    @Operation(summary = "获取支付方式统计")
    public R<Map<String, Object>> getPaymentTypeStatistics(
                        @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = cashierService.getPaymentTypeStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    @GetMapping("/trend")
    @Operation(summary = "获取收银趋势")
    public R<Map<String, Object>> getCashierTrend(
                        @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> trend = cashierService.getCashierTrend(startDate, endDate, tenantId);
        return R.success(trend);
    }

    @GetMapping("/settlement/summary")
    @Operation(summary = "获取日结汇总")
    public R<Map<String, Object>> getDailySettlementSummary(
                        @Parameter(description = "开始日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> summary = cashierService.getDailySettlementSummary(startDate, endDate, tenantId);
        return R.success(summary);
    }
}





