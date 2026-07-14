package com.reggie.module.delivery;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.delivery.model.DeliveryOrder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        Map<String, String> params = new HashMap<>();
        params.put("type", "new_order");
        params.put("platformOrderId", "MT123456");
        params.put("dishSummary", "鱼香肉丝x1");
        params.put("amount", "28.00");
        params.put("userName", "张三");
        params.put("phone", "13800138000");
        params.put("address", "北京市朝阳区xxx");
        deliveryService.handleCallback("MEITUAN", params);

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
        params.put("type", "new_order");
        params.put("platformOrderId", "MT_CB_001");
        params.put("dishSummary", "测试菜品");
        params.put("amount", "15.00");
        params.put("userName", "测试用户");
        params.put("phone", "13900139000");
        params.put("address", "测试地址");
        String result = deliveryService.handleCallback("MEITUAN", params);
        assertEquals("success", result);
    }

    @Test
    void testGetByPlatformOrderId() {
        Map<String, String> params = new HashMap<>();
        params.put("type", "new_order");
        params.put("platformOrderId", "MT_TRACK_002");
        params.put("dishSummary", "宫保鸡丁");
        params.put("amount", "32.00");
        params.put("userName", "李四");
        params.put("phone", "13700137000");
        params.put("address", "北京市海淀区xxx");
        deliveryService.handleCallback("MEITUAN", params);

        DeliveryOrder order = deliveryService.getByPlatformOrderId("MT_TRACK_002");
        assertNotNull(order);
        assertEquals("MEITUAN", order.getPlatform());
        assertEquals("MT_TRACK_002", order.getPlatformOrderId());
    }

    @Test
    void testControllerAcceptOrder() throws Exception {
        String createParams = "{\"type\":\"new_order\",\"platformOrderId\":\"MT123456\",\"dishSummary\":\"测试\",\"amount\":\"10.00\",\"userName\":\"王五\",\"phone\":\"13600136000\",\"address\":\"测试地址\"}";
        mockMvc.perform(post("/api/delivery/callback/MEITUAN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createParams)
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk());

        String json = "{\"platform\":\"MEITUAN\",\"platformOrderId\":\"MT123456\"}";
        mockMvc.perform(post("/api/delivery/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testTrackingByOrderId() throws Exception {
        String createParams = "{\"type\":\"new_order\",\"platformOrderId\":\"MT_TRACK_001\",\"dishSummary\":\"麻婆豆腐\",\"amount\":\"18.00\",\"userName\":\"赵六\",\"phone\":\"13500135000\",\"address\":\"北京市西城区xxx\"}";
        mockMvc.perform(post("/api/delivery/callback/MEITUAN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createParams)
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/delivery/tracking/MT_TRACK_001")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.platformOrderId").value("MT_TRACK_001"))
                .andExpect(jsonPath("$.data.platform").value("MEITUAN"));
    }
}
