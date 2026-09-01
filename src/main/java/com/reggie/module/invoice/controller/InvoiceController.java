package com.reggie.module.invoice.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.invoice.model.InvoiceRecord;
import com.reggie.module.invoice.model.InvoiceTitle;
import com.reggie.module.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 发票管理控制器
 * 后台管理端：发票列表、开具、作废；用户端：申请开票
 */
@Slf4j
@RestController
@RequestMapping("/invoice")
@Tag(name = "发票管理")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    // ==================== 发票抬头管理（用户端） ====================

    @GetMapping("/title/list")
    @Operation(summary = "获取发票抬头列表")
    public R<List<InvoiceTitle>> listTitles(@RequestParam Long tenantId, @RequestParam Long userId) {
        return R.success(invoiceService.listTitles(tenantId, userId));
    }

    @PostMapping("/title/save")
    @Operation(summary = "保存发票抬头")
    public R<Void> saveTitle(@RequestBody InvoiceTitle title) {
        invoiceService.saveTitle(title);
        return R.success(null);
    }

    @DeleteMapping("/title/{id}")
    @Operation(summary = "删除发票抬头")
    public R<Void> deleteTitle(@PathVariable Long id,
                               @RequestParam Long tenantId,
                               @RequestParam Long userId) {
        invoiceService.deleteTitle(id, tenantId, userId);
        return R.success(null);
    }

    // ==================== 发票申请（用户端） ====================

    @PostMapping("/apply/{orderId}")
    @Operation(summary = "申请开票")
    public R<InvoiceRecord> applyInvoice(@PathVariable Long orderId,
                                         @RequestBody InvoiceRecord applyDTO) {
        Long tenantId = applyDTO.getTenantId();
        Long userId = com.reggie.common.BaseContext.getCurrentId();
        InvoiceRecord record = invoiceService.applyInvoice(
                orderId, userId, tenantId,
                applyDTO.getTitleId(), applyDTO.getTitle(),
                applyDTO.getTaxNumber(), applyDTO.getType());
        return R.success(record);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单发票记录")
    public R<InvoiceRecord> getInvoiceByOrder(@PathVariable Long orderId,
                                              @RequestParam Long tenantId,
                                              @RequestParam Long userId) {
        return R.success(invoiceService.getInvoiceByOrder(orderId, userId, tenantId));
    }

    // ==================== 后台管理 ====================

    @GetMapping("/list")
    @RequireEmployee
    @Operation(summary = "发票列表（后台）")
    public R<List<InvoiceRecord>> listRecords(@RequestParam(required = false) Integer status) {
        // 修改点(2026-09-01)：租户从员工会话获取，不再由前端显式传参，防止跨租户越权查询
        Long tenantId = currentTenantId();
        return R.success(invoiceService.listRecords(status, tenantId));
    }

    @PostMapping("/issue/{recordId}")
    @RequireEmployee
    @Operation(summary = "开具发票")
    public R<Void> issueInvoice(@PathVariable Long recordId,
                                @RequestParam String invoiceNo,
                                @RequestParam String invoiceCode,
                                @RequestParam String invoiceUrl) {
        Long tenantId = currentTenantId();
        invoiceService.issueInvoice(recordId, invoiceNo, invoiceCode, invoiceUrl, tenantId);
        return R.success(null);
    }

    @PostMapping("/void/{recordId}")
    @RequireEmployee
    @Operation(summary = "作废发票")
    public R<Void> voidInvoice(@PathVariable Long recordId) {
        Long tenantId = currentTenantId();
        invoiceService.voidInvoice(recordId, tenantId);
        return R.success(null);
    }

    /**
     * 从登录会话中获取当前租户ID（后台接口统一入口）
     *
     * @return 租户ID
     */
    private Long currentTenantId() {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("未获取到租户信息，请重新登录");
        }
        return tenantId;
    }
}