package com.reggie.controller;

import com.reggie.entity.Orders;
import com.reggie.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService orderService;

    @Test
    void testOrderPage() throws Exception {
        Orders order = new Orders();
        order.setId(1L);
        order.setNumber("2024001");
        order.setStatus(2);
        order.setAmount(new BigDecimal("100.00"));
        order.setUserId(1L);
        orderService.save(order);

        mockMvc.perform(get("/order/page")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("employee", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].number").value("2024001"));
    }

    @Test
    void testUpdateOrderStatus() throws Exception {
        Orders order = new Orders();
        order.setId(2L);
        order.setNumber("2024002");
        order.setStatus(2);
        order.setAmount(new BigDecimal("200.00"));
        order.setUserId(1L);
        orderService.save(order);

        mockMvc.perform(put("/order")
                .sessionAttr("employee", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":2,\"status\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("操作成功"));

        org.junit.jupiter.api.Assertions.assertEquals(3, orderService.getById(2L).getStatus());
    }

    @Test
    void testOrderPageWithFilter() throws Exception {
        Orders order = new Orders();
        order.setId(3L);
        order.setNumber("2024003");
        order.setStatus(2);
        order.setAmount(new BigDecimal("300.00"));
        order.setUserId(1L);
        orderService.save(order);

        mockMvc.perform(get("/order/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("number", "2024003")
                .sessionAttr("employee", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }
}
