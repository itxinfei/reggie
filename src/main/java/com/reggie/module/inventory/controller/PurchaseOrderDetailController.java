package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.R;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import com.reggie.module.inventory.service.PurchaseOrderDetailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inventory/purchase-order-detail")
@Tag(name = "采购单明细")
public class PurchaseOrderDetailController {

    @Autowired
    private PurchaseOrderDetailService purchaseOrderDetailService;

    @GetMapping("/list/{orderId}")
    @Operation(summary = "根据采购单id查询明细")
    public R<List<PurchaseOrderDetail>> listByOrderId(@PathVariable Long orderId) {
        LambdaQueryWrapper<PurchaseOrderDetail> qw = new LambdaQueryWrapper<>();
        qw.eq(PurchaseOrderDetail::getPurchaseOrderId, orderId);
        return R.success(purchaseOrderDetailService.list(qw));
    }
}
