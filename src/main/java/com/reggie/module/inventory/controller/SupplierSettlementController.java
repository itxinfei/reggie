package com.reggie.module.inventory.controller;

import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.R;
import com.reggie.module.inventory.model.SupplierSettlement;
import com.reggie.module.inventory.service.SupplierSettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 供应商结算单控制器
 *
 * @author reggie
 * @since 2026-09-01
 */
@RequireEmployee
@RestController
@RequestMapping("/api/inventory/supplier-settlement")
@Tag(name = "供应商结算管理")
public class SupplierSettlementController {

    @Autowired
    private SupplierSettlementService supplierSettlementService;

    @PostMapping
    @Operation(summary = "创建结算单")
    public R<SupplierSettlement> create(@Parameter(description = "结算单信息", required = true) @RequestBody SupplierSettlement settlement) {
        return R.success(supplierSettlementService.createSettlement(settlement));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询结算单")
    public R<?> page(
            @Parameter(description = "页码，从1开始", required = true) @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页条数，最大100", required = true) @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "供应商ID，按供应商筛选") @RequestParam(required = false) Long supplierId,
            @Parameter(description = "状态（PENDING-待付款、PAID-已付款）") @RequestParam(required = false) String status) {
        return R.success(supplierSettlementService.pageSettlements(page, pageSize, supplierId, status));
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "结算单付款")
    @Parameter(name = "id", description = "结算单ID", required = true)
    public R<SupplierSettlement> pay(@PathVariable Long id, @Parameter(description = "实际付款金额（元）", required = true) @RequestParam BigDecimal payAmount) {
        return R.success(supplierSettlementService.paySettlement(id, payAmount));
    }
}
