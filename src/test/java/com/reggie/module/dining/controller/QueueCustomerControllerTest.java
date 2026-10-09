package com.reggie.module.dining.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.dining.model.QueueRecord;
import com.reggie.module.dining.service.QueueService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端顾客排队取号接口测试
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-dining.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class QueueCustomerControllerTest {

    private static final Long USER_ID = 990001L;
    private static final Long TENANT_ID = 999L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private QueueService queueService;

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

    @Test
    void testTakeNumber() throws Exception {
        mockMvc.perform(post("/api/dining/queue/customer/take")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.seatCount").value(2))
                .andExpect(jsonPath("$.data.phone").value("13800138001"))
                .andExpect(jsonPath("$.data.queueNo").isNotEmpty());
    }

    @Test
    void testTakeValidation() throws Exception {
        mockMvc.perform(post("/api/dining/queue/customer/take")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMyQueue() throws Exception {
        // 先取号
        mockMvc.perform(post("/api/dining/queue/customer/take")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatCount\":3}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/dining/queue/customer/my")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.record.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.record.seatCount").value(3))
                .andExpect(jsonPath("$.data.waitingAhead").value(0));
    }

    @Test
    void testMyQueueEmpty() throws Exception {
        mockMvc.perform(get("/api/dining/queue/customer/my")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testCancelMyQueue() throws Exception {
        // 取号 → 拿到记录 ID
        String resp = mockMvc.perform(post("/api/dining/queue/customer/take")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatCount\":2}"))
                .andReturn().getResponse().getContentAsString();
        Long id = Long.valueOf(resp.replaceAll(".*\"id\":\"(\\d+)\".*", "$1"));

        mockMvc.perform(put("/api/dining/queue/customer/" + id + "/cancel")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 已取消，my 无有效记录
        mockMvc.perform(get("/api/dining/queue/customer/my")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testCancelOthersQueueRejected() throws Exception {
        // 别人的排队记录
        QueueRecord other = new QueueRecord();
        other.setTenantId(TENANT_ID);
        other.setQueueNo("B0001");
        other.setPhone("13900139009");
        other.setUserId(990002L);
        other.setSeatCount(2);
        other.setStatus("WAITING");
        other.setCreatedTime(LocalDateTime.now());
        queueService.save(other);

        mockMvc.perform(put("/api/dining/queue/customer/" + other.getId() + "/cancel")
                .sessionAttr("user", USER_ID)
                .sessionAttr("tenantId", TENANT_ID))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testGuestRejected() throws Exception {
        mockMvc.perform(post("/api/dining/queue/customer/take")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"seatCount\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
