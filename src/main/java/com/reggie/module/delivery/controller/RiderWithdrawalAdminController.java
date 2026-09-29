package com.reggie.module.delivery.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.delivery.model.RiderWithdrawal;
import com.reggie.module.delivery.service.RiderSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端骑手提现审核控制器。
 *
 * @author reggie
 * @since 2026-09-28
 */
@RestController
@RequestMapping("/api/delivery/rider-withdrawal")
@Tag(name = "骑手提现审核", description = "管理端骑手提现申请查询与审核")
public class RiderWithdrawalAdminController {

    @Autowired
    private RiderSettlementService riderSettlementService;

    /**
     * 分页查询骑手提现申请。
     */
    @GetMapping("/page")
    @RequireEmployee
    @Operation(summary = "骑手提现申请分页", description = "按状态筛选（PENDING/APPROVED/REJECTED）")
    public R<Page<RiderWithdrawal>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return R.success(riderSettlementService.listWithdrawals(
                BaseContext.getCurrentTenantId(), status, page, size));
    }

    /**
     * 审核提现（通过 / 驳回）。
     */
    @PostMapping("/{id}/review")
    @RequireEmployee
    @Operation(summary = "审核骑手提现", description = "approve=true 通过（释放冻结视为已打款），false 驳回（退回可提现）")
    public R<RiderWithdrawal> review(
            @PathVariable Long id,
            @RequestParam boolean approve,
            @RequestParam(required = false) String remark) {
        Long reviewerId = BaseContext.getCurrentId();
        return R.success(riderSettlementService.review(id, approve, reviewerId, null, remark));
    }
}
