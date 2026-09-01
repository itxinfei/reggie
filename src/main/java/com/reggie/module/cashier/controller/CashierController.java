package com.reggie.module.cashier.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.auth.service.EmployeeService;
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
import javax.validation.Valid;

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
@RequireEmployee
public class CashierController {

    @Autowired
    private CashierService cashierService;

    @Autowired
    private EmployeeService employeeService;

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
    public R<CashierRecord> getCashierRecordByOrderId(@Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        CashierRecord record = cashierService.getCashierRecordByOrderId(orderId, tenantId);
        return R.success(record);
    }

    @PostMapping("/record")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "保存收银记录")
    public R<String> saveCashierRecord(@Parameter(description = "收银记录信息", required = true) @Valid @RequestBody CashierRecord cashierRecord) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();
        cashierRecord.setTenantId(tenantId);
        cashierRecord.setCreateUser(userId);
        boolean success = cashierService.saveCashierRecord(cashierRecord);
        return success ? R.success("保存成功") : R.error("保存失败");
    }

    @PostMapping("/cash-payment")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "收银收款")
    public R<CashierRecord> cashPayment(
                        @Parameter(description = "订单ID") @RequestParam Long orderId,
            @Parameter(description = "订单号") @RequestParam String orderNumber,
            @Parameter(description = "收银金额") @RequestParam BigDecimal amount,
            @Parameter(description = "实收金额（现金收银填写，其他支付方式等于收银金额）") @RequestParam BigDecimal actualAmount,
            @Parameter(description = "支付方式 1现金 2微信 3支付宝 4银行卡 5会员储值") @RequestParam(required = false, defaultValue = "1") Integer payType,
            @Parameter(description = "使用的优惠券ID（会员权益核销）") @RequestParam(required = false) Long usedCouponId,
            @Parameter(description = "会员关联用户ID（会员识别后传入，用于发放积分）") @RequestParam(required = false) Long memberUserId,
            @Parameter(description = "备注") @RequestParam(required = false) String remark) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();

        // P0-3：从员工表取真实姓名，不再硬编码"收银员"
        String cashierName = null;
        try {
            Employee emp = employeeService.getById(userId);
            if (emp != null && emp.getName() != null) {
                cashierName = emp.getName();
            }
        } catch (Exception ex) {
            log.warn("查询收银员姓名失败，userId={}", userId, ex);
        }
        cashierName = cashierName != null ? cashierName : "收银员";

        try {
            CashierRecord record = cashierService.cashPayment(orderId, orderNumber, amount, actualAmount,
                    payType, userId, cashierName, usedCouponId, memberUserId, remark);
            return R.success(record);
        } catch (CustomException e) {
            log.warn("收银收款业务错误：{}", e.getMessage(), e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("收银收款失败", e);
            return R.error("收银收款失败，请稍后重试");
        }
    }

    @DeleteMapping("/record/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除收银记录")
    public R<String> deleteCashierRecord(@Parameter(description = "收银记录ID", required = true) @PathVariable Long id) {
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
    public R<DailySettlement> getDailySettlementByDate(@Parameter(description = "结算日期，格式 yyyy-MM-dd", required = true) @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Long tenantId = BaseContext.getCurrentTenantId();
        DailySettlement settlement = cashierService.getDailySettlementByDate(date, tenantId);
        return R.success(settlement);
    }

    @PostMapping("/settlement/execute")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "执行日结")
    public R<DailySettlement> executeDailySettlement(
                        @Parameter(description = "结算日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate settlementDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Long userId = BaseContext.getCurrentId();

        try {
            DailySettlement settlement = cashierService.executeDailySettlement(settlementDate, userId, "操作员", tenantId);
            return R.success(settlement);
        } catch (CustomException e) {
            log.warn("日结业务错误：{}", e.getMessage(), e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("日结失败", e);
            return R.error("日结失败，请稍后重试");
        }
    }

    @PostMapping("/settlement/cancel")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "取消日结")
    public R<String> cancelDailySettlement(
                        @Parameter(description = "结算日期") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate settlementDate) {
        Long tenantId = BaseContext.getCurrentTenantId();

        try {
            boolean success = cashierService.cancelDailySettlement(settlementDate, tenantId);
            return success ? R.success("取消成功") : R.error("取消失败");
        } catch (CustomException e) {
            log.warn("取消日结业务错误：{}", e.getMessage(), e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("取消日结失败", e);
            return R.error("取消日结失败，请稍后重试");
        }
    }

    @DeleteMapping("/settlement/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除日结")
    public R<String> deleteDailySettlement(@Parameter(description = "日结记录ID", required = true) @PathVariable Long id) {
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





