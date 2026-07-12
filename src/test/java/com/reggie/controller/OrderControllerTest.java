package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.entity.AddressBook;
import com.reggie.entity.OrderDetail;
import com.reggie.entity.Orders;
import com.reggie.entity.ShoppingCart;
import com.reggie.service.AddressBookService;
import com.reggie.service.OrderDetailService;
import com.reggie.service.OrderService;
import com.reggie.service.ShoppingCartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        jdbcTemplate.update("INSERT INTO user (id, name, phone, status, create_time) VALUES (?, ?, ?, ?, ?)",
                1L, "测试用户", "13800138000", 1, java.time.LocalDateTime.now());

        AddressBook address = new AddressBook();
        address.setId(1L);
        address.setUserId(1L);
        address.setConsignee("张三");
        address.setPhone("13800138000");
        address.setProvinceName("浙江省");
        address.setCityName("杭州市");
        address.setDistrictName("西湖区");
        address.setDetail("测试路1号");
        address.setIsDefault(1);
        addressBookService.save(address);

        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setDishId(1L);
        cart.setName("测试菜品");
        cart.setNumber(2);
        cart.setAmount(new BigDecimal("10.00"));
        cart.setImage("test.jpg");
        cart.setCreateTime(LocalDateTime.now());
        shoppingCartService.save(cart);
    }

    @Test
    void testSubmit() throws Exception {
        mockMvc.perform(post("/order/submit")
                .sessionAttr("user", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressBookId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("下单成功"));
    }

    @Test
    void testSubmitEmptyCart() throws Exception {
        shoppingCartService.remove(null);

        mockMvc.perform(post("/order/submit")
                .sessionAttr("user", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"addressBookId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testOrderPage() throws Exception {
        Orders order = new Orders();
        order.setId(10L);
        order.setNumber("2024001");
        order.setStatus(2);
        order.setAmount(new BigDecimal("100.00"));
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        orderService.save(order);

        mockMvc.perform(get("/order/page")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].number").value("2024001"));
    }

    @Test
    void testOrderPageWithFilter() throws Exception {
        Orders order = new Orders();
        order.setId(11L);
        order.setNumber("2024002");
        order.setStatus(2);
        order.setAmount(new BigDecimal("200.00"));
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        orderService.save(order);

        mockMvc.perform(get("/order/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("number", "2024002")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testList() throws Exception {
        Orders order = new Orders();
        order.setId(20L);
        order.setNumber("2024010");
        order.setStatus(2);
        order.setAmount(new BigDecimal("150.00"));
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        orderService.save(order);

        mockMvc.perform(get("/order/list")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].number").value("2024010"));
    }

    @Test
    void testUserPage() throws Exception {
        Orders order = new Orders();
        order.setId(30L);
        order.setNumber("2024030");
        order.setStatus(2);
        order.setAmount(new BigDecimal("250.00"));
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        orderService.save(order);

        mockMvc.perform(get("/order/userPage")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testAgain() throws Exception {
        Orders order = new Orders();
        order.setId(40L);
        order.setNumber("2024040");
        order.setStatus(4);
        order.setAmount(new BigDecimal("100.00"));
        order.setUserId(1L);
        order.setOrderTime(LocalDateTime.now());
        orderService.save(order);

        OrderDetail detail = new OrderDetail();
        detail.setOrderId(40L);
        detail.setDishId(1L);
        detail.setName("测试菜品");
        detail.setNumber(2);
        detail.setAmount(new BigDecimal("10.00"));
        detail.setImage("test.jpg");
        orderDetailService.save(detail);

        mockMvc.perform(post("/order/again")
                .sessionAttr("user", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("添加购物车成功"));
    }

    @Test
    void testUpdateOrderStatus() throws Exception {
        Orders order = new Orders();
        order.setId(50L);
        order.setNumber("2024050");
        order.setStatus(2);
        order.setAmount(new BigDecimal("200.00"));
        order.setUserId(1L);
        orderService.save(order);

        mockMvc.perform(put("/order")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":50,\"status\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("操作成功"));

        org.junit.jupiter.api.Assertions.assertEquals(3, orderService.getById(50L).getStatus());
    }
}
