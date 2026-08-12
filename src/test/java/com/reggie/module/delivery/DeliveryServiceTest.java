package com.reggie.module.delivery;

import com.reggie.common.BaseContext;
import com.reggie.module.delivery.model.DeliveryOrder;
import com.reggie.module.delivery.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
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

/**
 * 配送模块服务测试
 *
 * <p>测试说明：MockMvc 默认不经过 {@code @WebFilter} 注册的 LoginCheckFilter
 * （LoginCheckFilter 未标注 {@code @Component}，仅通过 {@code @ServletComponentScan}
 * 注册到 Servlet 容器，不进入 MockMvc 的 Filter 链）。
 * 因此当 Controller/Service 依赖 {@link BaseContext} 中的租户上下文时，
 * 测试需在 perform 前手动设置 BaseContext。</p>
 *
 * <p>特别注意：{@code DeliveryServiceImpl.handleCallback} 的 finally 块会清理
 * BaseContext（防止线程复用串租户），因此连续两次 perform 的测试方法，
 * 必须在第二次 perform 前重新设置租户上下文。</p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = "classpath:schema-delivery.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DeliveryServiceTest {

    @Autowired
    private DeliveryService deliveryService;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 测试环境初始化租户上下文（MockMvc 不经过 LoginCheckFilter，需手动设置）
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
        params.put("tenantId", "1");
        deliveryService.handleCallback("MEITUAN", params);

        // handleCallback 的 finally 块会清理 BaseContext，后续服务调用需重新设置租户上下文
        BaseContext.setCurrentTenantId(1L);
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
        params.put("tenantId", "1");
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
        params.put("phone", "13700139000");
        params.put("address", "北京市海淀区xxx");
        params.put("tenantId", "1");
        deliveryService.handleCallback("MEITUAN", params);

        // handleCallback 的 finally 块会清理 BaseContext，后续服务调用需重新设置租户上下文
        BaseContext.setCurrentTenantId(1L);
        DeliveryOrder order = deliveryService.getByPlatformOrderId("MT_TRACK_002");
        assertNotNull(order);
        assertEquals("MEITUAN", order.getPlatform());
        assertEquals("MT_TRACK_002", order.getPlatformOrderId());
    }

    /**
     * 验证通过 HTTP 接口完成「平台回调创建订单 → 商家接单」的完整流程。
     *
     * <p>MockMvc 不经过 LoginCheckFilter，handleCallback 的 finally 块又会清理 BaseContext，
     * 所以接单请求前必须重新设置租户上下文，否则 acceptOrder 的 fail-closed 校验会拒绝接单。</p>
     */
    @Test
    void testControllerAcceptOrder() throws Exception {
        String createParams = "{\"type\":\"new_order\",\"platformOrderId\":\"MT123456\",\"dishSummary\":\"测试\",\"amount\":\"10.00\",\"userName\":\"王五\",\"phone\":\"13600136000\",\"address\":\"测试地址\",\"tenantId\":\"1\"}";
        mockMvc.perform(post("/api/delivery/callback/MEITUAN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createParams)
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 回调的 finally 块清理了 BaseContext，接单请求前需重新设置租户上下文
        // （acceptOrder 内部 fail-closed：tenantId 为 null 时直接返回 false）
        BaseContext.setCurrentTenantId(1L);

        String json = "{\"platform\":\"MEITUAN\",\"platformOrderId\":\"MT123456\"}";
        mockMvc.perform(post("/api/delivery/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    /**
     * 验证通过平台订单号查询配送追踪信息。
     *
     * <p>tracking 接口调用 getByPlatformOrderId，该方法依赖 BaseContext 的 tenantId
     * 做租户过滤。回调请求的 finally 块清理了 BaseContext，需在 tracking 请求前重置。</p>
     */
    @Test
    void testTrackingByOrderId() throws Exception {
        String createParams = "{\"type\":\"new_order\",\"platformOrderId\":\"MT_TRACK_001\",\"dishSummary\":\"麻婆豆腐\",\"amount\":\"18.00\",\"userName\":\"赵六\",\"phone\":\"13500135000\",\"address\":\"北京市西城区xxx\",\"tenantId\":\"1\"}";
        mockMvc.perform(post("/api/delivery/callback/MEITUAN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createParams)
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        // 回调的 finally 块清理了 BaseContext，tracking 请求前需重新设置租户上下文
        BaseContext.setCurrentTenantId(1L);

        mockMvc.perform(get("/api/delivery/tracking/MT_TRACK_001")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.platformOrderId").value("MT_TRACK_001"))
                .andExpect(jsonPath("$.data.platform").value("MEITUAN"));
    }
}

