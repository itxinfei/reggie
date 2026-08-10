package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.entity.*;
import com.reggie.service.*;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class MobileOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDetailService orderDetailService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testUserPage() throws Exception {
        Orders order = new Orders();
        order.setId(1L);
        order.setNumber("20250101001");
        order.setUserId(1L);
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
        detail.setId(1L);
        detail.setOrderId(1L);
        detail.setDishId(1L);
        detail.setName("测试菜品");
        detail.setNumber(2);
        detail.setAmount(new BigDecimal("99.00"));
        orderDetailService.save(detail);

        mockMvc.perform(get("/order/userPage")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].orderDetails[0].name").value("测试菜品"));
    }

    @Test
    void testAgain() throws Exception {
        Orders order = new Orders();
        order.setId(2L);
        order.setNumber("20250101002");
        order.setUserId(1L);
        order.setStatus(2);
        order.setAmount(new BigDecimal("59.00"));
        order.setOrderTime(LocalDateTime.now());
        order.setUserName("测试用户");
        orderService.save(order);

        OrderDetail detail = new OrderDetail();
        detail.setId(2L);
        detail.setOrderId(2L);
        detail.setDishId(1L);
        detail.setName("再来一单菜品");
        detail.setNumber(1);
        detail.setAmount(new BigDecimal("59.00"));
        orderDetailService.save(detail);

        mockMvc.perform(post("/order/again")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":2}")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testUserPageEmpty() throws Exception {
        mockMvc.perform(get("/order/userPage")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
