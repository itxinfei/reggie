package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.common.R;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.shopping.model.ShoppingCart;
import com.reggie.module.address.model.AddressBook;
import com.reggie.module.order.service.OrderService;
import com.reggie.module.order.service.OrderDetailService;
import com.reggie.module.shopping.service.ShoppingCartService;
import com.reggie.module.address.service.AddressBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class MobileOrderControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("order_detail", "orders");
        // 登录用户用测试专用高 ID，避开 reggie 库租户 1 演示用户的小主键（单库改造，2026-10-02）
        BaseContext.setCurrentId(994001L);
        BaseContext.setCurrentTenantId(999L);
    }

    @Test
    void testUserPage() throws Exception {
        Orders order = new Orders();
        order.setId(997001L);
        order.setNumber("20250101001");
        order.setUserId(994001L);
        order.setStatus(2);
        order.setAmount(new BigDecimal("99.00"));
        order.setOrderTime(LocalDateTime.now());
        order.setCheckoutTime(LocalDateTime.now());
        order.setUserName("测试用户");
        order.setPhone("13800138000");
        order.setAddress("测试地址");
        order.setConsignee("收餐人");
        orderService.save(order);

        OrderDetail detail = new OrderDetail();
        detail.setId(997101L);
        detail.setOrderId(997001L);
        detail.setDishId(992001L);
        detail.setName("测试菜品");
        detail.setNumber(2);
        detail.setAmount(new BigDecimal("99.00"));
        orderDetailService.save(detail);

        mockMvc.perform(get("/order/userPage")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("user", 994001L).sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].orderDetails[0].name").value("测试菜品"));
    }

    @Test
    void testAgain() throws Exception {
        Orders order = new Orders();
        order.setId(997002L);
        order.setNumber("20250101002");
        order.setUserId(994001L);
        order.setStatus(2);
        order.setAmount(new BigDecimal("59.00"));
        order.setOrderTime(LocalDateTime.now());
        order.setUserName("测试用户");
        orderService.save(order);

        OrderDetail detail = new OrderDetail();
        detail.setId(997102L);
        detail.setOrderId(997002L);
        detail.setDishId(992001L);
        detail.setName("再来一单菜品");
        detail.setNumber(1);
        detail.setAmount(new BigDecimal("59.00"));
        orderDetailService.save(detail);

        mockMvc.perform(withCsrfToken(mockMvc, post("/order/again")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":997002}")
                .sessionAttr("user", 994001L).sessionAttr("tenantId", 999L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testUserPageEmpty() throws Exception {
        mockMvc.perform(get("/order/userPage")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("user", 994001L).sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}



