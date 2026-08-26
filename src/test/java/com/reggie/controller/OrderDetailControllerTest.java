package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.service.OrderDetailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单明细控制器测试
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class OrderDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderDetailService orderDetailService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("order_detail", "orders");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testGetOrderDetailById() throws Exception {
        // 创建一个测试订单明细
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setId(1L);
        orderDetail.setName("测试菜品");
        orderDetail.setDishId(1L);
        orderDetail.setDishFlavor("微辣");
        orderDetail.setNumber(2);
        orderDetail.setAmount(new java.math.BigDecimal("20.00"));
        orderDetail.setOrderId(1L);
        orderDetailService.save(orderDetail);

        mockMvc.perform(get("/order-detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("测试菜品"));
    }

    @Test
    void testGetOrderDetailNotFound() throws Exception {
        mockMvc.perform(get("/order-detail/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("没有找到该对象"));
    }

    @Test
    void testGetOrderDetailWithDifferentId() throws Exception {
        // 创建另一个测试订单明细（IdType.AUTO，实际 id 由数据库生成）
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setName("另一个测试菜品");
        orderDetail.setDishId(2L);
        orderDetail.setNumber(1);
        orderDetail.setAmount(new java.math.BigDecimal("15.00"));
        orderDetail.setOrderId(1L);
        orderDetailService.save(orderDetail);
        Long actualId = orderDetail.getId();

        mockMvc.perform(get("/order-detail/" + actualId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.id").value(actualId.intValue()))
                .andExpect(jsonPath("$.data.name").value("另一个测试菜品"));
    }
}



