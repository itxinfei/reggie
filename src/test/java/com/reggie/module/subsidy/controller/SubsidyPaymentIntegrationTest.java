package com.reggie.module.subsidy.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.controller.BaseControllerTest;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.payment.service.PaymentOrderService;
import com.reggie.module.payment.service.RefundService;
import com.reggie.module.subsidy.model.MealSubsidyAccount;
import com.reggie.module.subsidy.model.MealSubsidyRecord;
import com.reggie.module.subsidy.service.SubsidyService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.mapper.UserMapper;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.test.TestIdGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 餐补发放 → 餐补支付 → 取消退款回充 全链路集成测试（企业内部订餐）。
 * <p>支付链路复用 {@code PaymentController#pay}（channel=SUBSIDY 同步扣款），
 * 退款复用 {@code RefundService#refundByOrder}（离线渠道本地记账 + 餐补回充）。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = {"classpath:schema.sql", "classpath:schema-subsidy.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SubsidyPaymentIntegrationTest extends BaseControllerTest {

    private static final long USER_ID = 990301L;
    private static final long OTHER_USER_ID = 990302L;
    private static final long DEPT_ID_FIXED = 990301L; // 仅登记用途，部门真实主键自增

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SubsidyService subsidyService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentOrderService paymentOrderService;

    @Autowired
    private RefundService refundService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private TestIdGuard testIdGuard;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("meal_subsidy_account", "meal_subsidy_record",
                "refund_record", "payment_order", "orders", "user", "department");
        testIdGuard.assertAbsent("user", USER_ID, OTHER_USER_ID);
        // 清限流计数，避免跨用例 429
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (rateLimitKeys != null && !rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
        BaseContext.setCurrentId(USER_ID);
        BaseContext.setCurrentTenantId(999L);
    }

    // ==================== 发放 ====================

    @Test
    @DisplayName("1. 按用户发放餐补 - 建户入账并记 GRANT 流水")
    void testGrant_singleUser() throws Exception {
        insertUser(USER_ID, null);
        mockMvc.perform(withCsrfToken(mockMvc, post("/subsidy/account/grant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", "SUPER_ADMIN");
                            return request;
                        })
                        .content("{\"userId\":" + USER_ID + ",\"amount\":100,\"remark\":\"10月餐补\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        MealSubsidyAccount account = accountOf(USER_ID);
        assertNotNull(account);
        assertEquals(0, new BigDecimal("100").compareTo(account.getBalance()));
        assertEquals(1, recordCount(USER_ID, MealSubsidyRecord.TYPE_GRANT));
    }

    @Test
    @DisplayName("2. 按部门批量发放 - 部门下用户全部入账")
    void testGrant_byDepartment() throws Exception {
        insertUser(USER_ID, insertDepartment("技术部"));
        insertUser(OTHER_USER_ID, null);

        mockMvc.perform(withCsrfToken(mockMvc, post("/subsidy/account/grant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", "SUPER_ADMIN");
                            return request;
                        })
                        .content("{\"departmentId\":" + departmentIdOf(USER_ID) + ",\"amount\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("1 人")));

        assertEquals(0, new BigDecimal("50").compareTo(accountOf(USER_ID).getBalance()));
        // 未挂部门的用户不入账
        assertEquals(null, accountOf(OTHER_USER_ID));
    }

    // ==================== 餐补支付 ====================

    @Test
    @DisplayName("3. 餐补支付 - 扣款成功且订单进入待接单")
    void testPay_bySubsidy() throws Exception {
        insertUser(USER_ID, null);
        subsidyService.grant(grantReq(USER_ID, new BigDecimal("100")), 999L, 1L);
        long orderId = insertPendingOrder(990301L, "30.00");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/pay"))
                        .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"channel\":\"SUBSIDY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.success").value(true));

        assertEquals(Orders.STATUS_ORDERED, orderService.getById(orderId).getStatus(), "支付成功应进入待接单");
        assertEquals(0, new BigDecimal("70").compareTo(accountOf(USER_ID).getBalance()), "余额应扣减为 70");
        assertEquals(1, recordCount(USER_ID, MealSubsidyRecord.TYPE_CONSUME));
    }

    @Test
    @DisplayName("4. 餐补支付 - 余额不足被拒且订单保持待付款")
    void testPay_insufficientBalance() throws Exception {
        insertUser(USER_ID, null);
        subsidyService.grant(grantReq(USER_ID, new BigDecimal("10")), 999L, 1L);
        long orderId = insertPendingOrder(990302L, "30.00");

        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/pay"))
                        .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"channel\":\"SUBSIDY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("余额不足")));

        assertEquals(Orders.STATUS_PENDING_PAY, orderService.getById(orderId).getStatus(), "支付失败订单应保持待付款");
        assertEquals(0, new BigDecimal("10").compareTo(accountOf(USER_ID).getBalance()), "余额不应变动");
    }

    @Test
    @DisplayName("5. 核销幂等 - 同一交易号重复核销只扣一次")
    void testConsume_idempotent() {
        insertUser(USER_ID, null);
        subsidyService.grant(grantReq(USER_ID, new BigDecimal("100")), 999L, 1L);
        subsidyService.consumeForOrder(USER_ID, 999L, 990301L, new BigDecimal("30"), "T-TEST-1");
        subsidyService.consumeForOrder(USER_ID, 999L, 990301L, new BigDecimal("30"), "T-TEST-1");

        assertEquals(0, new BigDecimal("70").compareTo(accountOf(USER_ID).getBalance()), "重复核销不应重复扣款");
        assertEquals(1, recordCount(USER_ID, MealSubsidyRecord.TYPE_CONSUME));
    }

    // ==================== 退款回充 ====================

    @Test
    @DisplayName("6. 订单取消退款 - 餐补余额自动回充")
    void testRefund_creditBack() {
        insertUser(USER_ID, null);
        subsidyService.grant(grantReq(USER_ID, new BigDecimal("100")), 999L, 1L);
        long orderId = insertPendingOrder(990303L, "30.00");
        // 真实链路：先建支付单（tradeNo 由后端生成），再核销并推进支付成功
        com.reggie.module.payment.model.PaymentOrder po =
                paymentOrderService.createPaymentOrder(orderId, "SUBSIDY", new BigDecimal("30"));
        subsidyService.consumeForOrder(USER_ID, 999L, orderId, new BigDecimal("30"), po.getTradeNo());
        paymentOrderService.handlePaymentSuccess(po.getTradeNo(), "SUBSIDY");
        assertEquals(0, new BigDecimal("70").compareTo(accountOf(USER_ID).getBalance()));

        boolean refunded = refundService.refundByOrder(orderId, "测试取消");

        assertTrue(refunded, "退款应成功");
        assertEquals(0, new BigDecimal("100").compareTo(accountOf(USER_ID).getBalance()), "餐补应全额回充");
        assertEquals(1, recordCount(USER_ID, MealSubsidyRecord.TYPE_REFUND));
    }

    // ==================== 越权 ====================

    @Test
    @DisplayName("7. 越权发放 - 跨租户用户被拒")
    void testGrant_crossTenantRejected() throws Exception {
        // 租户 1 的用户（演示数据段之外的高位 ID）；先清历史残留
        cleaner.cleanByCondition("user", "id = ?", 998801L);
        User outsider = new User();
        outsider.setId(998801L);
        outsider.setName("外部用户");
        outsider.setPhone("13888888888");
        outsider.setStatus(1);
        outsider.setTenantId(1L);
        outsider.setCreateTime(LocalDateTime.now());
        outsider.setUpdateTime(LocalDateTime.now());
        userMapper.insert(outsider);

        mockMvc.perform(withCsrfToken(mockMvc, post("/subsidy/account/grant"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", "SUPER_ADMIN");
                            return request;
                        })
                        .content("{\"userId\":998801,\"amount\":100}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("不属于当前租户")));

        assertEquals(null, accountOf(998801L));
        // 清掉跨租户测试数据（cleanTables 只按租户 999 清，租户 1 的这行需显式删除）
        cleaner.cleanByCondition("user", "id = ?", 998801L);
    }

    // ==================== 部门对账 ====================

    @Test
    @DisplayName("8. 部门对账 - 按部门聚合订单与餐补，未挂部门归入未分配")
    void testDepartmentReconciliation() {
        Long deptId = insertDepartment("技术部");
        insertUser(USER_ID, deptId);          // 有部门：发放+下单
        insertUser(OTHER_USER_ID, null);      // 无部门：下单（未分配）

        subsidyService.grant(grantReq(USER_ID, new BigDecimal("100")), 999L, 1L);
        long orderId = insertPendingOrder(990301L, "30.00");
        com.reggie.module.payment.model.PaymentOrder po =
                paymentOrderService.createPaymentOrder(orderId, "SUBSIDY", new BigDecimal("30"));
        subsidyService.consumeForOrder(USER_ID, 999L, orderId, new BigDecimal("30"), po.getTradeNo());
        paymentOrderService.handlePaymentSuccess(po.getTradeNo(), "SUBSIDY");

        // 未分配用户：无餐补账户，用微信渠道语义直接构造已支付订单（handlePaymentSuccess 不涉及餐补）
        long orderId2 = insertPendingOrder(990302L, OTHER_USER_ID, "20.00");
        com.reggie.module.payment.model.PaymentOrder po2 =
                paymentOrderService.createPaymentOrder(orderId2, "WECHAT", new BigDecimal("20"));
        paymentOrderService.handlePaymentSuccess(po2.getTradeNo(), "WECHAT");

        String today = java.time.LocalDate.now().toString();
        java.util.List<java.util.Map<String, Object>> rows =
                subsidyService.departmentReconciliation(999L, today, today);

        assertEquals(2, rows.size(), "应有 2 行：技术部 + 未分配");
        java.util.Map<String, Object> techRow = rows.get(0).get("departmentName").equals("技术部")
                ? rows.get(0) : rows.get(1);
        java.util.Map<String, Object> unassignedRow = rows.get(0).get("departmentName").equals("未分配")
                ? rows.get(0) : rows.get(1);
        assertEquals(1, ((Number) techRow.get("orderCount")).intValue());
        assertEquals(0, new BigDecimal("30").compareTo((BigDecimal) techRow.get("orderAmount")));
        assertEquals(0, new BigDecimal("100").compareTo((BigDecimal) techRow.get("subsidyGranted")));
        assertEquals(0, new BigDecimal("30").compareTo((BigDecimal) techRow.get("subsidyUsed")));
        assertEquals(0, new BigDecimal("0").compareTo((BigDecimal) techRow.get("selfPay")));
        assertEquals(1, ((Number) unassignedRow.get("orderCount")).intValue());
        assertEquals(0, new BigDecimal("20").compareTo((BigDecimal) unassignedRow.get("orderAmount")));
        assertEquals(0, BigDecimal.ZERO.compareTo((BigDecimal) unassignedRow.get("subsidyUsed")));
    }

    @Test
    @DisplayName("9. 部门对账 - 非法日期被拒")
    void testDepartmentReconciliation_invalidDate() {
        org.junit.jupiter.api.Assertions.assertThrows(com.reggie.common.CustomException.class,
                () -> subsidyService.departmentReconciliation(999L, "2026/10/01", "2026-10-31"));
        org.junit.jupiter.api.Assertions.assertThrows(com.reggie.common.CustomException.class,
                () -> subsidyService.departmentReconciliation(999L, "2026-10-31", "2026-10-01"));
    }

    // ==================== 辅助 ====================

    private com.reggie.module.subsidy.dto.SubsidyGrantDTO grantReq(Long userId, BigDecimal amount) {
        com.reggie.module.subsidy.dto.SubsidyGrantDTO dto = new com.reggie.module.subsidy.dto.SubsidyGrantDTO();
        dto.setUserId(userId);
        dto.setAmount(amount);
        return dto;
    }

    private void insertUser(long id, Long departmentId) {
        User user = new User();
        user.setId(id);
        user.setName("餐补测试用户" + id);
        user.setPhone("13900" + String.format("%06d", id % 1000000));
        user.setStatus(1);
        user.setTenantId(999L);
        user.setDepartmentId(departmentId);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
    }

    private Long insertDepartment(String name) {
        com.reggie.module.sys.dto.DepartmentSaveDTO dto = new com.reggie.module.sys.dto.DepartmentSaveDTO();
        dto.setName(name);
        departmentService().addTenantDepartment(dto);
        com.reggie.module.sys.model.Department dept = departmentService().getOne(
                new LambdaQueryWrapper<com.reggie.module.sys.model.Department>()
                        .eq(com.reggie.module.sys.model.Department::getName, name));
        assertNotNull(dept);
        return dept.getId();
    }

    private com.reggie.module.sys.service.DepartmentService departmentService() {
        return appContext.getBean(com.reggie.module.sys.service.DepartmentService.class);
    }

    private long insertPendingOrder(long orderId, String amount) {
        return insertPendingOrder(orderId, USER_ID, amount);
    }

    private long insertPendingOrder(long orderId, long userId, String amount) {
        Orders order = new Orders();
        order.setId(orderId);
        order.setNumber("SUB" + orderId);
        order.setUserId(userId);
        order.setStatus(Orders.STATUS_PENDING_PAY);
        order.setAmount(new BigDecimal(amount));
        order.setOrderTime(LocalDateTime.now());
        order.setUserName("测试用户");
        order.setPhone("13800138000");
        order.setAddress("测试地址");
        order.setConsignee("收餐人");
        order.setSource("TAKEOUT");
        order.setTenantId(999L);
        orderService.save(order);
        return orderId;
    }

    private MealSubsidyAccount accountOf(Long userId) {
        return subsidyService.getOne(new LambdaQueryWrapper<MealSubsidyAccount>()
                .eq(MealSubsidyAccount::getTenantId, 999L)
                .eq(MealSubsidyAccount::getUserId, userId));
    }

    private long recordCount(Long userId, String type) {
        return recordMapper.selectCount(new LambdaQueryWrapper<MealSubsidyRecord>()
                .eq(MealSubsidyRecord::getTenantId, 999L)
                .eq(MealSubsidyRecord::getUserId, userId)
                .eq(MealSubsidyRecord::getRecordType, type));
    }

    @Autowired
    private com.reggie.module.subsidy.mapper.MealSubsidyRecordMapper recordMapper;

    private Long departmentIdOf(Long userId) {
        User user = userMapper.selectById(userId);
        assertNotNull(user);
        return user.getDepartmentId();
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext appContext;
}
