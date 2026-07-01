package com.reggie.module.report;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.module.report.service.ReportService;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
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

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);

        Orders o1 = new Orders();
        o1.setId(1L);
        o1.setAmount(new BigDecimal("100.00"));
        o1.setStatus(4);
        o1.setPayMethod(1);
        o1.setOrderTime(LocalDateTime.of(2026, 7, 1, 8, 0));
        orderService.save(o1);

        Orders o2 = new Orders();
        o2.setId(2L);
        o2.setAmount(new BigDecimal("200.00"));
        o2.setStatus(4);
        o2.setPayMethod(2);
        o2.setOrderTime(LocalDateTime.of(2026, 7, 1, 12, 0));
        orderService.save(o2);

        Orders o3 = new Orders();
        o3.setId(3L);
        o3.setAmount(new BigDecimal("50.00"));
        o3.setStatus(5);
        o3.setPayMethod(1);
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
        Map<String, Object> report = reportService.getDailyReport("2026-07-01");

        assertEquals(3, report.get("totalOrders"));
        assertEquals(new BigDecimal("350.00"), report.get("totalAmount"));
        assertEquals(2, report.get("completedOrders"));
        assertEquals(1, report.get("cancelledOrders"));
        assertEquals(0, new BigDecimal("116.67").compareTo((BigDecimal) report.get("avgAmount")));
    }

    @Test
    void testDishRanking() {
        List<Map<String, Object>> ranking = reportService.getDishRanking("2026-07-01", "2026-07-01", 10);

        assertEquals(2, ranking.size());
        assertEquals("鱼香肉丝", ranking.get(0).get("name"));
        assertEquals(3, ranking.get(0).get("count"));
        assertEquals("宫保鸡丁", ranking.get(1).get("name"));
        assertEquals(1, ranking.get(1).get("count"));
    }

    @Test
    void testTimeSlotAnalysis() {
        List<Map<String, Object>> slots = reportService.getTimeSlotAnalysis("2026-07-01", "2026-07-01");

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
        Map<String, Object> payment = reportService.getPaymentAnalysis("2026-07-01", "2026-07-01");

        Map<String, Object> wechat = (Map<String, Object>) payment.get("wechat");
        assertEquals(2, wechat.get("count"));
        assertEquals(new BigDecimal("150.00"), wechat.get("amount"));

        Map<String, Object> alipay = (Map<String, Object>) payment.get("alipay");
        assertEquals(1, alipay.get("count"));
        assertEquals(new BigDecimal("200.00"), alipay.get("amount"));

        Map<String, Object> balance = (Map<String, Object>) payment.get("balance");
        assertEquals(0, balance.get("count"));
    }

    @Test
    void testExportReport() {
        byte[] data = reportService.exportDailyReport("2026-07-01", "2026-07-01");
        String csv = new String(data, StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("日期,订单数,总金额,已完成,已取消"));
        assertTrue(csv.contains("2026-07-01,3,350.00,2,1"));
    }

    @Test
    void testControllerDaily() throws Exception {
        mockMvc.perform(get("/api/report/daily")
                        .param("date", "2026-07-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.totalOrders").value(3))
                .andExpect(jsonPath("$.data.totalAmount").value(350.00))
                .andExpect(jsonPath("$.data.completedOrders").value(2))
                .andExpect(jsonPath("$.data.cancelledOrders").value(1));
    }
}
