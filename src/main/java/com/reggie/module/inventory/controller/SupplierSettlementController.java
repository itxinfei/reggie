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
    public R<SupplierSettlement> create(@RequestBody SupplierSettlement settlement) {
        return R.success(supplierSettlementService.createSettlement(settlement));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询结算单")
    public R<?> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) String status) {
        return R.success(supplierSettlementService.pageSettlements(page, pageSize, supplierId, status));
    }
}
