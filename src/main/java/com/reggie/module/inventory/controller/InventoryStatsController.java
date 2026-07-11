package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.inventory.model.*;
import com.reggie.enums.PurchaseOrderStatus;
import com.reggie.enums.StockRecordType;
import com.reggie.module.inventory.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 进销存统计控制器
 * 提供进销存模块的数据统计、趋势分析、库存预警摘要等聚合接口
 *
 * @author reggie
 * @since 2026-07-11
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory/stats")
@Tag(name = "进销存统计", description = "进销存模块数据统计API")
public class InventoryStatsController {

    @Autowired
    private MaterialService materialService;
    @Autowired
    private MaterialCategoryService materialCategoryService;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private PurchaseOrderService purchaseOrderService;
    @Autowired
    private StockRecordService stockRecordService;
    @Autowired
    private StockCheckService stockCheckService;

    /**
     * 进销存总览统计
     * GET /api/inventory/stats/overview
     */
    @GetMapping("/overview")
    @Operation(summary = "进销存总览", description = "获取进销存模块的核心统计数据：食材/分类/供应商数量、库存预警、今日采购、今日出入库")
    public R<Map<String, Object>> overview() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> result = new LinkedHashMap<>();

        // 食材统计
        LambdaQueryWrapper<Material> matQw = new LambdaQueryWrapper<>();
        if (tenantId != null) matQw.eq(Material::getTenantId, tenantId);
        List<Material> materials = materialService.list(matQw);
        long totalMaterials = materials.size();
        long activeMaterials = materials.stream().filter(m -> m.getStatus() != null && m.getStatus() == 1).count();
        long lowStockCount = materials.stream().filter(m ->
                m.getStockQty() != null && m.getMinStock() != null && m.getStockQty().compareTo(m.getMinStock()) <= 0).count();

        // 分类与供应商
        LambdaQueryWrapper<MaterialCategory> catQw = new LambdaQueryWrapper<>();
        if (tenantId != null) catQw.eq(MaterialCategory::getTenantId, tenantId);
        long totalCategories = materialCategoryService.count(catQw);

        LambdaQueryWrapper<Supplier> supQw = new LambdaQueryWrapper<>();
        if (tenantId != null) supQw.eq(Supplier::getTenantId, tenantId);
        long totalSuppliers = supplierService.count(supQw);

