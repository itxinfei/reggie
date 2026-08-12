package com.reggie.controller;

import com.reggie.common.BaseContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据概览仪表盘测试
 * 注意：Dashboard接口依赖较多业务数据，测试中仅验证接口可正常调用
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetOverview() throws Exception {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        mockMvc.perform(get("/api/dashboard/overview"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetTrend() throws Exception {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        mockMvc.perform(get("/api/dashboard/trend"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetOrderStatus() throws Exception {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        mockMvc.perform(get("/api/dashboard/order-status"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetHotDishes() throws Exception {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        mockMvc.perform(get("/api/dashboard/hot-dishes")
                .param("limit", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetHotDishesWithCustomLimit() throws Exception {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        mockMvc.perform(get("/api/dashboard/hot-dishes")
                .param("limit", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSystemHealth() throws Exception {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        mockMvc.perform(get("/api/dashboard/health"))
                .andExpect(status().isOk())
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllData() throws Exception {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        mockMvc.perform(get("/api/dashboard/all")
                .param("hotDishLimit", "10"))
                .andExpect(status().isOk());
    }
}

