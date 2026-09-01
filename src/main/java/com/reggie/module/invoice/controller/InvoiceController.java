package com.reggie.module.invoice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.invoice.model.InvoiceRecord;
import com.reggie.module.invoice.model.InvoiceTitle;
import com.reggie.module.invoice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public R<List<InvoiceTitle>> listTitles() {
        // 修改点(2026-09-01)：userId/tenantId 从登录会话获取，不再由前端显式传参，防止跨租户越权
        return R.success(invoiceService.listTitles(currentTenantId(), currentUserId()));
    }

    @PostMapping("/title/save")
    @Operation(summary = "保存发票抬头")
    public R<Void> saveTitle(@Parameter(description = "发票抬头信息", required = true) @RequestBody InvoiceTitle title) {
        invoiceService.saveTitle(title);
        return R.success(null);
    }

    @DeleteMapping("/title/{id}")
    @Operation(summary = "删除发票抬头")
    public R<Void> deleteTitle(@Parameter(description = "发票抬头ID", required = true) @PathVariable Long id) {
        invoiceService.deleteTitle(id, currentTenantId(), currentUserId());
        return R.success(null);
    }

    // ==================== 发票申请（用户端） ====================

    @PostMapping("/apply/{orderId}")
    @Operation(summary = "申请开票")
    public R<InvoiceRecord> applyInvoice(@Parameter(description = "订单ID", required = true) @PathVariable Long orderId,
                                         @Parameter(description = "开票申请信息（抬头/税号/类型）", required = true) @RequestBody InvoiceRecord applyDTO) {
        Long tenantId = currentTenantId();
        Long userId = currentUserId();
        InvoiceRecord record = invoiceService.applyInvoice(
                orderId, userId, tenantId,
                applyDTO.getTitleId(), applyDTO.getTitle(),
                applyDTO.getTaxNumber(), applyDTO.getType());
        return R.success(record);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "查询订单发票记录")
    public R<InvoiceRecord> getInvoiceByOrder(@Parameter(description = "订单ID", required = true) @PathVariable Long orderId) {
        return R.success(invoiceService.getInvoiceByOrder(orderId, currentUserId(), currentTenantId()));
    }

    // ==================== 后台管理 ====================

    @GetMapping("/list")
    @RequireEmployee
    @Operation(summary = "发票列表（后台，分页）")
    public R<Page<InvoiceRecord>> listRecords(@Parameter(description = "发票状态（可选，如 PENDING/ISSUED/VOID）") @RequestParam(required = false) Integer status,
                                              @Parameter(description = "页码，从1开始", required = true) @RequestParam(defaultValue = "1") Integer page,
                                              @Parameter(description = "每页条数，最大100", required = true) @RequestParam(defaultValue = "10") Integer pageSize) {
        // 修改点(2026-09-01)：租户从员工会话获取，防止跨租户越权查询；分页上限由 PageUtils.cap 收敛
        Long tenantId = currentTenantId();
        Page<InvoiceRecord> pageInfo = PageUtils.of(page, PageUtils.cap(pageSize));
        return R.success(invoiceService.listRecords(pageInfo, status, tenantId));
    }

    @GetMapping("/stats")
    @RequireEmployee
    @Operation(summary = "发票状态统计（后台统计卡）")
    public R<Map<String, Integer>> listStats() {
        // 修改点(2026-09-01)：租户从员工会话获取；统计卡全量统计，不随分页/状态筛选变化
        Long tenantId = currentTenantId();
        return R.success(invoiceService.listStats(tenantId));
    }

    @PostMapping("/issue/{recordId}")
    @RequireEmployee
    @Operation(summary = "开具发票")
    public R<Void> issueInvoice(@Parameter(description = "发票申请记录ID", required = true) @PathVariable Long recordId,
                                @Parameter(description = "发票号码", required = true) @RequestParam String invoiceNo,
                                @Parameter(description = "发票代码", required = true) @RequestParam String invoiceCode,
                                @Parameter(description = "发票PDF/图片地址", required = true) @RequestParam String invoiceUrl) {
        Long tenantId = currentTenantId();
        invoiceService.issueInvoice(recordId, invoiceNo, invoiceCode, invoiceUrl, tenantId);
        return R.success(null);
    }

    @PostMapping("/void/{recordId}")
    @RequireEmployee
    @Operation(summary = "作废发票")
    public R<Void> voidInvoice(@Parameter(description = "发票申请记录ID", required = true) @PathVariable Long recordId) {
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

    /**
     * 从登录会话中获取当前用户ID（用户端接口统一入口）
     *
     * @return 用户ID
     */
    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new CustomException("未获取到用户信息，请重新登录");
        }
        return userId;
    }
}