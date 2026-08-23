package com.reggie.module.inventory.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.dto.AddPurchaseDetailDTO;
import com.reggie.dto.CreatePurchaseOrderDTO;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import com.reggie.module.inventory.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 采购单管理控制器
 * 提供采购单的创建、审核、收货、取消等接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RequireEmployee
@RestController
@RequestMapping("/api/inventory/purchase-order")
@Tag(name = "采购单管理")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    /**
     * 分页查询采购单列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param status 采购单状态（可选）
     * @param supplierId 供应商ID（可选）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询采购单列表，支持按状态、供应商筛选，按创建时间降序排列")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "status", description = "状态（可选）：PENDING-待审核, APPROVED-已审核, RECEIVED-已收货, CANCELLED-已取消")
    @Parameter(name = "supplierId", description = "供应商ID（可选）")
    public R<Page<PurchaseOrder>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) Long supplierId) {
        Page<PurchaseOrder> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
        // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
        qw.eq(status != null && !status.isEmpty(), PurchaseOrder::getStatus, status);
        qw.eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId);
        qw.orderByDesc(PurchaseOrder::getCreatedTime);
        purchaseOrderService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 创建采购单
     * @param dto 采购单创建请求
     * @return 采购单信息
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "创建采购单", description = "创建新的采购单并关联供应商")
    public R<PurchaseOrder> create(@Validated @RequestBody CreatePurchaseOrderDTO dto) {
        PurchaseOrder po = purchaseOrderService.createOrder(dto.getSupplierId(), dto.getOperator(), dto.getRemark());
        return R.success(po);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询采购单", description = "根据ID查询采购单详情")
    @Parameter(name = "id", description = "采购单ID", required = true)
    public R<PurchaseOrder> get(@PathVariable Long id) {
        PurchaseOrder po = purchaseOrderService.getById(id);
        if (po == null) {
            return R.error("采购单不存在");
        }
        return R.success(po);
    }

    /**
     * 查询采购单明细
     * @param id 采购单ID
     * @return 采购单明细列表
     */
    @GetMapping("/detail/{id}")
    @Operation(summary = "查询采购单明细", description = "根据采购单ID查询所有明细项")
    @Parameter(name = "id", description = "采购单ID", required = true)
    public R<List<PurchaseOrderDetail>> getDetail(@PathVariable Long id) {
        List<PurchaseOrderDetail> details = purchaseOrderService.getDetailsByOrderId(id);
        return R.success(details);
    }

    /**
     * 为采购单添加明细项
     * @param dto 采购单明细请求
     * @return 操作结果
     */
    @PostMapping("/addDetail")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "添加明细", description = "为采购单添加食材明细项")
    public R<String> addDetail(@Validated @RequestBody AddPurchaseDetailDTO dto) {
        purchaseOrderService.addDetail(dto.getOrderId(), dto.getMaterialId(), dto.getQty(), dto.getUnitPrice());
        return R.success("添加明细成功");
    }

    /**
     * 确认采购单收货
     * @param id 采购单ID
     * @return 操作结果
     */
    @PutMapping("/receive/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "收货", description = "确认采购单收货并自动增加库存")
    @Parameter(name = "id", description = "采购单ID", required = true)
    public R<String> receive(@PathVariable Long id) {
        purchaseOrderService.receiveOrder(id);
        return R.success("收货成功");
    }

    /**
     * 取消采购单
     * @param id 采购单ID
     * @return 操作结果
     */
    @PutMapping("/cancel/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "取消", description = "取消采购单")
    @Parameter(name = "id", description = "采购单ID", required = true)
    public R<String> cancel(@PathVariable Long id) {
        purchaseOrderService.cancelOrder(id);
        return R.success("取消成功");
    }

    /**
     * 审核通过采购单
     * @param id 采购单ID
     * @return 操作结果
     */
    @PutMapping("/approve/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "审核通过", description = "审核通过采购单，将草稿状态转为已下单状态，允许后续收货")
    @Parameter(name = "id", description = "采购单ID", required = true)
    public R<String> approve(@PathVariable Long id) {
        purchaseOrderService.approveOrder(id);
        return R.success("审核通过");
    }
}

