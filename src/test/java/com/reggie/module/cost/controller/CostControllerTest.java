package com.reggie.module.cost.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CsrfTokenUtil;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.cost.model.CostRecord;
import com.reggie.module.cost.model.DishCost;
import com.reggie.module.cost.model.LaborCost;
import com.reggie.module.cost.model.OtherCost;
import com.reggie.module.cost.service.CostService;
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
 * CostController 测试 — 成本核算管理
 *
 * 测试策略：
 * - 使用真实 MySQL 数据库（application-test.yml + jdbc:mysql://localhost:3306/reggie）
 * - schema-cost.sql 通过 @Sql 在每个测试方法前执行建表
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
@Sql(scripts = "classpath:schema-cost.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class CostControllerTest {

    private static final String CSRF_TOKEN_SESSION_KEY = "csrfToken";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CostService costService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("dish_cost", "cost_record", "labor_cost", "other_cost");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    // ==================== 菜品成本管理 ====================

    @Test
    @DisplayName("1. 获取菜品成本列表 - 空列表")
    void testGetDishCostList_empty() throws Exception {
        mockMvc.perform(get("/cost/dish/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("2. 保存菜品成本 - 成功")
    void testSaveDishCost_success() throws Exception {
        DishCost dishCost = createDishCost(101L, "宫保鸡丁",
                new BigDecimal("8.50"), new BigDecimal("2.00"),
                new BigDecimal("1.50"), new BigDecimal("30.00"));

        mockMvc.perform(withCsrfToken(post("/cost/dish")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(dishCost))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<DishCost> list = costService.getDishCostList(1L);
        assertThat(list).hasSize(1);
        DishCost saved = list.get(0);
        assertThat(saved.getDishName()).isEqualTo("宫保鸡丁");
        assertThat(saved.getTotalCost()).isEqualByComparingTo(new BigDecimal("12.00"));
        assertThat(saved.getTenantId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("3. 更新菜品成本 - 成功")
    void testUpdateDishCost_success() throws Exception {
        DishCost dishCost = createDishCost(101L, "宫保鸡丁",
                new BigDecimal("8.50"), new BigDecimal("2.00"),
                new BigDecimal("1.50"), new BigDecimal("30.00"));
        costService.saveOrUpdateDishCost(dishCost);

        DishCost saved = costService.getDishCostByDishId(101L, 1L);
        assertThat(saved).isNotNull();
        Long id = saved.getId();

        saved.setSalePrice(new BigDecimal("35.00"));
        saved.setRemark("已更新");

        mockMvc.perform(withCsrfToken(put("/cost/dish")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(saved))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        DishCost updated = costService.getDishCostByDishId(101L, 1L);
        assertThat(updated).isNotNull();
        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getSalePrice()).isEqualByComparingTo(new BigDecimal("35.00"));
        assertThat(updated.getRemark()).isEqualTo("已更新");
    }

    @Test
    @DisplayName("4. 删除菜品成本 - 成功")
    void testDeleteDishCost_success() throws Exception {
        DishCost dishCost = createDishCost(101L, "宫保鸡丁",
                new BigDecimal("8.50"), new BigDecimal("2.00"),
                new BigDecimal("1.50"), new BigDecimal("30.00"));
        costService.saveOrUpdateDishCost(dishCost);
        DishCost saved = costService.getDishCostByDishId(101L, 1L);
        assertThat(saved).isNotNull();

        mockMvc.perform(withCsrfToken(delete("/cost/dish/{id}", saved.getId())
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        DishCost deleted = costService.getDishCostByDishId(101L, 1L);
        assertThat(deleted).isNull();
    }

    @Test
    @DisplayName("5. 批量保存菜品成本 - 成功")
    void testBatchSaveDishCost_success() throws Exception {
        DishCost cost1 = createDishCost(101L, "宫保鸡丁",
                new BigDecimal("8.50"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30.00"));
        DishCost cost2 = createDishCost(102L, "麻婆豆腐",
                new BigDecimal("5.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00"));

        String json = "[" + toJson(cost1) + "," + toJson(cost2) + "]";

        mockMvc.perform(withCsrfToken(post("/cost/dish/batch")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(json)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<DishCost> list = costService.getDishCostList(1L);
        assertThat(list).hasSize(2);
    }

    // ==================== 成本记录管理 ====================

    @Test
    @DisplayName("6. 保存成本记录 - 成功")
    void testSaveCostRecord_success() throws Exception {
        CostRecord record = new CostRecord();
        record.setCostType(1);
        record.setRefId(101L);
        record.setRefName("宫保鸡丁");
        record.setAmount(new BigDecimal("25.00"));
        record.setCostDate(LocalDateTime.now());
        record.setRemark("今日食材采购");

        mockMvc.perform(withCsrfToken(post("/cost/record")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(record))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<CostRecord> list = costService.getCostRecordList(null, null, null, 1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getCostType()).isEqualTo(1);
        assertThat(list.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("7. 获取成本记录列表 - 按类型筛选")
    void testGetCostRecordList_byType() throws Exception {
        CostRecord record1 = new CostRecord();
        record1.setCostType(1);
        record1.setRefName("食材1");
        record1.setAmount(new BigDecimal("10.00"));
        record1.setCostDate(LocalDateTime.now());
        costService.saveCostRecord(record1);

        CostRecord record2 = new CostRecord();
        record2.setCostType(2);
        record2.setRefName("人工1");
        record2.setAmount(new BigDecimal("1000.00"));
        record2.setCostDate(LocalDateTime.now());
        costService.saveCostRecord(record2);

        mockMvc.perform(get("/cost/record/list?costType=1")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].costType").value(1));
    }

    @Test
    @DisplayName("8. 删除成本记录 - 成功")
    void testDeleteCostRecord_success() throws Exception {
        CostRecord record = new CostRecord();
        record.setCostType(3);
        record.setRefName("水电费");
        record.setAmount(new BigDecimal("500.00"));
        record.setCostDate(LocalDateTime.now());
        costService.saveCostRecord(record);

        List<CostRecord> list = costService.getCostRecordList(null, null, null, 1L);
        Long id = list.get(0).getId();

        mockMvc.perform(withCsrfToken(delete("/cost/record/{id}", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<CostRecord> after = costService.getCostRecordList(null, null, null, 1L);
        assertThat(after).isEmpty();
    }

    // ==================== 人工成本管理 ====================

    @Test
    @DisplayName("9. 保存人工成本 - 成功")
    void testSaveLaborCost_success() throws Exception {
        LaborCost laborCost = new LaborCost();
        laborCost.setEmployeeId(1001L);
        laborCost.setEmployeeName("张师傅");
        laborCost.setSalary(new BigDecimal("5000.00"));
        laborCost.setSocialInsurance(new BigDecimal("500.00"));
        laborCost.setHousingFund(new BigDecimal("300.00"));
        laborCost.setOtherBenefits(new BigDecimal("200.00"));
        laborCost.setCostMonth(LocalDate.now());

        mockMvc.perform(withCsrfToken(post("/cost/labor")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(laborCost))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<LaborCost> list = costService.getLaborCostList(null, 1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTotalCost()).isEqualByComparingTo(new BigDecimal("6000.00"));
        assertThat(list.get(0).getEmployeeName()).isEqualTo("张师傅");
    }

    @Test
    @DisplayName("10. 批量保存人工成本 - 成功")
    void testBatchSaveLaborCost_success() throws Exception {
        LaborCost cost1 = new LaborCost();
        cost1.setEmployeeId(1001L);
        cost1.setEmployeeName("张师傅");
        cost1.setSalary(new BigDecimal("5000.00"));
        cost1.setCostMonth(LocalDate.now());

        LaborCost cost2 = new LaborCost();
        cost2.setEmployeeId(1002L);
        cost2.setEmployeeName("李师傅");
        cost2.setSalary(new BigDecimal("4500.00"));
        cost2.setCostMonth(LocalDate.now());

        String json = "[" + toJson(cost1) + "," + toJson(cost2) + "]";

        mockMvc.perform(withCsrfToken(post("/cost/labor/batch")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(json)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<LaborCost> list = costService.getLaborCostList(null, 1L);
        assertThat(list).hasSize(2);
    }

    // ==================== 其他成本管理 ====================

    @Test
    @DisplayName("11. 保存其他成本 - 成功")
    void testSaveOtherCost_success() throws Exception {
        OtherCost otherCost = new OtherCost();
        otherCost.setName("店面租金");
        otherCost.setCostType(1);
        otherCost.setAmount(new BigDecimal("8000.00"));
        otherCost.setCostDate(LocalDateTime.now());
        otherCost.setRemark("2026年8月租金");

        mockMvc.perform(withCsrfToken(post("/cost/other")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(otherCost))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        List<OtherCost> list = costService.getOtherCostList(null, null, null, 1L);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getName()).isEqualTo("店面租金");
        assertThat(list.get(0).getCostType()).isEqualTo(1);
    }

    // ==================== 统计分析 ====================

    @Test
    @DisplayName("12. 获取成本汇总统计")
    void testGetCostSummary() throws Exception {
        CostRecord record = new CostRecord();
        record.setCostType(1);
        record.setRefName("食材");
        record.setAmount(new BigDecimal("200.00"));
        record.setCostDate(LocalDateTime.now());
        costService.saveCostRecord(record);

        LaborCost labor = new LaborCost();
        labor.setEmployeeId(1001L);
        labor.setEmployeeName("张师傅");
        labor.setTotalCost(new BigDecimal("5000.00"));
        labor.setCostMonth(LocalDate.now());
        costService.saveOrUpdateLaborCost(labor);

        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/cost/summary?startDate=" + today + "&endDate=" + today)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap());
    }

    @Test
    @DisplayName("13. 获取菜品成本排行")
    void testGetDishCostRanking() throws Exception {
        costService.saveOrUpdateDishCost(createDishCost(101L, "宫保鸡丁",
                new BigDecimal("12.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30.00")));
        costService.saveOrUpdateDishCost(createDishCost(102L, "麻婆豆腐",
                new BigDecimal("8.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")));

        mockMvc.perform(get("/cost/dish/ranking?limit=5")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("14. 计算菜品毛利率")
    void testCalculateProfitRate() throws Exception {
        costService.saveOrUpdateDishCost(createDishCost(101L, "宫保鸡丁",
                new BigDecimal("12.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30.00")));

        mockMvc.perform(get("/cost/dish/profit-rate/{dishId}", 101L)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isNumber());
    }

    @Test
    @DisplayName("15. 获取成本预警列表")
    void testGetCostAlert() throws Exception {
        costService.saveOrUpdateDishCost(createDishCost(101L, "高价菜",
                new BigDecimal("20.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("30.00")));
        costService.saveOrUpdateDishCost(createDishCost(102L, "低价菜",
                new BigDecimal("8.00"), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("20.00")));

        mockMvc.perform(get("/cost/alert?threshold=30")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray());
    }

    // ==================== 辅助方法 ====================

    private DishCost createDishCost(Long dishId, String dishName,
                                     BigDecimal materialCost, BigDecimal laborCost,
                                     BigDecimal otherCost, BigDecimal salePrice) {
        DishCost cost = new DishCost();
        cost.setDishId(dishId);
        cost.setDishName(dishName);
        cost.setMaterialCost(materialCost);
        cost.setLaborCost(laborCost);
        cost.setOtherCost(otherCost);
        cost.setSalePrice(salePrice);
        cost.setTenantId(1L);
        return cost;
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