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
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-franchise.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class FranchiseSettlementTest {

    /** 当前结算周期：动态取系统月份（yyyy-MM），与造数订单的 orderTime=now() 同月，
     *  避免硬编码 CURRENT_PERIOD 在跨月后导致订单落不到查询区间、orderCount=0 的回归。 */
    private static final String CURRENT_PERIOD = java.time.YearMonth.now().toString();

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

        FranchiseSettlement st = franchiseSettlementService.generateSettlement(c.getId(), CURRENT_PERIOD);

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

        FranchiseSettlement st = franchiseSettlementService.generateSettlement(c.getId(), CURRENT_PERIOD);
        // 固定抽成 3000
        assertEquals(0, new BigDecimal("3000.00").compareTo(st.getCommissionAmount()));
        assertEquals(0, new BigDecimal("17000.00").compareTo(st.getSettleAmount()));
    }

    @Test
    void testGenerateSettlementIdempotent() {
        FranchiseContract c = prepareContract(FranchiseContract.COMMISSION_TYPE_RATE);
        createCompletedOrder(2L, new BigDecimal("8000.00"));

        FranchiseSettlement first = franchiseSettlementService.generateSettlement(c.getId(), CURRENT_PERIOD);
        FranchiseSettlement second = franchiseSettlementService.generateSettlement(c.getId(), CURRENT_PERIOD);
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

        FranchiseSettlement st = franchiseSettlementService.generateSettlement(c.getId(), CURRENT_PERIOD);
        assertEquals(1, st.getOrderCount());
        assertEquals(0, new BigDecimal("6000.00").compareTo(st.getSalesAmount()));
    }

    @Test
    void testConfirmAndSettle() {
        FranchiseContract c = prepareContract(FranchiseContract.COMMISSION_TYPE_RATE);
        createCompletedOrder(2L, new BigDecimal("10000.00"));
        FranchiseSettlement st = franchiseSettlementService.generateSettlement(c.getId(), CURRENT_PERIOD);

        franchiseSettlementService.confirmSettlement(st.getId());
        FranchiseSettlement confirmed = franchiseSettlementService.getById(st.getId());
        assertEquals(FranchiseSettlement.STATUS_CONFIRMED, confirmed.getStatus());
        assertNotNull(confirmed.getConfirmTime());

        franchiseSettlementService.settleSettlement(st.getId());
        FranchiseSettlement settled = franchiseSettlementService.getById(st.getId());
        assertEquals(FranchiseSettlement.STATUS_SETTLED, settled.getStatus());
        assertNotNull(settled.getSettleTime());
    }

    @Test
    void testStatsAggregation() {
        // 加盟商：启用 2 个、禁用 1 个；其中 2 个有生效合同
        Franchisee f1 = new Franchisee();
        f1.setTenantId(1L); f1.setName("加盟商A"); f1.setStatus(Franchisee.STATUS_ENABLED);
        franchiseeService.save(f1);
        Franchisee f2 = new Franchisee();
        f2.setTenantId(1L); f2.setName("加盟商B"); f2.setStatus(Franchisee.STATUS_ENABLED);
        franchiseeService.save(f2);
        Franchisee f3 = new Franchisee();
        f3.setTenantId(1L); f3.setName("加盟商C"); f3.setStatus(Franchisee.STATUS_DISABLED);
        franchiseeService.save(f3);

        FranchiseContract c1 = new FranchiseContract();
        c1.setTenantId(1L); c1.setFranchiseeId(f1.getId()); c1.setStoreTenantId(2L);
        c1.setContractNo("FR-A"); c1.setSettleCycle(FranchiseContract.SETTLE_CYCLE_MONTHLY);
        c1.setStatus(FranchiseContract.STATUS_ACTIVE);
        franchiseContractService.save(c1);
        FranchiseContract c2 = new FranchiseContract();
        c2.setTenantId(1L); c2.setFranchiseeId(f1.getId()); c2.setStoreTenantId(2L);
        c2.setContractNo("FR-B"); c2.setSettleCycle(FranchiseContract.SETTLE_CYCLE_MONTHLY);
        c2.setStatus(FranchiseContract.STATUS_ACTIVE);
        franchiseContractService.save(c2);
        FranchiseContract c3 = new FranchiseContract();
        c3.setTenantId(1L); c3.setFranchiseeId(f3.getId()); c3.setStoreTenantId(2L);
        c3.setContractNo("FR-C"); c3.setSettleCycle(FranchiseContract.SETTLE_CYCLE_MONTHLY);
        c3.setStatus(FranchiseContract.STATUS_TERMINATED);
        franchiseContractService.save(c3);

        // 结算单：1 待确认、1 已确认、1 已结算
        createCompletedOrder(2L, new BigDecimal("5000.00"));
        FranchiseSettlement s1 = franchiseSettlementService.generateSettlement(c1.getId(), CURRENT_PERIOD);
        franchiseSettlementService.generateSettlement(c2.getId(), CURRENT_PERIOD);
        franchiseSettlementService.confirmSettlement(s1.getId());
        franchiseSettlementService.settleSettlement(s1.getId());

        // 加盟商统计
        java.util.Map<String, Object> feStat = franchiseeService.statFranchisees(1L);
        assertEquals(3, ((Number) feStat.get("total")).intValue());
        assertEquals(2, ((Number) feStat.get("enabled")).intValue());
        assertEquals(1, ((Number) feStat.get("disabled")).intValue());
        // 有合同（含已终止）的加盟商去重 = 2（A、C）
        assertEquals(2, statInt(feStat, "contractCount"));

        // 合同统计
        java.util.Map<String, Object> ctStat = franchiseContractService.statContracts(1L);
        assertEquals(3, ((Number) ctStat.get("total")).intValue());
        assertEquals(2, ((Number) ctStat.get("active")).intValue());
        assertEquals(1, ((Number) ctStat.get("expired")).intValue());
        assertEquals(2, statInt(ctStat, "franchiseeCount"));

        // 结算单统计：s1 待确认 → 确认 → 结算（settled），s2 仍待确认（pending）
        java.util.Map<String, Object> stStat = franchiseSettlementService.statSettlements(1L);
        assertEquals(2, ((Number) stStat.get("total")).intValue());
        assertEquals(1, ((Number) stStat.get("pending")).intValue());
        assertEquals(0, ((Number) stStat.get("confirmed")).intValue());
        assertEquals(1, ((Number) stStat.get("settled")).intValue());
    }

    /**
     * 读取聚合统计 Map 字段。H2 测试库 DATABASE_TO_LOWER=TRUE 会把未加引号的列别名
     * 转为小写（如 contractCount → contractcount），MySQL 保留原始大小写，此处兼容两种 key。
     */
    private static int statInt(java.util.Map<String, Object> map, String key) {
        Object v = map.get(key);
        if (v == null) {
            v = map.get(key.toLowerCase());
        }
        assertNotNull(v, "聚合字段 " + key + " 不应为 null");
        return ((Number) v).intValue();
    }

    @Test
    void testStatsIsolatedByTenant() {
        Franchisee f = new Franchisee();
        f.setTenantId(1L); f.setName("租户1加盟商"); f.setStatus(Franchisee.STATUS_ENABLED);
        franchiseeService.save(f);
        Franchisee other = new Franchisee();
        other.setTenantId(99L); other.setName("其他租户加盟商"); other.setStatus(Franchisee.STATUS_ENABLED);
        franchiseeService.save(other);

        // 租户 1 只看得到自己的 1 个加盟商
        java.util.Map<String, Object> stat = franchiseeService.statFranchisees(1L);
        assertEquals(1, ((Number) stat.get("total")).intValue());
    }
}