        // 今日采购统计
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LambdaQueryWrapper<PurchaseOrder> poTodayQw = new LambdaQueryWrapper<>();
        if (tenantId != null) poTodayQw.eq(PurchaseOrder::getTenantId, tenantId);
        poTodayQw.between(PurchaseOrder::getCreatedTime, todayStart, todayEnd);
        List<PurchaseOrder> todayPO = purchaseOrderService.list(poTodayQw);
        long todayPOCount = todayPO.size();
        BigDecimal todayPOAmount = todayPO.stream()
                .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 本月采购统计
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);
        LambdaQueryWrapper<PurchaseOrder> poMonthQw = new LambdaQueryWrapper<>();
        if (tenantId != null) poMonthQw.eq(PurchaseOrder::getTenantId, tenantId);
        poMonthQw.between(PurchaseOrder::getCreatedTime, monthStart, todayEnd);
        List<PurchaseOrder> monthPO = purchaseOrderService.list(poMonthQw);
        long monthPOCount = monthPO.size();
        BigDecimal monthPOAmount = monthPO.stream()
                .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 待收货采购单
        long pendingReceiveCount = monthPO.stream()
                .filter(po -> PurchaseOrderStatus.ORDERED.getValue().equals(po.getStatus())
                        || PurchaseOrderStatus.PARTIAL.getValue().equals(po.getStatus())).count();

        // 今日出入库
        LambdaQueryWrapper<StockRecord> srTodayQw = new LambdaQueryWrapper<>();
        if (tenantId != null) srTodayQw.eq(StockRecord::getTenantId, tenantId);
        srTodayQw.between(StockRecord::getCreatedTime, todayStart, todayEnd);
        List<StockRecord> todaySR = stockRecordService.list(srTodayQw);
        long todayInCount = todaySR.stream().filter(r -> StockRecordType.IN.getValue().equals(r.getType())).count();
        long todayOutCount = todaySR.stream().filter(r -> StockRecordType.OUT.getValue().equals(r.getType())).count();
        BigDecimal todayInAmount = todaySR.stream()
                .filter(r -> StockRecordType.IN.getValue().equals(r.getType())
                        && r.getTotalAmount() != null)
                .map(StockRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 库存预警详情
        List<Map<String, Object>> lowStockItems = new ArrayList<>();
        for (Material m : materials) {
            if (m.getStockQty() != null && m.getMinStock() != null
                    && m.getStockQty().compareTo(m.getMinStock()) <= 0 && m.getStatus() != null && m.getStatus() == 1) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", m.getId());
                item.put("name", m.getName());
                item.put("stockQty", m.getStockQty());
                item.put("minStock", m.getMinStock());
                item.put("unit", m.getUnit());
                lowStockItems.add(item);
            }
        }
        // 最多返回10条预警
        if (lowStockItems.size() > 10) {
            lowStockItems = lowStockItems.subList(0, 10);
        }

        // 库存总价值
        BigDecimal totalInventoryValue = materials.stream()
                .filter(m -> m.getStockQty() != null && m.getUnitPrice() != null)
                .map(m -> m.getStockQty().multiply(m.getUnitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        result.put("totalMaterials", totalMaterials);
        result.put("activeMaterials", activeMaterials);
        result.put("lowStockCount", lowStockCount);
        result.put("totalCategories", totalCategories);
        result.put("totalSuppliers", totalSuppliers);
        result.put("totalInventoryValue", totalInventoryValue);
        result.put("todayPOCount", todayPOCount);
        result.put("todayPOAmount", todayPOAmount);
        result.put("monthPOCount", monthPOCount);
        result.put("monthPOAmount", monthPOAmount);
        result.put("pendingReceiveCount", pendingReceiveCount);
        result.put("todayInCount", todayInCount);
        result.put("todayOutCount", todayOutCount);
        result.put("todayInAmount", todayInAmount);
        result.put("lowStockItems", lowStockItems);
        return R.success(result);
    }

    /**
     * 近30天采购趋势
     * GET /api/inventory/stats/purchase-trend
     */
    @GetMapping("/purchase-trend")
    @Operation(summary = "采购趋势", description = "获取近30天每日采购金额趋势")
    public R<List<Map<String, Object>>> purchaseTrend() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> trend = new ArrayList<>();

        LambdaQueryWrapper<PurchaseOrder> allQw = new LambdaQueryWrapper<>();
        if (tenantId != null) allQw.eq(PurchaseOrder::getTenantId, tenantId);
        List<PurchaseOrder> allPO = purchaseOrderService.list(allQw);

        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime ds = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime de = LocalDateTime.of(date, LocalTime.MAX);
            BigDecimal dayAmount = allPO.stream()
                    .filter(po -> po.getCreatedTime() != null && !po.getCreatedTime().isBefore(ds) && !po.getCreatedTime().isAfter(de))
                    .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long dayCount = allPO.stream()
                    .filter(po -> po.getCreatedTime() != null && !po.getCreatedTime().isBefore(ds) && !po.getCreatedTime().isAfter(de))
                    .count();
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString().substring(5));
            day.put("amount", dayAmount);
            day.put("count", dayCount);
            trend.add(day);
        }
        return R.success(trend);
    }

    /**
     * 近30天出入库趋势
     * GET /api/inventory/stats/stock-trend
     */
    @GetMapping("/stock-trend")
    @Operation(summary = "库存趋势", description = "获取近30天每日入库/出库数量趋势")
    public R<List<Map<String, Object>>> stockTrend() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> trend = new ArrayList<>();

        LambdaQueryWrapper<StockRecord> allQw = new LambdaQueryWrapper<>();
        if (tenantId != null) allQw.eq(StockRecord::getTenantId, tenantId);
        List<StockRecord> allSR = stockRecordService.list(allQw);

        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime ds = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime de = LocalDateTime.of(date, LocalTime.MAX);
            long dayIn = allSR.stream()
                    .filter(r -> r.getCreatedTime() != null && !r.getCreatedTime().isBefore(ds)
                            && !r.getCreatedTime().isAfter(de)
                            && StockRecordType.IN.getValue().equals(r.getType()))
                    .count();
            long dayOut = allSR.stream()
                    .filter(r -> r.getCreatedTime() != null && !r.getCreatedTime().isBefore(ds)
                            && !r.getCreatedTime().isAfter(de)
                            && StockRecordType.OUT.getValue().equals(r.getType()))
                    .count();
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date.toString().substring(5));
            day.put("inCount", dayIn);
            day.put("outCount", dayOut);
            trend.add(day);
        }
        return R.success(trend);
    }
}
