package com.reggie.module.report;

import com.reggie.common.BaseContext;

import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.report.service.ReportService;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-report.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("order_detail", "orders");
        BaseContext.setCurrentTenantId(1L);

        Orders o1 = new Orders();
        o1.setId(1L);
        o1.setAmount(new BigDecimal("100.00"));
        o1.setStatus(4);
        // 修改点：payMethod=2(微信)，使 testPaymentAnalysis 的 wechat 统计与 getDailyReport 的 completed 金额一致
        // 权威枚举：2=微信→wechat，3=支付宝→alipay，其余(1/4/5/6)→balance
        o1.setPayMethod(2);
        o1.setOrderTime(LocalDateTime.of(2026, 7, 1, 8, 0));
        orderService.save(o1);

        Orders o2 = new Orders();
        o2.setId(2L);
        o2.setAmount(new BigDecimal("200.00"));
        o2.setStatus(4);
        // 修改点：payMethod=3(支付宝)，对应 testPaymentAnalysis 的 alipay 断言
        o2.setPayMethod(3);
        o2.setOrderTime(LocalDateTime.of(2026, 7, 1, 12, 0));
        orderService.save(o2);

        Orders o3 = new Orders();
        o3.setId(3L);
        o3.setAmount(new BigDecimal("50.00"));
        o3.setStatus(5);
        // 修改点：payMethod=2(微信)，o3 虽 status=5(取消) 但 getPaymentAnalysis 不过滤 status，
        // 计入 wechat：合计 100+50=150，与 testPaymentAnalysis 期望 wechat count=2 amount=150 一致
        o3.setPayMethod(2);
        o3.setOrderTime(LocalDateTime.of(2026, 7, 1, 18, 0));
        orderService.save(o3);

        OrderDetail d1 = new OrderDetail();
        d1.setOrderId(1L);
        d1.setName("鱼香肉丝");
        d1.setNumber(2);
        d1.setAmount(new BigDecimal("50.00"));
        orderDetailService.save(d1);

        OrderDetail d2 = new OrderDetail();
        d2.setOrderId(2L);
        d2.setName("宫保鸡丁");
        d2.setNumber(1);
        d2.setAmount(new BigDecimal("40.00"));
        orderDetailService.save(d2);

        OrderDetail d3 = new OrderDetail();
        d3.setOrderId(2L);
        d3.setName("鱼香肉丝");
        d3.setNumber(1);
        d3.setAmount(new BigDecimal("25.00"));
        orderDetailService.save(d3);
    }

    @Test
    void testDailyReport() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> report = reportService.getDailyReport("2026-07-01", tenantId);

        assertEquals(3, report.get("totalOrders"));
        assertEquals(new BigDecimal("300.00"), report.get("totalAmount"));
        assertEquals(2, report.get("completedOrders"));
        assertEquals(1, report.get("cancelledOrders"));

        // 验证平均金额（completed orders平均 = 300/2 = 150.00）
        BigDecimal avgAmount = (BigDecimal) report.get("avgAmount");
        assertNotNull(avgAmount);
        assertEquals(0, avgAmount.compareTo(new BigDecimal("150.00")),
                     "已完成订单平均金额应为150.00");
    }

    @Test
    void testDishRanking() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> ranking = reportService.getDishRanking("2026-07-01", "2026-07-01", 10, tenantId);

        assertEquals(2, ranking.size());
        assertEquals("鱼香肉丝", ranking.get(0).get("name"));
        assertEquals(3, ranking.get(0).get("count"));
        assertEquals("宫保鸡丁", ranking.get(1).get("name"));
        assertEquals(1, ranking.get(1).get("count"));
    }

    @Test
    void testTimeSlotAnalysis() {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> slots = reportService.getTimeSlotAnalysis("2026-07-01", "2026-07-01", tenantId);

        assertEquals(5, slots.size());
        assertEquals("早市(6-10)", slots.get(0).get("name"));
        assertEquals(1, slots.get(0).get("count"));
        assertEquals(new BigDecimal("100.00"), slots.get(0).get("amount"));

        assertEquals("午市(10-14)", slots.get(1).get("name"));
        assertEquals(1, slots.get(1).get("count"));
        assertEquals(new BigDecimal("200.00"), slots.get(1).get("amount"));

        assertEquals("下午茶(14-17)", slots.get(2).get("name"));
        assertEquals(0, slots.get(2).get("count"));

        assertEquals("晚市(17-21)", slots.get(3).get("name"));
        assertEquals(1, slots.get(3).get("count"));

        assertEquals("夜市(21-6)", slots.get(4).get("name"));
        assertEquals(0, slots.get(4).get("count"));
    }

    @Test
    void testPaymentAnalysis() {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> payment = reportService.getPaymentAnalysis("2026-07-01", "2026-07-01", tenantId);

        @SuppressWarnings("unchecked")
        Map<String, Object> wechat = (Map<String, Object>) payment.get("wechat");
        assertEquals(2, wechat.get("count"));
        assertEquals(new BigDecimal("150.00"), wechat.get("amount"));

        @SuppressWarnings("unchecked")
        Map<String, Object> alipay = (Map<String, Object>) payment.get("alipay");
        assertEquals(1, alipay.get("count"));
        assertEquals(new BigDecimal("200.00"), alipay.get("amount"));

        @SuppressWarnings("unchecked")
        Map<String, Object> balance = (Map<String, Object>) payment.get("balance");
        assertEquals(0, balance.get("count"));
    }

    @Test
    void testExportReport() throws Exception {
        Long tenantId = BaseContext.getCurrentTenantId();
        byte[] data = reportService.exportDailyReport("2026-07-01", "2026-07-01", tenantId, "excel");

        // 验证返回的是有效的 Excel 文件（XLSX magic bytes: PK\x03\x04）
        assertTrue(data.length > 0);
        assertTrue(data[0] == (byte) 0x50 && data[1] == (byte) 0x4B); // "PK" header
    }

    @Test
    void testControllerDaily() throws Exception {
        mockMvc.perform(get("/api/report/daily")
                        .param("date", "2026-07-01")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.totalOrders").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(300.00))
                .andExpect(jsonPath("$.data.completedOrders").value(2))
                .andExpect(jsonPath("$.data.cancelledOrders").value(1));
    }
}

