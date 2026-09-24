package com.reggie.module.inventory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.dto.StockCheckItemDTO;
import com.reggie.enums.PurchaseOrderStatus;
import com.reggie.module.inventory.dto.BatchRestockDTO;
import com.reggie.module.inventory.model.*;
import com.reggie.module.inventory.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-inventory.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class InventoryServiceTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private MaterialCategoryService materialCategoryService;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private StockRecordService stockRecordService;

    @Autowired
    private StockCheckService stockCheckService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @Autowired
    private PurchaseOrderDetailService purchaseOrderDetailService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testSupplierCrud() {
        Supplier supplier = new Supplier();
        supplier.setTenantId(1L);
        supplier.setName("新供应商");
        supplier.setContact("李四");
        supplier.setPhone("13900139000");
        supplier.setStatus(1);
        supplierService.save(supplier);
        assertNotNull(supplier.getId());

        Supplier found = supplierService.getById(supplier.getId());
        assertEquals("新供应商", found.getName());

        found.setName("修改后供应商");
        supplierService.updateById(found);
        Supplier updated = supplierService.getById(supplier.getId());
        assertEquals("修改后供应商", updated.getName());
    }

    @Test
    void testMaterialCategoryCrud() {
        MaterialCategory category = new MaterialCategory();
        category.setTenantId(1L);
        category.setName("调味品");
        category.setSort(3);
        materialCategoryService.save(category);
        assertNotNull(category.getId());

        List<MaterialCategory> list = materialCategoryService.list();
        assertTrue(list.size() >= 1);
    }

    @Test
    void testMaterialCrud() {
        Material material = new Material();
        material.setTenantId(1L);
        material.setCategoryId(1L);
        material.setName("大白菜");
        material.setUnit("斤");
        material.setStockQty(BigDecimal.ZERO);
        material.setMinStock(new BigDecimal("10"));
        material.setStatus(1);
        materialService.save(material);
        assertNotNull(material.getId());

        Material found = materialService.getById(material.getId());
        assertEquals("大白菜", found.getName());
    }

    @Test
    void testStockIn() {
        Material material = new Material();
        material.setTenantId(1L);
        material.setCategoryId(1L);
        material.setName("土豆");
        material.setUnit("斤");
        material.setStockQty(BigDecimal.ZERO);
        material.setMinStock(new BigDecimal("10"));
        material.setStatus(1);
        materialService.save(material);

        stockRecordService.stockIn(material.getId(), new BigDecimal("100"),
            new BigDecimal("2.50"), null, "采购入库", "admin");

        Material updated = materialService.getById(material.getId());
        assertEquals(0, new BigDecimal("100").compareTo(updated.getStockQty()));

        List<StockRecord> records = stockRecordService.list();
        assertEquals(1, records.size());
        assertEquals("IN", records.get(0).getType());
    }

    @Test
    void testStockOut() {
        Material material = new Material();
        material.setTenantId(1L);
        material.setCategoryId(1L);
        material.setName("猪肉");
        material.setUnit("斤");
        material.setStockQty(new BigDecimal("50"));
        material.setMinStock(new BigDecimal("10"));
        material.setStatus(1);
        materialService.save(material);

        stockRecordService.stockOut(material.getId(), new BigDecimal("20"),
            null, "领料出库", "admin");

        Material updated = materialService.getById(material.getId());
        assertEquals(0, new BigDecimal("30").compareTo(updated.getStockQty()));
    }

    @Test
    void testStockOutInsufficient() {
        Material material = new Material();
        material.setTenantId(1L);
        material.setCategoryId(1L);
        material.setName("牛肉");
        material.setUnit("斤");
        material.setStockQty(new BigDecimal("5"));
        material.setMinStock(new BigDecimal("10"));
        material.setStatus(1);
        materialService.save(material);

        assertThrows(Exception.class, () -> {
            stockRecordService.stockOut(material.getId(), new BigDecimal("20"),
                null, "出库测试", "admin");
        });
    }

    @Test
    void testStockWarning() {
        Material material = new Material();
        material.setTenantId(1L);
        material.setCategoryId(1L);
        material.setName("鸡蛋");
        material.setUnit("个");
        material.setStockQty(new BigDecimal("5"));
        material.setMinStock(new BigDecimal("10"));
        material.setStatus(1);
        materialService.save(material);

        List<Material> warnings = materialService.checkWarning();
        assertTrue(warnings.stream().anyMatch(m -> m.getId().equals(material.getId())));
    }

    @Test
    void testPurchaseOrderFlow() {
        Material material = new Material();
        material.setTenantId(1L);
        material.setCategoryId(1L);
        material.setName("大米");
        material.setUnit("袋");
        material.setStockQty(new BigDecimal("0"));
        material.setMinStock(new BigDecimal("5"));
        material.setStatus(1);
        materialService.save(material);

        PurchaseOrder po = purchaseOrderService.createOrder(1L, "admin", "测试采购");
        assertNotNull(po.getId());
        assertEquals("DRAFT", po.getStatus());
        assertTrue(po.getOrderNo().startsWith("PO"));

        // 修改点：断言添加明细后总金额实时 = Σ明细金额（10×50=500）
        purchaseOrderService.addDetail(po.getId(), material.getId(),
            new BigDecimal("10"), new BigDecimal("50.00"));
        PurchaseOrder afterFirst = purchaseOrderService.getById(po.getId());
        assertEquals(0, new BigDecimal("500.00").compareTo(afterFirst.getTotalAmount()));

        // 修改点：追加第二条明细，总金额应累加为 500 + 5×100 = 1000
        purchaseOrderService.addDetail(po.getId(), material.getId(),
            new BigDecimal("5"), new BigDecimal("100.00"));
        PurchaseOrder afterSecond = purchaseOrderService.getById(po.getId());
        assertEquals(0, new BigDecimal("1000.00").compareTo(afterSecond.getTotalAmount()));

        List<PurchaseOrderDetail> details = purchaseOrderDetailService.list();
        assertEquals(2, details.size());

        // 修改点：走真实审核流程（内部重算总金额），断言状态与金额
        purchaseOrderService.approveOrder(po.getId());
        PurchaseOrder approved = purchaseOrderService.getById(po.getId());
        assertEquals("ORDERED", approved.getStatus());
        assertEquals(0, new BigDecimal("1000.00").compareTo(approved.getTotalAmount()));

        purchaseOrderService.receiveOrder(po.getId());

        PurchaseOrder received = purchaseOrderService.getById(po.getId());
        assertEquals("RECEIVED", received.getStatus());

        Material updated = materialService.getById(material.getId());
        assertEquals(0, new BigDecimal("15").compareTo(updated.getStockQty()));
    }

    /**
     * 修改点：异常场景——非草稿状态的采购单禁止添加明细
     */
    @Test
    void testAddDetailAfterApproveRejected() {
        Material material = new Material();
        material.setTenantId(1L);
        material.setCategoryId(1L);
        material.setName("面粉");
        material.setUnit("袋");
        material.setStockQty(BigDecimal.ZERO);
        material.setMinStock(new BigDecimal("5"));
        material.setStatus(1);
        materialService.save(material);

        PurchaseOrder po = purchaseOrderService.createOrder(1L, "admin", "异常测试");
        purchaseOrderService.addDetail(po.getId(), material.getId(),
            new BigDecimal("10"), new BigDecimal("20.00"));
        purchaseOrderService.approveOrder(po.getId());

        assertThrows(Exception.class, () ->
            purchaseOrderService.addDetail(po.getId(), material.getId(),
                new BigDecimal("1"), new BigDecimal("1.00")));
    }

    @Test
    void testStockCheck() {
        Material material = new Material();
        material.setTenantId(1L);
        material.setCategoryId(1L);
        material.setName("食用油");
        material.setUnit("桶");
        material.setStockQty(new BigDecimal("10"));
        material.setMinStock(new BigDecimal("2"));
        material.setUnitPrice(new BigDecimal("60.00"));
        material.setStatus(1);
        materialService.save(material);

        StockCheck sc = stockCheckService.createCheck("admin", "月度盘点");
        assertNotNull(sc.getId());
        assertTrue(sc.getCheckNo().startsWith("CK"));

        // 构建盘点明细DTO
        StockCheckItemDTO item = new StockCheckItemDTO();
        item.setMaterialId(material.getId());
        item.setSystemStock(new BigDecimal("10"));   // 系统库存
        item.setActualStock(new BigDecimal("8"));    // 实际盘点数量
        item.setRemark("测试损耗");
        List<StockCheckItemDTO> items = Collections.singletonList(item);
        stockCheckService.completeCheck(sc.getId(), items);

        StockCheck completed = stockCheckService.getById(sc.getId());
        assertEquals("DONE", completed.getStatus());

        Material updated = materialService.getById(material.getId());
        assertEquals(0, new BigDecimal("8").compareTo(updated.getStockQty()));
    }

    @Test
    void testBatchRestockGroupsBySupplier() {
        // 两个供应商
        Supplier s1 = new Supplier();
        s1.setTenantId(1L); s1.setName("补货供应商甲"); s1.setStatus(1);
        supplierService.save(s1);
        Supplier s2 = new Supplier();
        s2.setTenantId(1L); s2.setName("补货供应商乙"); s2.setStatus(1);
        supplierService.save(s2);

        // 三个食材：甲供2个、乙供1个；记录初始库存，验证补货不自动入库
        Material m1 = buildRestockMaterial("补货食材1", s1.getId(), "10");
        Material m2 = buildRestockMaterial("补货食材2", s1.getId(), "20");
        Material m3 = buildRestockMaterial("补货食材3", s2.getId(), "30");

        BatchRestockDTO dto = new BatchRestockDTO();
        dto.setOperator("测试员");
        dto.setRemark("批量补货测试");
        List<BatchRestockDTO.RestockItem> items = new ArrayList<>();
        items.add(restockItem(m1.getId(), "5"));
        items.add(restockItem(m2.getId(), "8"));
        items.add(restockItem(m3.getId(), "3"));
        dto.setItems(items);

        List<String> orderNos = materialService.batchRestock(dto);

        // 按主供应商拆成 2 张采购单
        assertEquals(2, orderNos.size());
        List<PurchaseOrder> orders = purchaseOrderService.list(
                new LambdaQueryWrapper<PurchaseOrder>().in(PurchaseOrder::getOrderNo, orderNos));
        assertEquals(2, orders.size());
        // 全部为 ORDERED（已下单待收货），不是 DRAFT
        List<Long> orderIds = orders.stream().map(PurchaseOrder::getId).collect(Collectors.toList());
        orders.forEach(o -> assertEquals(PurchaseOrderStatus.ORDERED.getValue(), o.getStatus()));
        // 明细共 3 行
        long detailCount = purchaseOrderDetailService.count(new LambdaQueryWrapper<PurchaseOrderDetail>()
                .in(PurchaseOrderDetail::getPurchaseOrderId, orderIds));
        assertEquals(3, detailCount);
        // 库存保持不变（收货时才入库）
        assertEquals(0, new BigDecimal("10").compareTo(materialService.getById(m1.getId()).getStockQty()));
        assertEquals(0, new BigDecimal("20").compareTo(materialService.getById(m2.getId()).getStockQty()));
        assertEquals(0, new BigDecimal("30").compareTo(materialService.getById(m3.getId()).getStockQty()));

        // 未设主供应商的食材补货：应报错而不是挂到任意供应商
        Material m4 = buildRestockMaterial("补货食材4", null, "5");
        BatchRestockDTO dto2 = new BatchRestockDTO();
        List<BatchRestockDTO.RestockItem> items2 = new ArrayList<>();
        items2.add(restockItem(m4.getId(), "1"));
        dto2.setItems(items2);
        assertThrows(CustomException.class, () -> materialService.batchRestock(dto2));
    }

    /** 构造补货测试食材：启用、带单价与初始库存 */
    private Material buildRestockMaterial(String name, Long supplierId, String stockQty) {
        Material m = new Material();
        m.setTenantId(1L);
        m.setName(name);
        m.setUnit("斤");
        m.setSupplierId(supplierId);
        m.setStockQty(new BigDecimal(stockQty));
        m.setUnitPrice(new BigDecimal("5.00"));
        m.setStatus(1);
        materialService.save(m);
        return m;
    }

    private BatchRestockDTO.RestockItem restockItem(Long materialId, String qty) {
        BatchRestockDTO.RestockItem item = new BatchRestockDTO.RestockItem();
        item.setMaterialId(materialId);
        item.setQty(qty);
        return item;
    }
}

