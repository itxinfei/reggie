package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.address.service.AddressBookService;
import com.reggie.module.order.service.statusflow.OrderStatusFlowService;
import com.reggie.test.TestDatabaseCleaner;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C 端外卖交易主链路端到端测试（支付全程 mock，零真实网络/渠道）。
 *
 * <p>链路：加车 → 下单(待付款) → 发起支付(mock) → 模拟支付成功回调(待接单)
 * → 商家接单(配送中) → 用户确认收货(已完成) → 用户评价(待审核) → 我的评价。</p>
 *
 * <p>说明：C 端动作全部走 MockMvc HTTP；中间「商家接单 2→3」非 C 端能力，
 * 用 {@link OrderStatusFlowService#confirmOrder} 直调模拟。test profile 已默认
 * reggie.payment.mock-mode=true，回调只要求带非空 sign，验签恒真。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrderMainFlowIntegrationTest extends BaseControllerTest {

    private static final long USER_ID = 990001L;
    private static final long CATEGORY_ID = 99001L;
    private static final long DISH_ID = 99001L;
    private static final long ADDRESS_ID = 99001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderStatusFlowService orderStatusFlowService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("dish_evaluation", "order_detail", "orders", "payment_order", "refund_record",
                "shopping_cart", "dish", "category", "address_book", "user");
        // 清限流计数，避免同秒跨步骤累积 429
        Set<String> rateLimitKeys = redisTemplate.keys("rate_limit:*");
        if (rateLimitKeys != null && !rateLimitKeys.isEmpty()) {
            redisTemplate.delete(rateLimitKeys);
        }
        BaseContext.setCurrentId(USER_ID);
        BaseContext.setCurrentTenantId(999L);

        jdbcTemplate.update("INSERT INTO user (id, name, phone, status, create_time, tenant_id) VALUES (?, ?, ?, ?, ?, ?)",
                USER_ID, "测试用户", "13800138000", 1, LocalDateTime.now(), 999L);
        jdbcTemplate.update("INSERT INTO category (id, name, type, sort, create_time, update_time, create_user, update_user, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                CATEGORY_ID, "测试分类", 1, 1, LocalDateTime.now(), LocalDateTime.now(), USER_ID, USER_ID, 999L);
        jdbcTemplate.update("INSERT INTO dish (id, category_id, name, code, price, status, stock_qty, image, description, create_time, update_time, create_user, update_user, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                DISH_ID, CATEGORY_ID, "测试菜品", "T001", new BigDecimal("38.00"), 1, new BigDecimal("100"), "t.jpg", "测试",
                LocalDateTime.now(), LocalDateTime.now(), USER_ID, USER_ID, 999L);

        AddressBook address = new AddressBook();
        address.setId(ADDRESS_ID);
        address.setUserId(USER_ID);
        address.setConsignee("张三");
        address.setPhone("13800138000");
        address.setProvinceName("浙江省");
        address.setCityName("杭州市");
        address.setDistrictName("西湖区");
        address.setDetail("测试路1号");
        address.setIsDefault(1);
        addressBookService.save(address);
    }

    /** 从 MockMvc 响应体取 data 对象。 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> dataOf(MvcResult result) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(), Map.class);
        return (Map<String, Object>) body.get("data");
    }

    /** 雪花 ID 经 Jackson 可能序列化为 String，兼容 Number / String 两种形态。 */
    private long toLong(Object value) {
        if (value == null) {
            throw new IllegalStateException("响应中 id 为空");
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private int getStatus(long orderId) {
        Integer status = jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", Integer.class, orderId);
        return status == null ? -1 : status;
    }

    @Test
    void testFullMainFlowWithMockPayment() throws Exception {
        // 1. 加车（服务端取价，不信任前端金额）
        mockMvc.perform(withCsrfToken(mockMvc, post("/shopping-cart/add")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishId\":" + DISH_ID + ",\"name\":\"测试菜品\",\"number\":1,\"amount\":38.00,\"image\":\"t.jpg\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.number").value(1));

        // 2. 下单（payMethod=3 支付宝）→ 待付款(1)
        MvcResult submitResult = mockMvc.perform(withCsrfToken(mockMvc, post("/order/submit")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressBookId\":" + ADDRESS_ID + ",\"payMethod\":3}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1))
                .andReturn();
        long orderId = toLong(dataOf(submitResult).get("id"));

        // 3. 发起支付（mock 渠道）
        MvcResult payResult = mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/pay")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + orderId + ",\"channel\":\"ALIPAY\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.mockMode").value(true))
                .andReturn();
        String tradeNo = (String) dataOf(payResult).get("tradeNo");

        // 4. 模拟支付成功回调（mock 验签恒真，只需 sign + out_trade_no）
        mockMvc.perform(withCsrfToken(mockMvc, post("/api/payment/notify/ALIPAY")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sign\":\"mock\",\"out_trade_no\":\"" + tradeNo + "\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
        // 回调联动订单 待付款(1) → 待接单(2)
        assertEquals(2, getStatus(orderId));

        // 5. 商家接单（商家侧动作，service 直调模拟）→ 配送中(3)
        orderStatusFlowService.confirmOrder(orderId);
        assertEquals(3, getStatus(orderId));

        // 6. 用户确认收货 → 已完成(4)
        mockMvc.perform(withCsrfToken(mockMvc, put("/order/userConfirmReceipt")
                .param("id", String.valueOf(orderId))
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("确认收货成功"));
        assertEquals(4, getStatus(orderId));

        // 7. 用户评价 → 待审核(0)
        MvcResult evalResult = mockMvc.perform(withCsrfToken(mockMvc, post("/api/dish-evaluation")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":" + orderId + ",\"dishId\":" + DISH_ID
                        + ",\"starRating\":5,\"content\":\"菜品不错\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.status").value(0))
                .andReturn();
        long evalId = toLong(dataOf(evalResult).get("id"));

        // 8. 我的评价列表能查到本单评价
        mockMvc.perform(get("/api/dish-evaluation/user/my")
                .sessionAttr("user", USER_ID).sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(String.valueOf(evalId)))
                .andExpect(jsonPath("$.data.records[0].starRating").value(5));
    }
}
