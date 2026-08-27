package com.reggie.module.cashier.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CsrfTokenUtil;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.cashier.mapper.DailySettlementMapper;
import com.reggie.module.cashier.model.CashierRecord;
import com.reggie.module.cashier.model.DailySettlement;
import com.reggie.module.cashier.service.CashierService;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CashierController 测试 — 收银管理
 *
 * 测试策略：
 * - 使用真实 MySQL 数据库（application-test.yml + jdbc:mysql://localhost:3306/reggie）
 * - schema-cashier.sql 通过 @Sql 在每个测试方法前执行建表
 * - @Transactional 每个测试方法回滚，天然隔离
 * - 仅用 sessionAttr 注入：employee=1L、tenantId=1L 触发 LoginCheckFilter 设置 BaseContext
 * - 写操作（POST/PUT/DELETE）通过 withCsrfToken 注入有效的 CSRF token
 *
 * @author reggie
 * @since 2026-08-27
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:schema-cashier.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class CashierControllerTest {

    private static final String CSRF_TOKEN_SESSION_KEY = "csrfToken";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CashierService cashierService;

    @Autowired
    private DailySettlementMapper settlementMapper;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("cashier_record", "daily_settlement");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    // ==================== 收银记录管理 ====================

    @Test
    @DisplayName("1. 获取收银记录列表 - 空列表")
    void testGetCashierRecordList_empty() throws Exception {
        mockMvc.perform(get("/cashier/record/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("2. 保存收银记录 - 成功")
    void testSaveCashierRecord_success() throws Exception {
        CashierRecord record = createCashierRecord(1001L, "ORD001", 1,
                new BigDecimal("50.00"), new BigDecimal("50.00"));

        mockMvc.perform(withCsrfToken(post("/cashier/record")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(record))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<CashierRecord> list = cashierService.getCashierRecordList(null, null, null, 1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getOrderNumber()).isEqualTo("ORD001");
        assertThat(list.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("3. 获取收银记录 - 按订单ID查询")
    void testGetCashierRecordByOrderId() throws Exception {
        CashierRecord record = createCashierRecord(2001L, "ORD002", 2,
                new BigDecimal("88.00"), new BigDecimal("88.00"));
        cashierService.saveCashierRecord(record);

        mockMvc.perform(get("/cashier/record/order/{orderId}", 2001L)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD002"));
    }

    @Test
    @DisplayName("4. 收银收款 - 订单不存在返回错误")
    void testCashPayment_orderNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(post("/cashier/cash-payment")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("orderId", "999999")
                        .param("orderNumber", "ORD_NOT_FOUND")
                        .param("amount", "120.00")
                        .param("actualAmount", "120.00")
                        .param("payType", "1")
                        .param("remark", "订单不存在测试")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 收银记录不应被创建
        List<CashierRecord> list = cashierService.getCashierRecordList(null, null, null, 1L);
        assertThat(list).isEmpty();
    }

    @Test
    @DisplayName("5. 删除收银记录 - 成功")
    void testDeleteCashierRecord_success() throws Exception {
        CashierRecord record = createCashierRecord(4001L, "ORD004", 3,
                new BigDecimal("200.00"), new BigDecimal("200.00"));
        cashierService.saveCashierRecord(record);

        List<CashierRecord> list = cashierService.getCashierRecordList(null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(withCsrfToken(delete("/cashier/record/{id}", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<CashierRecord> after = cashierService.getCashierRecordList(null, null, null, 1L);
        assertThat(after).isEmpty();
    }

    // ==================== 日结管理 ====================

    @Test
    @DisplayName("6. 获取日结列表 - 空列表")
    void testGetDailySettlementList_empty() throws Exception {
        mockMvc.perform(get("/cashier/settlement/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("7. 执行日结 - 成功")
    void testExecuteDailySettlement_success() throws Exception {
        CashierRecord record = createCashierRecord(5001L, "ORD005", 1,
                new BigDecimal("30.00"), new BigDecimal("30.00"));
        record.setCashierTime(LocalDateTime.of(2026, 8, 27, 10, 0, 0));
        cashierService.saveCashierRecord(record);

        LocalDate settlementDate = LocalDate.of(2026, 8, 27);

        mockMvc.perform(withCsrfToken(post("/cashier/settlement/execute")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("settlementDate", settlementDate.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        DailySettlement settlement = cashierService.getDailySettlementByDate(settlementDate, 1L);
        assertThat(settlement).isNotNull();
        assertThat(settlement.getStatus()).isEqualTo(1);
    }

    @Test
    @DisplayName("8. 获取日结 - 按日期查询")
    void testGetDailySettlementByDate() throws Exception {
        DailySettlement settlement = new DailySettlement();
        settlement.setSettlementDate(LocalDate.of(2026, 8, 20));
        settlement.setTotalRevenue(new BigDecimal("500.00"));
        settlement.setOrderCount(10);
        settlement.setStatus(1);
        settlement.setSettlementUserId(1L);
        settlement.setSettlementUserName("操作员");
        settlement.setTenantId(1L);
        settlementMapper.insert(settlement);

        mockMvc.perform(get("/cashier/settlement/date/{date}", "2026-08-20")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.settlementDate").value("2026-08-20"))
                .andExpect(jsonPath("$.data.totalRevenue").isNumber());
    }

    @Test
    @DisplayName("9. 取消日结 - 成功")
    void testCancelDailySettlement_success() throws Exception {
        DailySettlement settlement = new DailySettlement();
        settlement.setSettlementDate(LocalDate.of(2026, 8, 21));
        settlement.setTotalRevenue(new BigDecimal("300.00"));
        settlement.setOrderCount(5);
        settlement.setStatus(1);
        settlement.setSettlementUserId(1L);
        settlement.setSettlementUserName("操作员");
        settlement.setTenantId(1L);
        settlementMapper.insert(settlement);

        LocalDate settlementDate = LocalDate.of(2026, 8, 21);

        mockMvc.perform(withCsrfToken(post("/cashier/settlement/cancel")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("settlementDate", settlementDate.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        DailySettlement cancelled = cashierService.getDailySettlementByDate(settlementDate, 1L);
        assertThat(cancelled).isNotNull();
        assertThat(cancelled.getStatus()).isEqualTo(0);
    }

    @Test
    @DisplayName("10. 删除日结 - 成功")
    void testDeleteDailySettlement_success() throws Exception {
        DailySettlement settlement = new DailySettlement();
        settlement.setSettlementDate(LocalDate.of(2026, 8, 22));
        settlement.setTotalRevenue(new BigDecimal("100.00"));
        settlement.setOrderCount(2);
        settlement.setStatus(1);
        settlement.setSettlementUserId(1L);
        settlement.setSettlementUserName("操作员");
        settlement.setTenantId(1L);
        settlementMapper.insert(settlement);

        DailySettlement saved = cashierService.getDailySettlementByDate(LocalDate.of(2026, 8, 22), 1L);
        assertThat(saved).isNotNull();
        Long id = saved.getId();

        mockMvc.perform(withCsrfToken(delete("/cashier/settlement/{id}", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        DailySettlement deleted = cashierService.getDailySettlementByDate(LocalDate.of(2026, 8, 22), 1L);
        assertThat(deleted).isNull();
    }

    // ==================== 统计分析 ====================

    @Test
    @DisplayName("11. 获取收银统计")
    void testGetCashierStatistics() throws Exception {
        CashierRecord record = createCashierRecord(6001L, "ORD006", 2,
                new BigDecimal("60.00"), new BigDecimal("60.00"));
        record.setCashierTime(LocalDateTime.of(2026, 8, 27, 12, 0, 0));
        cashierService.saveCashierRecord(record);

        mockMvc.perform(get("/cashier/statistics")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", "2026-08-27 00:00:00")
                        .param("endDate", "2026-08-27 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("12. 获取支付方式统计")
    void testGetPaymentTypeStatistics() throws Exception {
        CashierRecord record1 = createCashierRecord(7001L, "ORD007", 1,
                new BigDecimal("40.00"), new BigDecimal("40.00"));
        record1.setCashierTime(LocalDateTime.of(2026, 8, 27, 10, 0, 0));
        cashierService.saveCashierRecord(record1);

        CashierRecord record2 = createCashierRecord(7002L, "ORD008", 2,
                new BigDecimal("70.00"), new BigDecimal("70.00"));
        record2.setCashierTime(LocalDateTime.of(2026, 8, 27, 14, 0, 0));
        cashierService.saveCashierRecord(record2);

        mockMvc.perform(get("/cashier/statistics/payment-type")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", "2026-08-27 00:00:00")
                        .param("endDate", "2026-08-27 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("13. 获取收银趋势")
    void testGetCashierTrend() throws Exception {
        CashierRecord record = createCashierRecord(8001L, "ORD009", 4,
                new BigDecimal("150.00"), new BigDecimal("150.00"));
        record.setCashierTime(LocalDateTime.of(2026, 8, 27, 9, 0, 0));
        cashierService.saveCashierRecord(record);

        mockMvc.perform(get("/cashier/trend")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", "2026-08-27 00:00:00")
                        .param("endDate", "2026-08-27 23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("14. 获取日结汇总")
    void testGetDailySettlementSummary() throws Exception {
        mockMvc.perform(get("/cashier/settlement/summary")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", "2026-08-20")
                        .param("endDate", "2026-08-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap());
    }

    // ==================== 辅助方法 ====================

    private CashierRecord createCashierRecord(Long orderId, String orderNumber, Integer payType,
                                              BigDecimal amount, BigDecimal actualAmount) {
        CashierRecord record = new CashierRecord();
        record.setOrderId(orderId);
        record.setOrderNumber(orderNumber);
        record.setPayType(payType);
        record.setAmount(amount);
        record.setActualAmount(actualAmount);
        record.setCashierTime(LocalDateTime.now());
        record.setTenantId(1L);
        return record;
    }

    /**
     * 为 MockMvc 请求添加有效的 CSRF Token。
     * 原理：CsrfFilter 读取 session["csrfToken"] 与 header "X-CSRF-Token" 做常量时间比较。
     * 用 CsrfTokenUtil.generateToken() 生成有效 token（含时间戳，未过期），
     * 通过 sessionAttr 注入 session，通过 header 注入请求头。
     */
    private MockHttpServletRequestBuilder withCsrfToken(MockHttpServletRequestBuilder request) {
        String token = CsrfTokenUtil.generateToken();
        return request
                .sessionAttr(CSRF_TOKEN_SESSION_KEY, token)
                .header(CSRF_HEADER_NAME, token);
    }

    private String toJson(Object obj) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }
}