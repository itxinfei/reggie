package com.reggie.module.dining.controller;

import com.reggie.common.BaseContext;
import com.reggie.enums.DiningTableStatus;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.service.QueueService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.service.OrderService;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// 组合 payment-controller schema：开台联动会写 orders 表（IF NOT EXISTS，与 dining 表无冲突）
@Sql(scripts = {"classpath:schema-dining.sql", "classpath:schema-payment-controller.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueService queueService;

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);

        QueueRecord record = new QueueRecord();
        record.setId(1L);
        record.setTenantId(1L);
        record.setQueueNo("A001");
        record.setPhone("13800138000");
        record.setSeatCount(2);
        record.setStatus("WAITING");
        record.setCreatedTime(LocalDateTime.now());
        queueService.save(record);
    }

    @Test
    void testPage() throws Exception {
        mockMvc.perform(get("/api/dining/queue/page")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].queueNo").value("A001"));
    }

    @Test
    void testTakeNumber() throws Exception {
        mockMvc.perform(post("/api/dining/queue/take")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatCount\":4,\"phone\":\"13900139000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isNotEmpty())
                .andExpect(jsonPath("$.data.seatCount").value(4))
                .andExpect(jsonPath("$.data.queueNo").isNotEmpty());
    }

    @Test
    void testCallNext() throws Exception {
        mockMvc.perform(put("/api/dining/queue/call")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testCallNextWithEmptyQueue() throws Exception {
        // 先取消所有等待中的顾客
        mockMvc.perform(put("/api/dining/queue/cancel/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/dining/queue/call")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testCancel() throws Exception {
        mockMvc.perform(put("/api/dining/queue/cancel/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("取消排队成功"));
    }

    @Test
    void testCancelNonExistent() throws Exception {
        mockMvc.perform(put("/api/dining/queue/cancel/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testSeatCustomerOpensTable() throws Exception {
        // 准备 FREE 桌台
        DiningTable table = new DiningTable();
        table.setId(10L);
        table.setTenantId(1L);
        table.setName("测试桌10");
        table.setSeatCount(4);
        table.setStatus(DiningTableStatus.FREE.getValue());
        diningTableService.save(table);

        // 排队记录置为 CALLED（入座前置状态）
        QueueRecord called = queueService.getById(1L);
        called.setStatus("CALLED");
        queueService.updateById(called);

        // 安排入座并指定桌台 → 应联动开台
        mockMvc.perform(put("/api/dining/queue/seat")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"queueId\":1,\"tableId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 桌台已占用并绑定订单
        DiningTable updated = diningTableService.getById(10L);
        org.junit.jupiter.api.Assertions.assertEquals(
                DiningTableStatus.OCCUPIED.getValue(), updated.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(updated.getCurrentOrderId());
        // 占位订单：待付款、EAT_IN、绑定桌台、金额0
        Orders order = orderService.getById(updated.getCurrentOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(Orders.STATUS_PENDING_PAY, order.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("EAT_IN", order.getSource());
        org.junit.jupiter.api.Assertions.assertEquals(10L, order.getTableId().longValue());
        org.junit.jupiter.api.Assertions.assertEquals(
                0, order.getAmount().compareTo(java.math.BigDecimal.ZERO));
        // 排队记录状态为 SEATED
        org.junit.jupiter.api.Assertions.assertEquals("SEATED", queueService.getById(1L).getStatus());
    }
}
