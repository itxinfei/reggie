package com.reggie.module.inventory;

import com.reggie.common.BaseContext;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.module.inventory.model.SupplierSettlement;
import com.reggie.module.inventory.service.SupplierService;
import com.reggie.module.inventory.service.SupplierSettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 供应商结算单元测试
 * <p>覆盖：创建结算单、付款（累加已付金额、满额自动置PAID）、分页查询。</p>
 *
 * @author 心飞为你飞
 * @since 2026-09-01
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema-inventory.sql", "classpath:schema-groupbuy-withdraw.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SupplierSettlementTest {

    @Autowired
    private SupplierService supplierService;

    @Autowired
    private SupplierSettlementService settlementService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void createSettlement_valid_createsPending() {
        // 准备供应商
        Supplier supplier = new Supplier();
        supplier.setTenantId(1L);
        supplier.setName("测试供应商");
        supplier.setContact("张三");
        supplier.setPhone("13900139000");
        supplier.setStatus(1);
        supplierService.save(supplier);

        // 创建结算单
        SupplierSettlement settlement = new SupplierSettlement();
        settlement.setSupplierId(supplier.getId());
        settlement.setPeriod("202609");
        settlement.setTotalAmount(new BigDecimal("5000.00"));
        settlement.setStatus("PENDING");

        SupplierSettlement saved = settlementService.createSettlement(settlement);
        assertNotNull(saved.getId());
        assertEquals("PENDING", saved.getStatus());
        assertEquals(new BigDecimal("5000.00"), saved.getTotalAmount());
        assertEquals(BigDecimal.ZERO, saved.getPaidAmount());
    }

    @Test
    void paySettlement_partialPayment_updatesPaidAmount() {
        // 准备供应商和结算单
        Supplier supplier = new Supplier();
        supplier.setTenantId(1L);
        supplier.setName("部分付款测试");
        supplier.setContact("李四");
        supplier.setPhone("13900139001");
        supplier.setStatus(1);
        supplierService.save(supplier);

        SupplierSettlement settlement = new SupplierSettlement();
        settlement.setSupplierId(supplier.getId());
        settlement.setPeriod("202609");
        settlement.setTotalAmount(new BigDecimal("5000.00"));
        settlement.setStatus("PENDING");
        settlement = settlementService.createSettlement(settlement);

        // 部分付款 2000
        SupplierSettlement paid = settlementService.paySettlement(settlement.getId(), new BigDecimal("2000.00"));
        assertEquals(new BigDecimal("2000.00"), paid.getPaidAmount());
        assertEquals("PENDING", paid.getStatus()); // 未付满，仍为PENDING
    }

    @Test
    void paySettlement_fullPayment_marksPaid() {
        // 准备供应商和结算单
        Supplier supplier = new Supplier();
        supplier.setTenantId(1L);
        supplier.setName("全额付款测试");
        supplier.setContact("王五");
        supplier.setPhone("13900139002");
        supplier.setStatus(1);
        supplierService.save(supplier);

        SupplierSettlement settlement = new SupplierSettlement();
        settlement.setSupplierId(supplier.getId());
        settlement.setPeriod("202609");
        settlement.setTotalAmount(new BigDecimal("3000.00"));
        settlement.setStatus("PENDING");
        settlement = settlementService.createSettlement(settlement);

        // 第一次付款 1500
        settlementService.paySettlement(settlement.getId(), new BigDecimal("1500.00"));

        // 第二次付款 1500，应满额自动置PAID
        SupplierSettlement paid = settlementService.paySettlement(settlement.getId(), new BigDecimal("1500.00"));
        assertEquals(new BigDecimal("3000.00"), paid.getPaidAmount());
        assertEquals("PAID", paid.getStatus());
    }

    @Test
    void paySettlement_overPayment_allowsOverpayment() {
        // 准备供应商和结算单
        Supplier supplier = new Supplier();
        supplier.setTenantId(1L);
        supplier.setName("超额付款测试");
        supplier.setContact("赵六");
        supplier.setPhone("13900139003");
        supplier.setStatus(1);
        supplierService.save(supplier);

        SupplierSettlement settlement = new SupplierSettlement();
        settlement.setSupplierId(supplier.getId());
        settlement.setPeriod("202609");
        settlement.setTotalAmount(new BigDecimal("1000.00"));
        settlement.setStatus("PENDING");
        settlement = settlementService.createSettlement(settlement);

        // 超额付款 1500
        SupplierSettlement paid = settlementService.paySettlement(settlement.getId(), new BigDecimal("1500.00"));
        assertEquals(new BigDecimal("1500.00"), paid.getPaidAmount());
        assertEquals("PAID", paid.getStatus()); // 超过总额也置PAID
    }

    @Test
    void pageSettlements_findsBySupplier() {
        // 准备两个供应商
        Supplier supplier1 = new Supplier();
        supplier1.setTenantId(1L);
        supplier1.setName("供应商A");
        supplier1.setContact("用户A");
        supplier1.setPhone("13900139004");
        supplier1.setStatus(1);
        supplierService.save(supplier1);

        Supplier supplier2 = new Supplier();
        supplier2.setTenantId(1L);
        supplier2.setName("供应商B");
        supplier2.setContact("用户B");
        supplier2.setPhone("13900139005");
        supplier2.setStatus(1);
        supplierService.save(supplier2);

        // 创建结算单
        SupplierSettlement s1 = new SupplierSettlement();
        s1.setSupplierId(supplier1.getId());
        s1.setPeriod("202609");
        s1.setTotalAmount(new BigDecimal("1000.00"));
        s1.setStatus("PENDING");
        settlementService.createSettlement(s1);

        SupplierSettlement s2 = new SupplierSettlement();
        s2.setSupplierId(supplier2.getId());
        s2.setPeriod("202609");
        s2.setTotalAmount(new BigDecimal("2000.00"));
        s2.setStatus("PENDING");
        settlementService.createSettlement(s2);

        // 按供应商查询
        Page<SupplierSettlement> page = settlementService.pageSettlements(1, 10, supplier1.getId(), null);
        List<SupplierSettlement> results = page.getRecords();
        assertFalse(results.isEmpty());
        assertEquals(1, results.size());
        assertEquals(supplier1.getId(), results.get(0).getSupplierId());
    }

    @Test
    void pageSettlements_findsByStatus() {
        // 准备供应商
        Supplier supplier = new Supplier();
        supplier.setTenantId(1L);
        supplier.setName("状态筛选测试");
        supplier.setContact("测试");
        supplier.setPhone("13900139006");
        supplier.setStatus(1);
        supplierService.save(supplier);

        // 创建PENDING结算单
        SupplierSettlement pending = new SupplierSettlement();
        pending.setSupplierId(supplier.getId());
        pending.setPeriod("202609");
        pending.setTotalAmount(new BigDecimal("1000.00"));
        pending.setStatus("PENDING");
        pending = settlementService.createSettlement(pending);

        // 创建PAID结算单
        SupplierSettlement paid = new SupplierSettlement();
        paid.setSupplierId(supplier.getId());
        paid.setPeriod("202610");
        paid.setTotalAmount(new BigDecimal("2000.00"));
        paid.setStatus("PENDING");
        paid = settlementService.createSettlement(paid);
        settlementService.paySettlement(paid.getId(), paid.getTotalAmount());

        // 查询PENDING
        Page<SupplierSettlement> pendingPage = settlementService.pageSettlements(1, 10, null, "PENDING");
        final Long pendingId = pending.getId();
        assertTrue(pendingPage.getRecords().stream().anyMatch(item -> item.getId().equals(pendingId)));

        // 查询PAID
        Page<SupplierSettlement> paidPage = settlementService.pageSettlements(1, 10, null, "PAID");
        final Long paidId = paid.getId();
        assertTrue(paidPage.getRecords().stream().anyMatch(item -> item.getId().equals(paidId)));
    }
}
