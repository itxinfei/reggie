package com.reggie.module.dining.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.service.ReservationService;
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
@Sql(scripts = "classpath:schema-dining.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ReservationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

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
        mockMvc.perform(put("/api/dining/reservation/arrive/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("到店成功"));
    }

    @Test
    void testArriveNonExistent() throws Exception {
        mockMvc.perform(put("/api/dining/reservation/arrive/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
