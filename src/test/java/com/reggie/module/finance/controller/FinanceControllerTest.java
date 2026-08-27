package com.reggie.module.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CsrfTokenUtil;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.finance.mapper.ProfitAnalysisMapper;
import com.reggie.module.finance.mapper.ReconciliationStatementMapper;
import com.reggie.module.finance.mapper.WithdrawalApplicationMapper;
import com.reggie.module.finance.model.ProfitAnalysis;
import com.reggie.module.finance.model.ReconciliationStatement;
import com.reggie.module.finance.model.WithdrawalApplication;
import com.reggie.module.finance.service.FinanceService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * FinanceController 测试 — 财务管理
 *
 * 测试策略：
 * - 使用真实 MySQL 数据库（application-test.yml + jdbc:mysql://localhost:3306/reggie）
 * - schema-finance.sql 通过 @Sql 在每个测试方法前执行建表
 * - @Transactional 每个测试方法回滚，天然隔离
 * - 仅用 sessionAttr 注入：employee=1L、tenantId=1L 触发 LoginCheckFilter 设置 BaseContext
 * - 写操作（POST/PUT/DELETE）通过 withCsrfToken 注入有效的 CSRF token
 * - 提现状态机：PENDING(0) → APPROVED(1) → PAID(2)；PENDING(0) 可 review/cancel
 *
 * @author reggie
 * @since 2026-08-28
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:schema-finance.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class FinanceControllerTest {

    private static final String CSRF_TOKEN_SESSION_KEY = "csrfToken";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private WithdrawalApplicationMapper withdrawalMapper;

    @Autowired
    private ReconciliationStatementMapper reconciliationMapper;

    @Autowired
    private ProfitAnalysisMapper profitAnalysisMapper;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("withdrawal_application", "reconciliation_statement", "profit_analysis");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    // ==================== 提现管理 ====================

    @Test
    @DisplayName("1. 获取提现列表 - 空列表")
    void testGetWithdrawalList_empty() throws Exception {
        mockMvc.perform(get("/finance/withdrawal/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("2. 创建提现申请 - 成功")
    void testCreateWithdrawal_success() throws Exception {
        WithdrawalApplication application = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("500.00"), 1, "6222021234567890", "张三");

        mockMvc.perform(withCsrfToken(post("/finance/withdrawal")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(application))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<WithdrawalApplication> list = financeService.getWithdrawalList(null, null, null, 1L);
        assertThat(list).hasSize(1);
        WithdrawalApplication saved = list.get(0);
        assertThat(saved.getApplicationNo()).startsWith("WD");
        assertThat(saved.getStatus()).isEqualTo(WithdrawalApplication.STATUS_PENDING);
        assertThat(saved.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(saved.getTenantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("3. 审核提现申请 - 通过")
    void testReviewWithdrawal_approve() throws Exception {
        WithdrawalApplication application = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("500.00"), 1, "6222021234567890", "张三");
        financeService.createWithdrawal(application);

        List<WithdrawalApplication> list = financeService.getWithdrawalList(null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(withCsrfToken(post("/finance/withdrawal/{id}/review", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("status", String.valueOf(WithdrawalApplication.STATUS_APPROVED))
                        .param("remark", "审核通过")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        WithdrawalApplication reviewed = financeService.getWithdrawalById(id);
        assertThat(reviewed).isNotNull();
        assertThat(reviewed.getStatus()).isEqualTo(WithdrawalApplication.STATUS_APPROVED);
        assertThat(reviewed.getReviewRemark()).isEqualTo("审核通过");
    }

    @Test
    @DisplayName("4. 审核提现申请 - 拒绝")
    void testReviewWithdrawal_reject() throws Exception {
        WithdrawalApplication application = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("500.00"), 1, "6222021234567890", "张三");
        financeService.createWithdrawal(application);

        List<WithdrawalApplication> list = financeService.getWithdrawalList(null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(withCsrfToken(post("/finance/withdrawal/{id}/review", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("status", String.valueOf(WithdrawalApplication.STATUS_REJECTED))
                        .param("remark", "资料不全")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        WithdrawalApplication reviewed = financeService.getWithdrawalById(id);
        assertThat(reviewed).isNotNull();
        assertThat(reviewed.getStatus()).isEqualTo(WithdrawalApplication.STATUS_REJECTED);
    }

    @Test
    @DisplayName("5. 付款提现 - 审批后付款")
    void testProcessWithdrawalPayment() throws Exception {
        WithdrawalApplication application = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("300.00"), 2, "alipay_account", "张三");
        financeService.createWithdrawal(application);

        List<WithdrawalApplication> list = financeService.getWithdrawalList(null, null, null, 1L);
        Long id = list.get(0).getId();

        // 先审批通过
        financeService.reviewWithdrawal(id, WithdrawalApplication.STATUS_APPROVED, 1L, "Admin", "通过");

        // 再付款
        mockMvc.perform(withCsrfToken(post("/finance/withdrawal/{id}/payment", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("paymentNo", "PAY202608280001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        WithdrawalApplication paid = financeService.getWithdrawalById(id);
        assertThat(paid).isNotNull();
        assertThat(paid.getStatus()).isEqualTo(WithdrawalApplication.STATUS_PAID);
        assertThat(paid.getPaymentNo()).isEqualTo("PAY202608280001");
        assertThat(paid.getPaymentTime()).isNotNull();
    }

    @Test
    @DisplayName("6. 取消提现申请 - PENDING 状态可取消")
    void testCancelWithdrawal() throws Exception {
        WithdrawalApplication application = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("200.00"), 3, "wechat_account", "张三");
        financeService.createWithdrawal(application);

        List<WithdrawalApplication> list = financeService.getWithdrawalList(null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(withCsrfToken(post("/finance/withdrawal/{id}/cancel", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        WithdrawalApplication cancelled = financeService.getWithdrawalById(id);
        assertThat(cancelled).isNotNull();
        assertThat(cancelled.getStatus()).isEqualTo(WithdrawalApplication.STATUS_CANCELLED);
    }

    @Test
    @DisplayName("7. 删除提现申请 - 成功")
    void testDeleteWithdrawal_success() throws Exception {
        WithdrawalApplication application = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("100.00"), 1, "6222021234567890", "张三");
        financeService.createWithdrawal(application);

        List<WithdrawalApplication> list = financeService.getWithdrawalList(null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(withCsrfToken(delete("/finance/withdrawal/{id}", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        WithdrawalApplication deleted = financeService.getWithdrawalById(id);
        assertThat(deleted).isNull();
    }

    @Test
    @DisplayName("8. 获取提现详情 - 按ID查询")
    void testGetWithdrawalById() throws Exception {
        WithdrawalApplication application = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("800.00"), 1, "6222021234567890", "张三");
        financeService.createWithdrawal(application);

        List<WithdrawalApplication> list = financeService.getWithdrawalList(null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(get("/finance/withdrawal/{id}", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.applicationNo").isNotEmpty())
                .andExpect(jsonPath("$.data.amount").isNumber());
    }

    @Test
    @DisplayName("9. 获取提现列表 - 按状态筛选")
    void testGetWithdrawalList_byStatus() throws Exception {
        WithdrawalApplication app1 = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("100.00"), 1, "acct1", "张三");
        financeService.createWithdrawal(app1);

        WithdrawalApplication app2 = createWithdrawalApplication(1002L, "李四",
                new BigDecimal("200.00"), 2, "acct2", "李四");
        financeService.createWithdrawal(app2);

        mockMvc.perform(get("/finance/withdrawal/list?status=0")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ==================== 对账管理 ====================

    @Test
    @DisplayName("10. 获取对账列表 - 空列表")
    void testGetReconciliationList_empty() throws Exception {
        mockMvc.perform(get("/finance/reconciliation/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("11. 生成对账单 - 成功")
    void testGenerateReconciliation_success() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 27);

        mockMvc.perform(withCsrfToken(post("/finance/reconciliation/generate")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("date", date.toString())
                        .param("platform", "all")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.statementNo").isNotEmpty())
                .andExpect(jsonPath("$.data.statementDate").value(date.toString()));

        List<ReconciliationStatement> list = financeService.getReconciliationList(
                null, null, null, 1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPlatform()).isEqualTo("all");
        assertThat(list.get(0).getStatus()).isEqualTo(ReconciliationStatement.STATUS_UNRECONCILED);
    }

    @Test
    @DisplayName("12. 确认对账 - 系统金额等于平台金额时状态为已对账")
    void testConfirmReconciliation_reconciled() throws Exception {
        ReconciliationStatement statement = new ReconciliationStatement();
        statement.setStatementNo("RC202608270001");
        statement.setStatementDate(LocalDate.of(2026, 8, 27));
        statement.setPlatform("all");
        statement.setSystemAmount(new BigDecimal("1000.00"));
        statement.setPlatformAmount(new BigDecimal("1000.00"));
        statement.setDifferenceAmount(BigDecimal.ZERO);
        statement.setOrderCount(10);
        statement.setStatus(ReconciliationStatement.STATUS_UNRECONCILED);
        statement.setTenantId(1L);
        reconciliationMapper.insert(statement);

        List<ReconciliationStatement> list = financeService.getReconciliationList(
                null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(withCsrfToken(post("/finance/reconciliation/{id}/confirm", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        ReconciliationStatement confirmed = financeService.getReconciliationById(id);
        assertThat(confirmed).isNotNull();
        assertThat(confirmed.getStatus()).isEqualTo(ReconciliationStatement.STATUS_RECONCILED);
        assertThat(confirmed.getDifferenceAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("13. 确认对账 - 系统金额与平台金额不等时状态为有差异")
    void testConfirmReconciliation_discrepancy() throws Exception {
        ReconciliationStatement statement = new ReconciliationStatement();
        statement.setStatementNo("RC202608270002");
        statement.setStatementDate(LocalDate.of(2026, 8, 27));
        statement.setPlatform("wechat");
        statement.setSystemAmount(new BigDecimal("1000.00"));
        statement.setPlatformAmount(new BigDecimal("950.00"));
        statement.setDifferenceAmount(BigDecimal.ZERO);
        statement.setOrderCount(8);
        statement.setStatus(ReconciliationStatement.STATUS_UNRECONCILED);
        statement.setTenantId(1L);
        reconciliationMapper.insert(statement);

        List<ReconciliationStatement> list = financeService.getReconciliationList(
                null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(withCsrfToken(post("/finance/reconciliation/{id}/confirm", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        ReconciliationStatement confirmed = financeService.getReconciliationById(id);
        assertThat(confirmed).isNotNull();
        assertThat(confirmed.getStatus()).isEqualTo(ReconciliationStatement.STATUS_DISCREPANCY);
        assertThat(confirmed.getDifferenceAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("14. 删除对账单 - 成功")
    void testDeleteReconciliation_success() throws Exception {
        ReconciliationStatement statement = new ReconciliationStatement();
        statement.setStatementNo("RC202608270003");
        statement.setStatementDate(LocalDate.of(2026, 8, 27));
        statement.setPlatform("alipay");
        statement.setSystemAmount(new BigDecimal("500.00"));
        statement.setPlatformAmount(new BigDecimal("500.00"));
        statement.setDifferenceAmount(BigDecimal.ZERO);
        statement.setOrderCount(5);
        statement.setStatus(ReconciliationStatement.STATUS_UNRECONCILED);
        statement.setTenantId(1L);
        reconciliationMapper.insert(statement);

        List<ReconciliationStatement> list = financeService.getReconciliationList(
                null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(withCsrfToken(delete("/finance/reconciliation/{id}", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        ReconciliationStatement deleted = financeService.getReconciliationById(id);
        assertThat(deleted).isNull();
    }

    @Test
    @DisplayName("15. 获取对账详情 - 按ID查询")
    void testGetReconciliationById() throws Exception {
        ReconciliationStatement statement = new ReconciliationStatement();
        statement.setStatementNo("RC202608270004");
        statement.setStatementDate(LocalDate.of(2026, 8, 20));
        statement.setPlatform("all");
        statement.setSystemAmount(new BigDecimal("2000.00"));
        statement.setPlatformAmount(new BigDecimal("2000.00"));
        statement.setDifferenceAmount(BigDecimal.ZERO);
        statement.setOrderCount(20);
        statement.setStatus(ReconciliationStatement.STATUS_UNRECONCILED);
        statement.setTenantId(1L);
        reconciliationMapper.insert(statement);

        List<ReconciliationStatement> list = financeService.getReconciliationList(
                null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(get("/finance/reconciliation/{id}", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.statementDate").value("2026-08-20"))
                .andExpect(jsonPath("$.data.systemAmount").isNumber());
    }

    // ==================== 利润分析 ====================

    @Test
    @DisplayName("16. 获取利润分析列表 - 空列表")
    void testGetProfitAnalysisList_empty() throws Exception {
        mockMvc.perform(get("/finance/profit/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("17. 生成利润分析 - 成功")
    void testGenerateProfitAnalysis_success() throws Exception {
        LocalDate date = LocalDate.of(2026, 8, 27);

        mockMvc.perform(withCsrfToken(post("/finance/profit/generate")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("date", date.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.analysisDate").value(date.toString()))
                .andExpect(jsonPath("$.data.totalRevenue").isNumber());

        ProfitAnalysis analysis = financeService.getProfitAnalysisByDate(date, 1L);
        assertThat(analysis).isNotNull();
        assertThat(analysis.getAnalysisDate()).isEqualTo(date);
        assertThat(analysis.getTenantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("18. 获取利润分析 - 按日期查询")
    void testGetProfitAnalysisByDate() throws Exception {
        ProfitAnalysis analysis = new ProfitAnalysis();
        analysis.setAnalysisDate(LocalDate.of(2026, 8, 20));
        analysis.setTotalRevenue(new BigDecimal("5000.00"));
        analysis.setTotalCost(new BigDecimal("3000.00"));
        analysis.setGrossProfit(new BigDecimal("2000.00"));
        analysis.setOrderCount(50);
        analysis.setCustomerCount(30);
        analysis.setTenantId(1L);
        profitAnalysisMapper.insert(analysis);

        mockMvc.perform(get("/finance/profit/date/{date}", "2026-08-20")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.analysisDate").value("2026-08-20"))
                .andExpect(jsonPath("$.data.totalRevenue").isNumber());
    }

    @Test
    @DisplayName("19. 获取利润趋势")
    void testGetProfitTrend() throws Exception {
        // 插入两天的利润数据
        insertProfitAnalysis(LocalDate.of(2026, 8, 25), new BigDecimal("1000.00"),
                new BigDecimal("600.00"), new BigDecimal("400.00"));
        insertProfitAnalysis(LocalDate.of(2026, 8, 26), new BigDecimal("1500.00"),
                new BigDecimal("900.00"), new BigDecimal("600.00"));

        mockMvc.perform(get("/finance/profit/trend")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", "2026-08-25")
                        .param("endDate", "2026-08-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.dates").isArray())
                .andExpect(jsonPath("$.data.revenues").isArray());
    }

    @Test
    @DisplayName("20. 获取利润结构")
    void testGetProfitStructure() throws Exception {
        insertProfitAnalysis(LocalDate.of(2026, 8, 27), new BigDecimal("3000.00"),
                new BigDecimal("1500.00"), new BigDecimal("1500.00"));

        mockMvc.perform(get("/finance/profit/structure")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", "2026-08-27")
                        .param("endDate", "2026-08-27"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap());
    }

    // ==================== 统计分析 ====================

    @Test
    @DisplayName("21. 获取财务统计")
    void testGetFinanceStatistics() throws Exception {
        WithdrawalApplication application = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("1000.00"), 1, "6222021234567890", "张三");
        financeService.createWithdrawal(application);

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        mockMvc.perform(get("/finance/statistics")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", today.atStartOfDay().format(fmt))
                        .param("endDate", today.atTime(23, 59, 59).format(fmt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("22. 获取提现统计")
    void testGetWithdrawalStatistics() throws Exception {
        WithdrawalApplication app1 = createWithdrawalApplication(1001L, "张三",
                new BigDecimal("500.00"), 1, "acct1", "张三");
        financeService.createWithdrawal(app1);

        WithdrawalApplication app2 = createWithdrawalApplication(1002L, "李四",
                new BigDecimal("300.00"), 2, "acct2", "李四");
        financeService.createWithdrawal(app2);

        mockMvc.perform(get("/finance/withdrawal/statistics")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap());
    }

    // ==================== 辅助方法 ====================

    private WithdrawalApplication createWithdrawalApplication(Long applicantId, String applicantName,
                                                              BigDecimal amount, Integer withdrawMethod,
                                                              String receiveAccount, String receiveName) {
        WithdrawalApplication application = new WithdrawalApplication();
        application.setApplicantId(applicantId);
        application.setApplicantName(applicantName);
        application.setAmount(amount);
        application.setWithdrawMethod(withdrawMethod);
        application.setReceiveAccount(receiveAccount);
        application.setReceiveName(receiveName);
        application.setTenantId(1L);
        return application;
    }

    private void insertProfitAnalysis(LocalDate date, BigDecimal totalRevenue,
                                       BigDecimal totalCost, BigDecimal grossProfit) {
        ProfitAnalysis analysis = new ProfitAnalysis();
        analysis.setAnalysisDate(date);
        analysis.setTotalRevenue(totalRevenue);
        analysis.setTotalCost(totalCost);
        analysis.setGrossProfit(grossProfit);
        analysis.setOrderCount(10);
        analysis.setCustomerCount(5);
        analysis.setTenantId(1L);
        profitAnalysisMapper.insert(analysis);
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