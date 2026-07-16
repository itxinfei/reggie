package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.R;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.mapper.PurchaseOrderMapper;
import com.reggie.module.inventory.mapper.StockRecordMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.model.StockRecord;
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
    private MaterialMapper materialMapper;
    @Autowired
    private PurchaseOrderMapper purchaseOrderMapper;
    @Autowired
    private StockRecordMapper stockRecordMapper;

    /**
     * 进销存总览统计
     * GET /api/inventory/stats/overview
     */
    @GetMapping("/overview")
    @Operation(summary = "进销存总览", description = "获取进销存模块的核心统计数据：食材/分类/供应商数量、库存预警、今日采购、今日出入库")
    public R<Map<String, Object>> overview() {
        // 修改点：所有统计改用 SQL 聚合查询，避免 list() 全表扫描导致内存暴涨/OOM
        Map<String, Object> result = new LinkedHashMap<>();

        // 食材统计（聚合查询，避免全表扫描）
        long totalMaterials = materialService.count();
        long activeMaterials = materialService.count(
                new LambdaQueryWrapper<Material>().eq(Material::getStatus, Material.STATUS_NORMAL));
        long lowStockCount = materialMapper.countLowStock();
        BigDecimal totalInventoryValue = materialMapper.sumInventoryValue()
                .setScale(2, RoundingMode.HALF_UP);
        List<Map<String, Object>> lowStockItems = new ArrayList<>();
        for (Material m : materialMapper.selectLowStock(10)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", m.getId());
            item.put("name", m.getName());
            item.put("stockQty", m.getStockQty());
            item.put("minStock", m.getMinStock());
            item.put("unit", m.getUnit());
            lowStockItems.add(item);
        }

        // 今日 / 本月采购统计（SQL 聚合）
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);

        long todayPOCount = purchaseOrderMapper.countBetween(todayStart, todayEnd);
        BigDecimal todayPOAmount = purchaseOrderMapper.sumAmountBetween(todayStart, todayEnd);
        long monthPOCount = purchaseOrderMapper.countBetween(monthStart, todayEnd);
        BigDecimal monthPOAmount = purchaseOrderMapper.sumAmountBetween(monthStart, todayEnd);
        long pendingReceiveCount = purchaseOrderMapper.countPendingReceive(monthStart, todayEnd,
                PurchaseOrderStatus.ORDERED.getValue(), PurchaseOrderStatus.PARTIAL.getValue());

        // 今日出入库（SQL 聚合）
        long todayInCount = stockRecordMapper.countByTypeBetween(todayStart, todayEnd, StockRecordType.IN.getValue());
        long todayOutCount = stockRecordMapper.countByTypeBetween(todayStart, todayEnd, StockRecordType.OUT.getValue());
        BigDecimal todayInAmount = stockRecordMapper.sumAmountByTypeBetween(todayStart, todayEnd, StockRecordType.IN.getValue());

        result.put("totalMaterials", totalMaterials);
        result.put("activeMaterials", activeMaterials);
        result.put("lowStockCount", lowStockCount);
        result.put("totalCategories", materialCategoryService.count());
        result.put("totalSuppliers", supplierService.count());
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
        // 修改点：仅查询近30天采购单（而非全量历史），在内存按日聚合，避免全表扫描
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime trendStart = LocalDateTime.of(LocalDate.now().minusDays(29), LocalTime.MIN);
        List<PurchaseOrder> poList = purchaseOrderMapper.selectSince(trendStart);

        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime ds = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime de = LocalDateTime.of(date, LocalTime.MAX);
            BigDecimal dayAmount = poList.stream()
                    .filter(po -> po.getCreatedTime() != null && !po.getCreatedTime().isBefore(ds) && !po.getCreatedTime().isAfter(de))
                    .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long dayCount = poList.stream()
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
        // 修改点：仅查询近30天库存记录（而非全量历史），在内存按日聚合，避免全表扫描
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDateTime trendStart = LocalDateTime.of(LocalDate.now().minusDays(29), LocalTime.MIN);
        List<StockRecord> srList = stockRecordMapper.selectSince(trendStart);

        for (int i = 29; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime ds = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime de = LocalDateTime.of(date, LocalTime.MAX);
            long dayIn = srList.stream()
                    .filter(r -> r.getCreatedTime() != null && !r.getCreatedTime().isBefore(ds)
                            && !r.getCreatedTime().isAfter(de)
                            && StockRecordType.IN.getValue().equals(r.getType()))
                    .count();
            long dayOut = srList.stream()
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
