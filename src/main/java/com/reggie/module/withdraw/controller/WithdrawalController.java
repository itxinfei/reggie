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
    public R<WithdrawalRequest> submit(@RequestBody WithdrawalRequest request) {
        return R.success(withdrawalService.submitWithdrawal(request));
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "同意提现")
    @Parameter(name = "id", description = "提现申请ID", required = true)
    public R<WithdrawalRequest> approve(@PathVariable Long id, @RequestParam Long approveUserId) {
        return R.success(withdrawalService.approveWithdrawal(id, approveUserId));
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "拒绝提现")
    @Parameter(name = "id", description = "提现申请ID", required = true)
    public R<WithdrawalRequest> reject(@PathVariable Long id, @RequestParam String rejectReason, @RequestParam Long approveUserId) {
        return R.success(withdrawalService.rejectWithdrawal(id, rejectReason, approveUserId));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询提现申请")
    public R<Page<WithdrawalRequest>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        return R.success(withdrawalService.listWithdrawals(page, pageSize, status));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "确认转账完成")
    @Parameter(name = "id", description = "提现申请ID", required = true)
    public R<WithdrawalRecord> confirm(
            @PathVariable Long id,
            @RequestParam BigDecimal actualAmount,
            @RequestParam BigDecimal fee,
            @RequestParam String bankTraceNo) {
        return R.success(withdrawalService.confirmTransfer(id, actualAmount, fee, bankTraceNo));
    }
}
