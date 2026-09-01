package com.reggie.module.withdraw.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.withdraw.model.WithdrawalRequest;
import com.reggie.module.withdraw.model.WithdrawalRecord;
import com.reggie.module.withdraw.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 提现管理控制器
 *
 * @author reggie
 * @since 2026-09-01
 */
@RequireEmployee
@RestController
@RequestMapping("/api/withdraw")
@Tag(name = "提现管理")
public class WithdrawalController {

    @Autowired
    private WithdrawalService withdrawalService;

    @PostMapping
    @Operation(summary = "用户提交提现申请")
    public R<WithdrawalRequest> submit(@Parameter(description = "提现申请信息（金额、收款方式等）", required = true) @RequestBody WithdrawalRequest request) {
        return R.success(withdrawalService.submitWithdrawal(request));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "同意提现")
    @Parameter(name = "id", description = "提现申请ID", required = true)
    public R<WithdrawalRequest> approve(@PathVariable Long id, @Parameter(description = "审批人ID", required = true) @RequestParam Long approveUserId) {
        return R.success(withdrawalService.approveWithdrawal(id, approveUserId));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "拒绝提现")
    @Parameter(name = "id", description = "提现申请ID", required = true)
    public R<WithdrawalRequest> reject(@PathVariable Long id, @Parameter(description = "拒绝原因", required = true) @RequestParam String rejectReason, @Parameter(description = "审批人ID", required = true) @RequestParam Long approveUserId) {
        return R.success(withdrawalService.rejectWithdrawal(id, rejectReason, approveUserId));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询提现申请")
    public R<Page<WithdrawalRequest>> page(
            @Parameter(description = "页码，从1开始", required = true) @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数，最大100", required = true) @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态（PENDING-待审核、APPROVED-已同意、REJECTED-已拒绝、CONFIRMED-已转账）") @RequestParam(required = false) String status) {
        return R.success(withdrawalService.listWithdrawals(page, pageSize, status));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认转账完成")
    @Parameter(name = "id", description = "提现申请ID", required = true)
    public R<WithdrawalRecord> confirm(
            @PathVariable Long id,
            @Parameter(description = "实际转账金额（元）", required = true) @RequestParam BigDecimal actualAmount,
            @Parameter(description = "手续费（元）", required = true) @RequestParam BigDecimal fee,
            @Parameter(description = "银行流水号", required = true) @RequestParam String bankTraceNo) {
        return R.success(withdrawalService.confirmTransfer(id, actualAmount, fee, bankTraceNo));
    }
}
