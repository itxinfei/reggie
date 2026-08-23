package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.mapper.PurchaseOrderMapper;
import com.reggie.module.inventory.mapper.StockRecordMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.module.inventory.service.InventoryStatsService;
import com.reggie.module.inventory.service.MaterialCategoryService;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.service.SupplierService;
import com.reggie.enums.PurchaseOrderStatus;
import com.reggie.enums.StockRecordType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 进销存统计服务实现
 * <p>域4 改造：从 InventoryStatsController 下沉，封装所有跨 Mapper 聚合查询</p>
 *
 * @author reggie
 * @since 2026-08-22
 */
@Slf4j
@Service
public class InventoryStatsServiceImpl implements InventoryStatsService {

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

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 食材统计
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

        // 今日 / 本月采购统计
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime monthStart = LocalDateTime.of(LocalDate.now().withDayOfMonth(1), LocalTime.MIN);

        long todayPOCount = purchaseOrderMapper.countBetween(todayStart, todayEnd);
        BigDecimal todayPOAmount = purchaseOrderMapper.sumAmountBetween(todayStart, todayEnd);
        long monthPOCount = purchaseOrderMapper.countBetween(monthStart, todayEnd);
        BigDecimal monthPOAmount = purchaseOrderMapper.sumAmountBetween(monthStart, todayEnd);
        long pendingReceiveCount = purchaseOrderMapper.countPendingReceive(monthStart, todayEnd,
                PurchaseOrderStatus.ORDERED.getValue(), PurchaseOrderStatus.PARTIAL.getValue());

        // 今日出入库
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
        return result;
    }

    @Override
    public List<Map<String, Object>> purchaseTrend() {
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
        return trend;
    }

    @Override
    public List<Map<String, Object>> stockTrend() {
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
        return trend;
    }
}