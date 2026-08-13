package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.BaseContext;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import com.reggie.module.inventory.service.PurchaseOrderDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 采购单明细控制器
 * 提供采购单明细查询接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RequireEmployee
@RestController
@RequestMapping("/api/inventory/purchase-order-detail")
@Tag(name = "采购单明细")
public class PurchaseOrderDetailController {

    @Autowired
    private PurchaseOrderDetailService purchaseOrderDetailService;

    @GetMapping("/list/{orderId}")
    @Operation(summary = "根据采购单id查询明细", description = "查询指定采购单的所有明细项")
    @Parameter(name = "orderId", description = "采购单ID", required = true)
    public R<List<PurchaseOrderDetail>> listByOrderId(@PathVariable Long orderId) {
        // 多租户校验：确认采购单属于当前租户
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return R.error("未登录或登录已过期");
        }
        LambdaQueryWrapper<PurchaseOrderDetail> qw = new LambdaQueryWrapper<>();
        qw.eq(PurchaseOrderDetail::getPurchaseOrderId, orderId);
        return R.success(purchaseOrderDetailService.list(qw));
    }
}

