package com.reggie.module.billing;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.module.billing.dto.SubscribeDTO;
import com.reggie.module.billing.model.BillingPlan;
import com.reggie.module.billing.model.TenantSubscription;
import com.reggie.module.billing.service.BillingPlanService;
import com.reggie.module.billing.service.TenantSubscriptionService;
import com.reggie.module.billing.vo.SubscriptionEntitlementVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SaaS 订阅计费模块测试。
 * <p>覆盖：套餐管理与上下架 → 计价 → 订阅/支付开通 → 续费时间顺延 → 取消 → 过期扫描 → 权益额度校验。</p>
 *
 * @author reggie
 * @since 2026-09-12
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-billing.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class BillingServiceTest {

    @Autowired
    private BillingPlanService billingPlanService;

    @Autowired
    private TenantSubscriptionService subscriptionService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("tenant_subscription", "billing_plan");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    /** 构造一个上架套餐 */
    private BillingPlan buildPlan(String code, String name, String monthPrice, String annualPrice,
                                  int maxStores, int maxEmployees) {
        BillingPlan plan = new BillingPlan();
        plan.setPlanCode(code);
        plan.setPlanName(name);
        plan.setPrice(new BigDecimal(monthPrice));
        plan.setAnnualPrice(annualPrice == null ? null : new BigDecimal(annualPrice));
        plan.setMaxStores(maxStores);
        plan.setMaxEmployees(maxEmployees);
        plan.setFeatures("聚合外卖,会员营销");
        plan.setSortOrder(1);
        plan.setStatus(BillingPlan.STATUS_ON);
        return plan;
    }

    @Test
    void testPlanCrudAndUniqueCode() {
        BillingPlan plan = billingPlanService.createPlan(buildPlan("BASIC", "基础版", "99.00", "990.00", 1, 5));
        assertNotNull(plan.getId());

        // 编码重复应拒绝
        assertThrows(CustomException.class, () ->
                billingPlanService.createPlan(buildPlan("BASIC", "基础版2", "89.00", null, 1, 5)));

        // 上下架
        billingPlanService.toggleStatus(plan.getId(), BillingPlan.STATUS_OFF);
        assertEquals(BillingPlan.STATUS_OFF, billingPlanService.getById(plan.getId()).getStatus());

        // 上架列表只包含上架套餐
        BillingPlan standard = billingPlanService.createPlan(buildPlan("STANDARD", "标准版", "299.00", null, 3, 20));
        List<BillingPlan> onShelf = billingPlanService.listOnShelf();
        assertEquals(1, onShelf.size());
        assertEquals("STANDARD", onShelf.get(0).getPlanCode());
        assertNotNull(standard.getId());
    }

    @Test
    void testCalculateAmount() {
        BillingPlan plan = buildPlan("STANDARD", "标准版", "299.00", "2990.00", 3, 20);
        assertEquals(new BigDecimal("299.00"),
                subscriptionService.calculateAmount(plan, TenantSubscription.CYCLE_MONTH));
        assertEquals(new BigDecimal("2990.00"),
                subscriptionService.calculateAmount(plan, TenantSubscription.CYCLE_YEAR));

        // 年价为空时按 月价×12
        BillingPlan noAnnual = buildPlan("BASIC", "基础版", "99.00", null, 1, 5);
        assertEquals(new BigDecimal("1188.00"),
                subscriptionService.calculateAmount(noAnnual, TenantSubscription.CYCLE_YEAR));

        // 非法周期
        assertThrows(CustomException.class, () -> subscriptionService.calculateAmount(plan, 3));
    }

    @Test
    void testSubscribeThenPayActivate() {
        BillingPlan plan = billingPlanService.createPlan(buildPlan("STANDARD", "标准版", "299.00", "2990.00", 3, 20));

        SubscribeDTO dto = new SubscribeDTO();
        dto.setPlanId(plan.getId());
        dto.setBillingCycle(TenantSubscription.CYCLE_MONTH);
        TenantSubscription sub = subscriptionService.subscribe(dto);
        assertEquals(TenantSubscription.STATUS_PENDING, sub.getStatus());
        assertEquals(new BigDecimal("299.00"), sub.getAmount());
        assertNotNull(sub.getOrderNo());
        // 套餐额度快照
        assertEquals(Integer.valueOf(3), sub.getMaxStores());

        // 支付开通
        LocalDateTime before = LocalDateTime.now();
        TenantSubscription active = subscriptionService.markPaid(sub.getId());
        LocalDateTime after = LocalDateTime.now();
        assertEquals(TenantSubscription.STATUS_ACTIVE, active.getStatus());
        assertNotNull(active.getPayTime());
        // 月付：到期时间约为开通后一个月
        assertFalse(active.getEndTime().isBefore(before.plusMonths(1).minusMinutes(1)));
        assertFalse(active.getEndTime().isAfter(after.plusMonths(1).plusMinutes(1)));

        // 可查到当前有效订阅与权益
        assertNotNull(subscriptionService.getActiveSubscription());
        SubscriptionEntitlementVO vo = subscriptionService.getEntitlement();
        assertTrue(vo.isActive());
        assertEquals("STANDARD", vo.getPlanCode());
        assertTrue(vo.getDaysRemaining() >= 27);
    }

    @Test
    void testRenewalExtendsFromCurrentEndTime() {
        BillingPlan plan = billingPlanService.createPlan(buildPlan("STANDARD", "标准版", "299.00", null, 3, 20));

        // 第一次开通月付
        TenantSubscription first = subscriptionService.subscribe(buildSubscribe(plan.getId(), TenantSubscription.CYCLE_MONTH));
        first = subscriptionService.markPaid(first.getId());
        LocalDateTime firstEnd = first.getEndTime();

        // 立即续费第二个月：正确实现应在第一次到期时间上顺延（约 now+2月），而非从 now 重新算（now+1月）
        TenantSubscription second = subscriptionService.subscribe(buildSubscribe(plan.getId(), TenantSubscription.CYCLE_MONTH));
        second = subscriptionService.markPaid(second.getId());

        assertTrue(second.getEndTime().isAfter(firstEnd), "续费后到期时间必须晚于首次到期时间");
        LocalDateTime expected = firstEnd.plusMonths(1);
        long diffSeconds = Math.abs(java.time.Duration.between(expected, second.getEndTime()).getSeconds());
        assertTrue(diffSeconds < 60, "续费应在原到期时间上顺延一个月");
    }

    @Test
    void testCancelAndIllegalTransition() {
        BillingPlan plan = billingPlanService.createPlan(buildPlan("BASIC", "基础版", "99.00", null, 1, 5));
        TenantSubscription sub = subscriptionService.subscribe(buildSubscribe(plan.getId(), TenantSubscription.CYCLE_MONTH));

        // 待支付可取消
        subscriptionService.cancel(sub.getId());
        assertEquals(TenantSubscription.STATUS_CANCELED, subscriptionService.getById(sub.getId()).getStatus());
        // 已取消不能再支付
        assertThrows(CustomException.class, () -> subscriptionService.markPaid(sub.getId()));

        // 已开通的订阅不能取消
        TenantSubscription sub2 = subscriptionService.subscribe(buildSubscribe(plan.getId(), TenantSubscription.CYCLE_MONTH));
        subscriptionService.markPaid(sub2.getId());
        assertThrows(CustomException.class, () -> subscriptionService.cancel(sub2.getId()));
    }

    @Test
    void testOffShelfPlanCannotSubscribe() {
        BillingPlan plan = billingPlanService.createPlan(buildPlan("BASIC", "基础版", "99.00", null, 1, 5));
        billingPlanService.toggleStatus(plan.getId(), BillingPlan.STATUS_OFF);
        assertThrows(CustomException.class,
                () -> subscriptionService.subscribe(buildSubscribe(plan.getId(), TenantSubscription.CYCLE_MONTH)));
    }

    @Test
    void testExpireOverdue() {
        // 直接构造一条已过到期时间但仍"生效中"的订阅
        TenantSubscription overdue = new TenantSubscription();
        overdue.setTenantId(1L);
        overdue.setPlanCode("BASIC");
        overdue.setPlanName("基础版");
        overdue.setOrderNo("SUB_OVERDUE_1");
        overdue.setBillingCycle(TenantSubscription.CYCLE_MONTH);
        overdue.setAmount(new BigDecimal("99.00"));
        overdue.setStatus(TenantSubscription.STATUS_ACTIVE);
        overdue.setStartTime(LocalDateTime.now().minusMonths(2));
        overdue.setEndTime(LocalDateTime.now().minusDays(1));
        subscriptionService.save(overdue);

        int n = subscriptionService.expireOverdue();
        assertTrue(n >= 1);
        assertEquals(TenantSubscription.STATUS_EXPIRED, subscriptionService.getById(overdue.getId()).getStatus());
        // 过期后查不到有效订阅
        assertNull(subscriptionService.getActiveSubscription());
    }

    @Test
    void testEntitlementLimit() {
        // 有效订阅：门店上限 3、员工上限 20
        TenantSubscription active = new TenantSubscription();
        active.setTenantId(1L);
        active.setPlanCode("STANDARD");
        active.setPlanName("标准版");
        active.setOrderNo("SUB_ACTIVE_1");
        active.setBillingCycle(TenantSubscription.CYCLE_MONTH);
        active.setAmount(new BigDecimal("299.00"));
        active.setStatus(TenantSubscription.STATUS_ACTIVE);
        active.setStartTime(LocalDateTime.now().minusDays(1));
        active.setEndTime(LocalDateTime.now().plusDays(29));
        active.setMaxStores(3);
        active.setMaxEmployees(20);
        subscriptionService.save(active);

        assertTrue(subscriptionService.checkStoreLimit(3));
        assertFalse(subscriptionService.checkStoreLimit(4));
        assertTrue(subscriptionService.checkEmployeeLimit(20));
        assertFalse(subscriptionService.checkEmployeeLimit(21));

        // -1 表示不限
        active.setMaxStores(-1);
        subscriptionService.updateById(active);
        assertTrue(subscriptionService.checkStoreLimit(999));
    }

    @Test
    void testNoSubscriptionMeansNoEntitlement() {
        // 无任何有效订阅时，权益校验不通过
        assertFalse(subscriptionService.checkStoreLimit(1));
        assertFalse(subscriptionService.checkEmployeeLimit(1));
        SubscriptionEntitlementVO vo = subscriptionService.getEntitlement();
        assertFalse(vo.isActive());
    }

    private SubscribeDTO buildSubscribe(Long planId, int cycle) {
        SubscribeDTO dto = new SubscribeDTO();
        dto.setPlanId(planId);
        dto.setBillingCycle(cycle);
        dto.setPayChannel("MOCK");
        return dto;
    }
}
