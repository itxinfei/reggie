package com.reggie.module.delivery;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.delivery.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:schema-delivery.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DeliveryServiceTest {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testAcceptOrder() {
        boolean result = deliveryService.acceptOrder("MEITUAN", "MT123456");
        assertTrue(result);
    }

    @Test
    void testSyncMenu() {
        List<Map<String, Object>> dishes = new ArrayList<>();
        Map<String, Object> dish = new HashMap<>();
        dish.put("id", 1L);
        dish.put("name", "鱼香肉丝");
        dish.put("price", 28.00);
        dishes.add(dish);
        boolean result = deliveryService.syncMenu("ELEME", dishes);
        assertTrue(result);
    }

    @Test
    void testSyncStock() {
        Map<Long, Integer> stock = new HashMap<>();
        stock.put(1L, 100);
        stock.put(2L, 50);
        boolean result = deliveryService.syncStock("MEITUAN", stock);
        assertTrue(result);
    }

    @Test
    void testCallback() {
        Map<String, String> params = new HashMap<>();
        params.put("orderId", "MT123456");
        params.put("status", "confirmed");
        String result = deliveryService.handleCallback("MEITUAN", params);
        assertEquals("success", result);
    }

    @Test
    void testControllerAcceptOrder() throws Exception {
        String json = "{\"platform\":\"MEITUAN\",\"platformOrderId\":\"MT123456\"}";
        mockMvc.perform(post("/api/delivery/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
