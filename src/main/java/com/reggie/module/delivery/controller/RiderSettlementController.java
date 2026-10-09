package com.reggie.module.delivery.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireRider;
import com.reggie.module.delivery.model.RiderAccount;
import com.reggie.module.delivery.model.RiderWithdrawal;
import com.reggie.module.delivery.service.RiderSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 骑手端结算与提现控制器。
 *
 * @author reggie
 * @since 2026-09-28
 */
@RestController
@RequestMapping("/api/rider/settlement")
@Tag(name = "骑手结算", description = "骑手端账户余额与提现")
public class RiderSettlementController {

    @Autowired
    private RiderSettlementService riderSettlementService;

    /**
     * 骑手账户（可提现余额 / 冻结 / 累计收入 / 累计已提现）。
     */
    @GetMapping("/balance")
    @RequireRider
    @Operation(summary = "骑手账户余额", description = "返回可提现、冻结、累计收入、累计已提现")
    public R<RiderAccount> balance() {
        return R.success(riderSettlementService.getAccount(BaseContext.getCurrentId(),
                BaseContext.getCurrentTenantId()));
    }

    /**
     * 申请提现。
     */
    @PostMapping("/withdraw")
    @RequireRider
    @Operation(summary = "骑手申请提现", description = "冻结可提现金额，生成待审核提现申请")
    public R<RiderWithdrawal> withdraw(@RequestParam BigDecimal amount) {
        return R.success(riderSettlementService.applyWithdraw(BaseContext.getCurrentId(),
                BaseContext.getCurrentTenantId(), amount));
    }

    /**
     * 我的提现记录。
     */
    @GetMapping("/withdrawals")
    @RequireRider
    @Operation(summary = "我的提现记录", description = "骑手视角的提现申请列表")
    public R<List<RiderWithdrawal>> withdrawals() {
        return R.success(riderSettlementService.myWithdrawals(BaseContext.getCurrentId(),
                BaseContext.getCurrentTenantId()));
    }
}
