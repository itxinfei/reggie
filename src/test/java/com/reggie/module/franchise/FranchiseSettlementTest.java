package com.reggie.module.franchise;

import com.reggie.common.BaseContext;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.module.franchise.model.FranchiseContract;
import com.reggie.module.franchise.model.FranchiseSettlement;
import com.reggie.module.franchise.model.Franchisee;
import com.reggie.module.franchise.service.FranchiseContractService;
import com.reggie.module.franchise.service.FranchiseSettlementService;
import com.reggie.module.franchise.service.FranchiseeService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 加盟分账模块测试
 * <p>覆盖：加盟商/合同 CRUD → 生成结算单（聚合已完成订单）→ 确认 → 结算 → 幂等。</p>
 *
 * @author reggie
 * @since 2026-08-15
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = "classpath:schema-franchise.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FranchiseSettlementTest {

    @Autowired
    private FranchiseeService franchiseeService;

    @Autowired
    private FranchiseContractService franchiseContractService;

    @Autowired
    private FranchiseSettlementService franchiseSettlementService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("franchise_settlement", "franchise_contract", "franchisee");
        cleaner.cleanByCondition("orders", "tenant_id = ?", 2L);
        cleaner.cleanByCondition("order_detail", "tenant_id = ?", 2L);
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    /** 准备加盟商 + 生效合同（比例抽成 5%） */
    private FranchiseContract prepareContract(int commissionType) {
        Franchisee f = new Franchisee();
        f.setTenantId(1L);
        f.setName("测试加盟商");
        f.setContactPerson("张三");
        f.setContactPhone("13800138000");
        f.setStatus(Franchisee.STATUS_ENABLED);
        franchiseeService.save(f);

        FranchiseContract c = new FranchiseContract();
        c.setTenantId(1L);
        c.setFranchiseeId(f.getId());
        c.setStoreTenantId(2L); // 加盟门店租户
        c.setContractNo("FR-TEST-" + System.currentTimeMillis());
        c.setCommissionType(commissionType);
        if (commissionType == FranchiseContract.COMMISSION_TYPE_RATE) {
            c.setCommissionRate(new BigDecimal("0.0500"));
        } else {
            c.setCommissionAmount(new BigDecimal("3000.00"));
        }
        c.setSettleCycle(FranchiseContract.SETTLE_CYCLE_MONTHLY);
        c.setStatus(FranchiseContract.STATUS_ACTIVE);
        franchiseContractService.save(c);
        return c;
    }

    /** 为指定门店租户造一笔已完成订单 */
    private void createCompletedOrder(Long storeTenantId, BigDecimal amount) {
        Orders order = new Orders();
        order.setStatus(Orders.STATUS_COMPLETED);
        order.setAmount(amount);
        order.setTenantId(storeTenantId);
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        orderService.save(order);
    }

    @Test
    void testGenerateSettlementByRate() {
        FranchiseContract c = prepareContract(FranchiseContract.COMMISSION_TYPE_RATE);
        createCompletedOrder(2L, new BigDecimal("10000.00"));
        createCompletedOrder(2L, new BigDecimal("5000.00"));

        FranchiseSettlement st = franchiseSettlementService.generateSettlement(c.getId(), "2026-08");

        assertNotNull(st.getId());
        assertEquals(2, st.getOrderCount());
        assertEquals(0, new BigDecimal("15000.00").compareTo(st.getSalesAmount()));
        // 5% 抽成 = 750
        assertEquals(0, new BigDecimal("750.00").compareTo(st.getCommissionAmount()));
        // 结算金额 = 15000 - 750 = 14250
        assertEquals(0, new BigDecimal("14250.00").compareTo(st.getSettleAmount()));
        assertEquals(FranchiseSettlement.STATUS_PENDING, st.getStatus());
    }

    @Test
    void testGenerateSettlementByFixedAmount() {
        FranchiseContract c = prepareContract(FranchiseContract.COMMISSION_TYPE_FIXED);
        createCompletedOrder(2L, new BigDecimal("20000.00"));

        FranchiseSettlement st = franchiseSettlementService.generateSettlement(c.getId(), "2026-08");
        // 固定抽成 3000
        assertEquals(0, new BigDecimal("3000.00").compareTo(st.getCommissionAmount()));
        assertEquals(0, new BigDecimal("17000.00").compareTo(st.getSettleAmount()));
    }

    @Test
    void testGenerateSettlementIdempotent() {
        FranchiseContract c = prepareContract(FranchiseContract.COMMISSION_TYPE_RATE);
        createCompletedOrder(2L, new BigDecimal("8000.00"));

        FranchiseSettlement first = franchiseSettlementService.generateSettlement(c.getId(), "2026-08");
        FranchiseSettlement second = franchiseSettlementService.generateSettlement(c.getId(), "2026-08");
        assertEquals(first.getId(), second.getId());
    }

    @Test
    void testGenerateSettlementExcludesUncompletedOrders() {
        FranchiseContract c = prepareContract(FranchiseContract.COMMISSION_TYPE_RATE);
        createCompletedOrder(2L, new BigDecimal("6000.00"));
        // 未完成订单不应计入
        Orders pending = new Orders();
        pending.setStatus(Orders.STATUS_ORDERED);
        pending.setAmount(new BigDecimal("9999.00"));
        pending.setTenantId(2L);
        pending.setUserId(1L);
        pending.setOrderTime(LocalDateTime.now());
        orderService.save(pending);

        FranchiseSettlement st = franchiseSettlementService.generateSettlement(c.getId(), "2026-08");
        assertEquals(1, st.getOrderCount());
        assertEquals(0, new BigDecimal("6000.00").compareTo(st.getSalesAmount()));
    }

    @Test
    void testConfirmAndSettle() {
        FranchiseContract c = prepareContract(FranchiseContract.COMMISSION_TYPE_RATE);
        createCompletedOrder(2L, new BigDecimal("10000.00"));
        FranchiseSettlement st = franchiseSettlementService.generateSettlement(c.getId(), "2026-08");

        franchiseSettlementService.confirmSettlement(st.getId());
        FranchiseSettlement confirmed = franchiseSettlementService.getById(st.getId());
        assertEquals(FranchiseSettlement.STATUS_CONFIRMED, confirmed.getStatus());
        assertNotNull(confirmed.getConfirmTime());

        franchiseSettlementService.settleSettlement(st.getId());
        FranchiseSettlement settled = franchiseSettlementService.getById(st.getId());
        assertEquals(FranchiseSettlement.STATUS_SETTLED, settled.getStatus());
        assertNotNull(settled.getSettleTime());
    }
}
