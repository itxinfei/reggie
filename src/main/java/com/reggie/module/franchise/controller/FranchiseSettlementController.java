package com.reggie.module.franchise.controller;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.module.franchise.model.FranchiseSettlement;
import com.reggie.module.franchise.service.FranchiseSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.reggie.common.RateLimit;

/**
 * 加盟分账结算单管理
 *
 * @author reggie
 * @since 2026-08-15
 */
@Slf4j
@RestController
@RequestMapping("/api/franchise/settlement")
@Tag(name = "加盟管理-分账结算", description = "加盟分账结算单：生成/确认/结算/查询")
@RequiresPermission("franchise:manage")
public class FranchiseSettlementController {

    @Autowired
    private FranchiseSettlementService franchiseSettlementService;

    @PostMapping("/generate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "生成结算单", description = "按合同与周期（如 2026-08）聚合已完成订单生成分账结算单，幂等")
    public R<FranchiseSettlement> generate(
            @Parameter(description = "加盟合同ID", required = true) @RequestParam Long contractId,
            @Parameter(description = "结算周期，如 2026-08", required = true) @RequestParam String settlePeriod) {
        return R.success(franchiseSettlementService.generateSettlement(contractId, settlePeriod));
    }

    @PutMapping("/confirm/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "确认结算单", description = "待确认 → 已确认（总部核对营业额与抽成后确认）")
    public R<String> confirm(@Parameter(description = "结算单ID", required = true) @PathVariable Long id) {
        franchiseSettlementService.confirmSettlement(id);
        return R.success("确认成功");
    }

    @PutMapping("/settle/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "完成结算", description = "已确认 → 已结算（完成抽成划转）")
    public R<String> settle(@Parameter(description = "结算单ID", required = true) @PathVariable Long id) {
        franchiseSettlementService.settleSettlement(id);
        return R.success("结算成功");
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询结算单详情")
    public R<FranchiseSettlement> getById(@Parameter(description = "结算单ID", required = true) @PathVariable Long id) {
        FranchiseSettlement settlement = franchiseSettlementService.getById(id);
        if (settlement == null) {
            return R.error("结算单不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(settlement.getTenantId())) {
            return R.error("无权查看其他租户的结算单信息");
        }
        return R.success(settlement);
    }

    @GetMapping("/page")
    @Operation(summary = "结算单分页查询", description = "支持按周期/状态/加盟商筛选")
    public R<Page<FranchiseSettlement>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") @Min(1) int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
            @Parameter(description = "结算周期，如 2026-08") @RequestParam(required = false) String settlePeriod,
            @Parameter(description = "状态：0待确认 1已确认 2已结算") @RequestParam(required = false) Integer status,
            @Parameter(description = "加盟商ID") @RequestParam(required = false) Long franchiseeId) {
        return R.success(franchiseSettlementService.pageQuery(page, pageSize, settlePeriod, status, franchiseeId));
    }

    @GetMapping("/stats")
    @Operation(summary = "结算单统计", description = "返回总数/待确认/已确认/已结算，按当前租户聚合")
    public R<java.util.Map<String, Object>> stats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        return R.success(franchiseSettlementService.statSettlements(tenantId));
    }
}
