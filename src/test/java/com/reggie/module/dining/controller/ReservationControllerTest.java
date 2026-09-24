package com.reggie.module.dining.controller;

import com.reggie.common.BaseContext;
import com.reggie.enums.DiningTableStatus;
import com.reggie.enums.ReservationStatus;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.dining.service.ReservationService;
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
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// 组合 payment-controller schema：到店联动开台会写 orders 表
@Sql(scripts = {"classpath:schema-dining.sql", "classpath:schema-payment-controller.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DiningTableService diningTableService;

    @Autowired
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);

        Reservation reservation = new Reservation();
        reservation.setId(1L);
        reservation.setTenantId(1L);
        reservation.setCustomerName("张三");
        reservation.setPhone("13800138000");
        reservation.setReservedTime(LocalDateTime.now().plusDays(1));
        reservation.setSeatCount(4);
        reservation.setStatus("PENDING");
        reservation.setRemark("靠窗位置");
        reservation.setCreatedTime(LocalDateTime.now());
        reservationService.save(reservation);
    }

    @Test
    void testPage() throws Exception {
        mockMvc.perform(get("/api/dining/reservation/page")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].customerName").value("张三"));
    }

    @Test
    void testCreate() throws Exception {
        LocalDateTime reservedTime = LocalDateTime.now().plusDays(2);
        String reservedTimeStr = reservedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        mockMvc.perform(post("/api/dining/reservation")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"李四\",\"phone\":\"13900139000\",\"reservedTime\":\"" + reservedTimeStr + "\",\"seatCount\":2,\"remark\":\"无烟区\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.customerName").value("李四"));
    }

    @Test
    void testConfirm() throws Exception {
        mockMvc.perform(put("/api/dining/reservation/confirm/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("确认预订成功"));
    }

    @Test
    void testConfirmNonExistent() throws Exception {
        mockMvc.perform(put("/api/dining/reservation/confirm/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testCancel() throws Exception {
        mockMvc.perform(put("/api/dining/reservation/cancel/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("取消预订成功"));
    }

    @Test
    void testArrive() throws Exception {
        // 到店仅对已确认(CONFIRMED)预订开放，先确认
        mockMvc.perform(put("/api/dining/reservation/confirm/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(put("/api/dining/reservation/arrive/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("到店成功"));

        // 重复到店须拦截，避免把已开桌台的首单踢成孤儿单
        mockMvc.perform(put("/api/dining/reservation/arrive/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testArriveNonExistent() throws Exception {
        mockMvc.perform(put("/api/dining/reservation/arrive/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testArriveOpensTable() throws Exception {
        // 预订确认时桌台已置 RESERVED
        DiningTable table = new DiningTable();
        table.setId(20L);
        table.setTenantId(1L);
        table.setName("测试桌20");
        table.setSeatCount(4);
        table.setStatus(DiningTableStatus.RESERVED.getValue());
        diningTableService.save(table);

        // 第二条预订（setUp 的 id=1 未绑桌台），绑定桌台
        Reservation r = new Reservation();
        r.setId(2L);
        r.setTenantId(1L);
        r.setCustomerName("李四");
        r.setPhone("13900139000");
        r.setReservedTime(LocalDateTime.now().plusHours(2));
        r.setSeatCount(4);
        r.setStatus(ReservationStatus.CONFIRMED.getValue());
        r.setTableId(20L);
        reservationService.save(r);

        // 到店 → 先释放 RESERVED→FREE 再联动开台
        mockMvc.perform(put("/api/dining/reservation/arrive/2")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 桌台已占用并绑定订单
        DiningTable updated = diningTableService.getById(20L);
        org.junit.jupiter.api.Assertions.assertEquals(
                DiningTableStatus.OCCUPIED.getValue(), updated.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(updated.getCurrentOrderId());
        // 占位订单：待付款、EAT_IN
        Orders order = orderService.getById(updated.getCurrentOrderId());
        org.junit.jupiter.api.Assertions.assertEquals(Orders.STATUS_PENDING_PAY, order.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("EAT_IN", order.getSource());
        // 预订状态 ARRIVED
        org.junit.jupiter.api.Assertions.assertEquals(
                ReservationStatus.ARRIVED.getValue(), reservationService.getById(2L).getStatus());
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/api/dining/reservation/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.customerName").value("张三"))
                .andExpect(jsonPath("$.data.phone").value("13800138000"));
    }

    @Test
    void testGetByIdNonExistent() throws Exception {
        mockMvc.perform(get("/api/dining/reservation/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testUpdate() throws Exception {
        LocalDateTime reservedTime = LocalDateTime.now().plusDays(3);
        String reservedTimeStr = reservedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        mockMvc.perform(put("/api/dining/reservation")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"customerName\":\"张三改\",\"phone\":\"13800138001\",\"reservedTime\":\""
                        + reservedTimeStr + "\",\"seatCount\":6,\"remark\":\"改为大桌\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.customerName").value("张三改"))
                .andExpect(jsonPath("$.data.seatCount").value(6));
    }

    @Test
    void testUpdateMissingId() throws Exception {
        mockMvc.perform(put("/api/dining/reservation")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"张三\",\"phone\":\"13800138000\",\"reservedTime\":\"2026-12-01 18:00:00\",\"seatCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testDelete() throws Exception {
        // 先取消预订（只有已取消状态才允许删除）
        mockMvc.perform(put("/api/dining/reservation/cancel/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 删除已取消的预订
        mockMvc.perform(delete("/api/dining/reservation/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("删除成功"));
    }

    @Test
    void testDeleteNonCancelledFails() throws Exception {
        // PENDING 状态直接删除应失败
        mockMvc.perform(delete("/api/dining/reservation/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("只有已取消的预订才能删除，请先取消预订"));
    }
}
