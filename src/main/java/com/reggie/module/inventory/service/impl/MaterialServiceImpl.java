package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.enums.StockRecordType;
import com.reggie.module.inventory.dto.BatchRestockDTO;
import com.reggie.module.inventory.mapper.MaterialCategoryMapper;
import com.reggie.module.inventory.mapper.MaterialMapper;
import com.reggie.module.inventory.mapper.SupplierMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.MaterialCategory;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.service.PurchaseOrderService;
import com.reggie.module.inventory.service.StockRecordService;
import com.reggie.module.inventory.service.SupplierService;
import com.reggie.module.inventory.vo.WarningMaterialVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.reggie.module.inventory.model.Material.STATUS_NORMAL;

/**
 * 食材服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class MaterialServiceImpl extends ServiceImpl<MaterialMapper, Material> implements MaterialService {

    @Autowired
    private MaterialCategoryMapper materialCategoryMapper;
    @Autowired
    private SupplierMapper supplierMapper;
    @Autowired
    private StockRecordService stockRecordService;
    @Autowired
    private PurchaseOrderService purchaseOrderService;
    @Autowired
    private SupplierService supplierService;

    @Override
    public Page<Material> pageWithCategory(int page, int pageSize) {
        Page<Material> pageInfo = PageUtils.of(page, pageSize);
        return page(pageInfo);
    }

    @Override
    public List<Material> checkWarning() {
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(Material::getTenantId, tenantId);
        }
        qw.eq(Material::getStatus, STATUS_NORMAL);
        qw.apply("stock_qty < min_stock");
        List<Material> list = list(qw);
        fillCategoryAndSupplier(list);
        return list;
    }

    /**
     * 重写双参分页方法，确保分页查询后填充分类名称和供应商名称
     */
    @Override
    public <E extends IPage<Material>> E page(E page, Wrapper<Material> queryWrapper) {
        E result = super.page(page, queryWrapper);
        List<Material> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillCategoryAndSupplier(records);
        }
        return result;
    }

    public Page<Material> page(Page<Material> pageInfo) {
        Page<Material> result = super.page(pageInfo);
        List<Material> records = result.getRecords();
        if (!CollectionUtils.isEmpty(records)) {
            fillCategoryAndSupplier(records);
        }
        return result;
    }

    @Override
    public List<Material> list(Wrapper<Material> queryWrapper) {
        List<Material> list = super.list(queryWrapper);
        if (!CollectionUtils.isEmpty(list)) {
            fillCategoryAndSupplier(list);
        }
        return list;
    }

    /**
     * 预警食材分页查询
     * <p>
     * 严重度分级规则：
     * - CRITICAL: stockQty / minStock < 0.3
     * - WARNING:  0.3 <= stockQty / minStock < 0.8
     * - LOW:      0.8 <= stockQty / minStock < 1.0
     * </p>
     *
     * @param page       页码
     * @param pageSize   每页数量
     * @param categoryId 分类ID（可选）
     * @param severity   严重度（可选）
     * @return 预警VO分页
     */
    @Override
    public Page<WarningMaterialVO> warningPage(int page, int pageSize, Long categoryId, String severity) {
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(Material::getTenantId, tenantId);
        }
        qw.eq(Material::getStatus, STATUS_NORMAL);
        qw.apply("stock_qty < min_stock");
        qw.eq(categoryId != null, Material::getCategoryId, categoryId);
        qw.orderByAsc(Material::getStockQty);

        Page<Material> pageInfo = PageUtils.of(page, pageSize);
        Page<Material> result = page(pageInfo, qw);

        Page<WarningMaterialVO> voPage = new Page<>(page, pageSize, result.getTotal());
        List<Material> records = result.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        List<WarningMaterialVO> voList = new ArrayList<>(records.size());
        for (Material m : records) {
            WarningMaterialVO vo = toWarningVO(m);
            // severity 筛选
            if (severity != null && !severity.isEmpty() && !severity.equals(vo.getSeverity())) {
                continue;
            }
            voList.add(vo);
        }
        // 重新计算总数（因 severity 筛选会减少记录数，但总数仍反映所有预警食材）
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 预警聚合统计
     * 返回按严重度分类的预警数量 + 按分类聚合的预警数量 + 总数
     */
    @Override
    public Map<String, Object> warningStats() {
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(Material::getTenantId, tenantId);
        }
        qw.eq(Material::getStatus, STATUS_NORMAL);
        qw.apply("stock_qty < min_stock");
        List<Material> list = list(qw);
        fillCategoryAndSupplier(list);

        Map<String, Object> stats = new HashMap<String, Object>();

        if (CollectionUtils.isEmpty(list)) {
            stats.put("totalCount", 0L);
            Map<String, Long> bySeverity = new HashMap<String, Long>();
            bySeverity.put(WarningMaterialVO.SEVERITY_CRITICAL, 0L);
            bySeverity.put(WarningMaterialVO.SEVERITY_WARNING, 0L);
            bySeverity.put(WarningMaterialVO.SEVERITY_LOW, 0L);
            stats.put("bySeverity", bySeverity);
            stats.put("byCategory", new ArrayList<Map<String, Object>>());
            return stats;
        }

        stats.put("totalCount", (long) list.size());

        // 按严重度统计
        Map<String, Long> bySeverity = new HashMap<String, Long>();
        bySeverity.put(WarningMaterialVO.SEVERITY_CRITICAL, 0L);
        bySeverity.put(WarningMaterialVO.SEVERITY_WARNING, 0L);
        bySeverity.put(WarningMaterialVO.SEVERITY_LOW, 0L);

        // 按分类统计
        Map<Long, Map<String, Object>> categoryMap = new HashMap<Long, Map<String, Object>>();

        for (Material m : list) {
            String sev = calcSeverity(m.getStockQty(), m.getMinStock());
            bySeverity.put(sev, bySeverity.getOrDefault(sev, 0L) + 1);

            Long catId = m.getCategoryId();
            if (catId != null) {
                Map<String, Object> catStats = categoryMap.get(catId);
                if (catStats == null) {
                    catStats = new HashMap<String, Object>();
                    catStats.put("categoryId", catId);
                    catStats.put("categoryName", m.getCategoryName());
                    catStats.put("count", 0L);
                    categoryMap.put(catId, catStats);
                }
                catStats.put("count", (Long) catStats.get("count") + 1);
            }
        }

        // 按 count 降序排列（冒泡排序，JDK 1.8 兼容）
        List<Map<String, Object>> byCategory = new ArrayList<Map<String, Object>>(categoryMap.values());
        for (int i = 0; i < byCategory.size(); i++) {
            for (int j = 0; j < byCategory.size() - 1 - i; j++) {
                Long a = (Long) byCategory.get(j).get("count");
                Long b = (Long) byCategory.get(j + 1).get("count");
                if (b != null && (a == null || b.longValue() > a.longValue())) {
                    Map<String, Object> tmp = byCategory.get(j);
                    byCategory.set(j, byCategory.get(j + 1));
                    byCategory.set(j + 1, tmp);
                }
            }
        }

        stats.put("bySeverity", bySeverity);
        stats.put("byCategory", byCategory);
        return stats;
    }

    /**
     * 补货建议：基于历史 N 天出库量计算日均消耗，给出建议采购量
     * 建议采购量 = 日均消耗 × 14（补货周期） + minStock - stockQty
     * 若结果为负或零，说明库存充足，建议采购量为 0
     *
     * @param days 统计天数（默认30天）
     * @return 补货建议列表，按建议采购量降序
     */
    @Override
    public List<Map<String, Object>> replenishSuggest(int days) {
        if (days <= 0) {
            days = 30;
        }
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        // 查询近 N 天出库记录（附加租户条件，防止跨租户探测库存流水）
        Long tenantId = BaseContext.getCurrentTenantId();
        List<StockRecord> outRecords = stockRecordService.list(
            new LambdaQueryWrapper<StockRecord>()
                .eq(StockRecord::getType, StockRecordType.OUT.getValue())
                .ge(StockRecord::getCreatedTime, since)
                .eq(tenantId != null, StockRecord::getTenantId, tenantId)
        );

        // 按 materialId 汇总出库总量
        Map<Long, BigDecimal> outQtyMap = new HashMap<Long, BigDecimal>();
        if (!CollectionUtils.isEmpty(outRecords)) {
            for (StockRecord r : outRecords) {
                Long mid = r.getMaterialId();
                if (mid != null && r.getQty() != null) {
                    outQtyMap.put(mid, outQtyMap.getOrDefault(mid, BigDecimal.ZERO)
                        .add(r.getQty()));
                }
            }
        }

        // 获取所有预警食材（stock < minStock）
        List<Material> warningMaterials = checkWarning();
        if (CollectionUtils.isEmpty(warningMaterials)) {
            return new ArrayList<Map<String, Object>>();
        }

        List<Map<String, Object>> suggestList = new ArrayList<Map<String, Object>>(warningMaterials.size());
        BigDecimal replenishCycleDays = new BigDecimal("14");

        for (Material m : warningMaterials) {
            BigDecimal outTotal = outQtyMap.get(m.getId());
            if (outTotal == null) {
                outTotal = BigDecimal.ZERO;
            }
            // 除零防护：days 为方法参数，若为 0 或负数会抛 ArithmeticException
            BigDecimal daysBd = new BigDecimal(days);
            BigDecimal dailyUsage = daysBd.compareTo(BigDecimal.ZERO) > 0
                    ? outTotal.divide(daysBd, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            BigDecimal minStock = m.getMinStock() != null ? m.getMinStock() : BigDecimal.ZERO;
            BigDecimal stockQty = m.getStockQty() != null ? m.getStockQty() : BigDecimal.ZERO;

            // 建议采购量 = 日均消耗 × 14 + minStock - stockQty
            BigDecimal suggestQty = dailyUsage.multiply(replenishCycleDays)
                .add(minStock)
                .subtract(stockQty)
                .setScale(2, RoundingMode.HALF_UP);

            // 不足 0 时置为 0
            if (suggestQty.compareTo(BigDecimal.ZERO) < 0) {
                suggestQty = BigDecimal.ZERO;
            }

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("materialId", m.getId());
            item.put("materialName", m.getName());
            item.put("unit", m.getUnit());
            item.put("stockQty", stockQty);
            item.put("minStock", minStock);
            item.put("dailyUsage", dailyUsage);
            item.put("outTotal", outTotal);
            item.put("suggestQty", suggestQty);
            item.put("unitPrice", m.getUnitPrice());
            item.put("supplierId", m.getSupplierId());
            item.put("supplierName", m.getSupplierName());
            suggestList.add(item);
        }

        // 按 suggestQty 降序（冒泡排序）
        for (int i = 0; i < suggestList.size(); i++) {
            for (int j = 0; j < suggestList.size() - 1 - i; j++) {
                BigDecimal a = (BigDecimal) suggestList.get(j).get("suggestQty");
                BigDecimal b = (BigDecimal) suggestList.get(j + 1).get("suggestQty");
                if (b != null && (a == null || b.compareTo(a) > 0)) {
                    Map<String, Object> tmp = suggestList.get(j);
                    suggestList.set(j, suggestList.get(j + 1));
                    suggestList.set(j + 1, tmp);
                }
            }
        }

        return suggestList;
    }

    /**
     * 批量补货：创建采购单并自动入库
     * <p>
     * 流程：
     * 1. 获取第一条补货食材的 supplierId 作为采购单供应商
     * 2. 创建采购单（草稿状态）
     * 3. 为每个补货食材添加采购明细
     * 4. 直接调用 stockIn 完成入库（无需等待确认收货）
     * </p>
     *
     * @param dto 批量补货请求
     * @return 生成的采购单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long batchRestock(BatchRestockDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new CustomException("补货明细不能为空");
        }

        List<BatchRestockDTO.RestockItem> items = dto.getItems();

        // 获取第一条补货食材的 supplierId 作为采购单供应商
        Long supplierId = items.get(0).getMaterialId() != null
            ? getByItemId(items.get(0).getMaterialId()) : null;

        // 若无法确定供应商，尝试取第一条有效食材的供应商
        if (supplierId == null) {
            for (BatchRestockDTO.RestockItem item : items) {
                if (item.getMaterialId() != null) {
                    Material m = getById(item.getMaterialId());
                    if (m != null && m.getSupplierId() != null) {
                        supplierId = m.getSupplierId();
                        break;
                    }
                }
            }
            if (supplierId == null) {
                throw new CustomException("无法确定供应商");
            }
        }

        // 创建采购单
        Long tenantId = BaseContext.getCurrentTenantId();
        Long orderSupplierId = supplierId;
        String operator = dto.getOperator() != null ? dto.getOperator() : "系统补货";
        String remark = dto.getRemark() != null ? dto.getRemark() : "批量补货";
        PurchaseOrder po = purchaseOrderService.createOrder(orderSupplierId, operator, remark);
        Long orderId = po != null ? po.getId() : null;

        // 为每个补货食材添加采购明细并入库
        for (BatchRestockDTO.RestockItem item : items) {
            Long materialId = item.getMaterialId();
            if (materialId == null) continue;

            BigDecimal qty = new BigDecimal(item.getQty() != null ? item.getQty() : "0");
            if (qty.compareTo(BigDecimal.ZERO) <= 0) continue;

            Material material = getById(materialId);
            if (material == null) continue;

            BigDecimal unitPrice = material.getUnitPrice() != null ? material.getUnitPrice() : BigDecimal.ZERO;

            // 添加采购明细
            purchaseOrderService.addDetail(orderId, materialId, qty, unitPrice);

            // 直接入库（补货模式：无需等待确认收货）
            stockRecordService.stockIn(materialId, qty, unitPrice, orderId, "批量补货入库", operator);
        }

        return orderId;
    }

    private Long getByItemId(Long materialId) {
        Material m = getById(materialId);
        return m != null ? m.getSupplierId() : null;
    }

    /**
     * 将 Material 转换为 WarningMaterialVO，计算阈值比例和严重度
     */
    private WarningMaterialVO toWarningVO(Material m) {
        WarningMaterialVO vo = new WarningMaterialVO();
        vo.setId(m.getId());
        vo.setName(m.getName());
        vo.setUnit(m.getUnit());
        vo.setStockQty(m.getStockQty());
        vo.setMinStock(m.getMinStock());
        vo.setUnitPrice(m.getUnitPrice());
        vo.setCategoryId(m.getCategoryId());
        vo.setCategoryName(m.getCategoryName());
        vo.setSupplierId(m.getSupplierId());
        vo.setSupplierName(m.getSupplierName());
        vo.setStatus(m.getStatus());
        vo.setUpdateTime(m.getUpdateTime());

        // 计算阈值比例
        BigDecimal minStock = m.getMinStock();
        BigDecimal stockQty = m.getStockQty();
        if (minStock != null && minStock.compareTo(BigDecimal.ZERO) > 0 && stockQty != null) {
            BigDecimal ratio = stockQty.divide(minStock, 4, RoundingMode.HALF_UP);
            vo.setStockRatio(ratio);
            vo.setWarningRatio(ratio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
        } else {
            vo.setStockRatio(BigDecimal.ZERO);
            vo.setWarningRatio(BigDecimal.ZERO);
        }

        vo.setSeverity(calcSeverity(stockQty, minStock));
        return vo;
    }

    /**
     * 计算严重度
     * CRITICAL: ratio < 0.3
     * WARNING:  0.3 <= ratio < 0.8
     * LOW:      0.8 <= ratio < 1.0
     */
    private String calcSeverity(BigDecimal stockQty, BigDecimal minStock) {
        if (minStock == null || minStock.compareTo(BigDecimal.ZERO) <= 0) {
            return WarningMaterialVO.SEVERITY_LOW;
        }
        if (stockQty == null) {
            stockQty = BigDecimal.ZERO;
        }
        BigDecimal ratio = stockQty.divide(minStock, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.3")) < 0) {
            return WarningMaterialVO.SEVERITY_CRITICAL;
        } else if (ratio.compareTo(new BigDecimal("0.8")) < 0) {
            return WarningMaterialVO.SEVERITY_WARNING;
        }
        return WarningMaterialVO.SEVERITY_LOW;
    }

    /**
     * 批量填充食材的分类名称和供应商名称
     */
    private void fillCategoryAndSupplier(List<Material> materials) {
        if (CollectionUtils.isEmpty(materials)) return;

        List<Long> categoryIds = materials.stream()
                .map(Material::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        List<Long> supplierIds = materials.stream()
                .map(Material::getSupplierId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> categoryNameMap = CollectionUtils.isEmpty(categoryIds) ? null
                : materialCategoryMapper.selectList(new LambdaQueryWrapper<MaterialCategory>()
                        .in(MaterialCategory::getId, categoryIds))
                .stream().collect(Collectors.toMap(
                        MaterialCategory::getId,
                        MaterialCategory::getName,
                        (v1, v2) -> v1));

        Map<Long, String> supplierNameMap = CollectionUtils.isEmpty(supplierIds) ? null
                : supplierMapper.selectList(new LambdaQueryWrapper<Supplier>()
                        .in(Supplier::getId, supplierIds))
                .stream().collect(Collectors.toMap(
                        Supplier::getId,
                        Supplier::getName,
                        (v1, v2) -> v1));

        for (Material m : materials) {
            if (categoryNameMap != null && m.getCategoryId() != null) {
                m.setCategoryName(categoryNameMap.get(m.getCategoryId()));
            }
            if (supplierNameMap != null && m.getSupplierId() != null) {
                m.setSupplierName(supplierNameMap.get(m.getSupplierId()));
            }
        }
    }
}