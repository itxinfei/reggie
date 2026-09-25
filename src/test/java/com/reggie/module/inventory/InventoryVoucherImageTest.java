package com.reggie.module.inventory;

import com.reggie.common.BaseContext;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.model.StockCheck;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.enums.StockRecordType;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.service.PurchaseOrderService;
import com.reggie.module.inventory.service.StockCheckService;
import com.reggie.module.inventory.service.StockRecordService;
import com.reggie.module.inventory.service.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 库存纸质单据凭证图片字段测试：
 * 供应商资质 / 出入库流水 / 盘点单 / 采购单 的多图（逗号分隔）保存与回显。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-inventory.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class InventoryVoucherImageTest {

    private static final String MULTI = "images/purchase/a1.png,images/purchase/a2.png";

    @Autowired
    private SupplierService supplierService;
    @Autowired
    private MaterialService materialService;
    @Autowired
    private StockRecordService stockRecordService;
    @Autowired
    private StockCheckService stockCheckService;
    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(999L);
    }

    private Material prepareMaterial(String name) {
        Material material = new Material();
        material.setTenantId(999L);
        material.setCategoryId(1L);
        material.setName(name);
        material.setUnit("瓶");
        material.setStockQty(BigDecimal.ZERO);
        material.setMinStock(new BigDecimal("5"));
        material.setStatus(1);
        materialService.save(material);
        return material;
    }

    @Test
    void testSupplierLicenseImages() {
        Supplier supplier = new Supplier();
        supplier.setTenantId(999L);
        supplier.setName("资质供应商");
        supplier.setContact("王五");
        supplier.setPhone("13500135000");
        supplier.setStatus(1);
        supplier.setLicenseImages("images/supplier/lic1.png,images/supplier/lic2.png");
        supplierService.save(supplier);

        Supplier found = supplierService.getById(supplier.getId());
        assertEquals("images/supplier/lic1.png,images/supplier/lic2.png", found.getLicenseImages());
    }

    @Test
    void testStockInVoucherImages() {
        Material material = prepareMaterial("酱油");

        stockRecordService.stockIn(material.getId(), new BigDecimal("20"),
                new BigDecimal("8.00"), null, "采购入库", "admin", MULTI);

        List<StockRecord> records = stockRecordService.list();
        assertEquals(1, records.size());
        assertEquals(MULTI, records.get(0).getVoucherImages());
    }

    @Test
    void testCreateCheckVoucherImages() {
        StockCheck check = stockCheckService.createCheck("admin", "月末盘点", MULTI);
        assertNotNull(check.getId());

        StockCheck found = stockCheckService.getById(check.getId());
        assertEquals(MULTI, found.getVoucherImages());
    }

    @Test
    void testCreateOrderVoucherImages() {
        Supplier supplier = new Supplier();
        supplier.setTenantId(999L);
        supplier.setName("采购供应商");
        supplier.setContact("赵六");
        supplier.setPhone("13600136000");
        supplier.setStatus(1);
        supplierService.save(supplier);

        PurchaseOrder order = purchaseOrderService.createOrder(supplier.getId(), "admin", "首批采购", MULTI);
        assertNotNull(order.getId());

        PurchaseOrder found = purchaseOrderService.getById(order.getId());
        assertEquals(MULTI, found.getVoucherImages());
        assertEquals(2, found.getVoucherImages().split(",").length);
    }

    @Test
    void testStockOutVoucherImages() {
        Material material = prepareMaterial("食用油");
        // 先足量入库（不带凭证），再出库并携带凭证
        stockRecordService.stockIn(material.getId(), new BigDecimal("50"),
                new BigDecimal("10.00"), null, "采购入库", "admin", null);
        stockRecordService.stockOut(material.getId(), new BigDecimal("10"),
                null, "领用出库", "admin", MULTI);

        List<StockRecord> outs = stockRecordService.lambdaQuery()
                .eq(StockRecord::getType, StockRecordType.OUT.getValue()).list();
        assertEquals(1, outs.size());
        assertEquals(MULTI, outs.get(0).getVoucherImages());
    }
}
