package com.reggie.module.inventory;

import com.reggie.common.BaseContext;
import com.reggie.dto.StockCheckItemDTO;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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
        assertTrue(list.size() >= 3);
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

        purchaseOrderService.addDetail(po.getId(), material.getId(),
            new BigDecimal("10"), new BigDecimal("50.00"));

        List<PurchaseOrderDetail> details = purchaseOrderDetailService.list();
        assertEquals(1, details.size());

        po.setStatus("ORDERED");
        purchaseOrderService.updateById(po);

        purchaseOrderService.receiveOrder(po.getId());

        PurchaseOrder received = purchaseOrderService.getById(po.getId());
        assertEquals("RECEIVED", received.getStatus());

        Material updated = materialService.getById(material.getId());
        assertEquals(0, new BigDecimal("10").compareTo(updated.getStockQty()));
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
}
