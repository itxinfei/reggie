package com.reggie.module.dining.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.dining.model.Reservation;
import com.reggie.module.dining.service.ReservationService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
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

/**
 * C 端顾客到店预订接口测试
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-dining.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class ReservationCustomerControllerTest {

    private static final Long USER_ID = 990001L;
    private static final Long TENANT_ID = 999L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(TENANT_ID);
        User user = new User();
        user.setId(USER_ID);
        user.setName("测试顾客");
        user.setPhone("13800138001");
        user.setStatus(1);
        user.setTenantId(TENANT_ID);
        userService.removeById(USER_ID);
        userService.save(user);
    }

    private String futureTime(int plusHours) {
        return LocalDateTime.now().plusHours(plusHours).format(FMT);
    }

    @Test
    void testCreateReservation() throws Exception {
        mockMvc.perform(post("/api/dining/reservation/customer")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"张先生\",\"reservedTime\":\"" + futureTime(24)
                        + "\",\"seatCount\":4,\"remark\":\"需要宝宝椅\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.phone").value("13800138001"))
                .andExpect(jsonPath("$.data.customerName").value("张先生"))
                .andExpect(jsonPath("$.data.seatCount").value(4))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void testCreateReservationMissingTime() throws Exception {
        mockMvc.perform(post("/api/dining/reservation/customer")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatCount\":2}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateReservationPastTime() throws Exception {
        mockMvc.perform(post("/api/dining/reservation/customer")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservedTime\":\"2020-01-01 12:00:00\",\"seatCount\":2}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testMyReservations() throws Exception {
        mockMvc.perform(post("/api/dining/reservation/customer")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservedTime\":\"" + futureTime(24) + "\",\"seatCount\":2}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dining/reservation/customer/my")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userId").value(USER_ID))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void testCancelMyReservation() throws Exception {
        String resp = mockMvc.perform(post("/api/dining/reservation/customer")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservedTime\":\"" + futureTime(24) + "\",\"seatCount\":2}"))
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(resp.replaceAll(".*\"id\":\"(\\d+)\".*", "$1"));

        mockMvc.perform(put("/api/dining/reservation/customer/" + id + "/cancel")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        mockMvc.perform(get("/api/dining/reservation/customer/my")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("CANCELLED"));
    }

    @Test
    void testCancelOthersReservationRejected() throws Exception {
        Reservation other = new Reservation();
        other.setTenantId(TENANT_ID);
        other.setCustomerName("别人");
        other.setPhone("13900139009");
        other.setUserId(990002L);
        other.setSeatCount(2);
        other.setReservedTime(LocalDateTime.now().plusHours(24));
        other.setStatus("PENDING");
        reservationService.save(other);

        mockMvc.perform(put("/api/dining/reservation/customer/" + other.getId() + "/cancel")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testGuestRejected() throws Exception {
        mockMvc.perform(post("/api/dining/reservation/customer")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reservedTime\":\"" + futureTime(24) + "\",\"seatCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
