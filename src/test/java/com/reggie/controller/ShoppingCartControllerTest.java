package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.module.shopping.model.ShoppingCart;
import com.reggie.module.shopping.service.ShoppingCartService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ShoppingCartControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("shopping_cart");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testAddDish() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/shopping-cart/add")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishId\":1,\"name\":\"测试菜品\",\"number\":1,\"amount\":10.00,\"image\":\"test.jpg\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.number").value(1));
    }

    @Test
    void testAddDishIncrement() throws Exception {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setDishId(1L);
        cart.setName("测试菜品");
        cart.setNumber(2);
        cart.setAmount(new BigDecimal("10.00"));
        cart.setImage("test.jpg");
        shoppingCartService.save(cart);

        mockMvc.perform(withCsrfToken(mockMvc, post("/shopping-cart/add")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishId\":1,\"name\":\"测试菜品\",\"number\":1,\"amount\":10.00,\"image\":\"test.jpg\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.number").value(3));
    }

    @Test
    void testAddSetmeal() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/shopping-cart/add")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"setmealId\":1,\"name\":\"测试套餐\",\"number\":1,\"amount\":50.00,\"image\":\"setmeal.jpg\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.number").value(1));
    }

    @Test
    void testList() throws Exception {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setDishId(1L);
        cart.setName("测试菜品");
        cart.setNumber(2);
        cart.setAmount(new BigDecimal("10.00"));
        cart.setImage("test.jpg");
        shoppingCartService.save(cart);

        mockMvc.perform(get("/shopping-cart/list")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].name").value("测试菜品"))
                .andExpect(jsonPath("$.data[0].number").value(2));
    }

    @Test
    void testListEmpty() throws Exception {
        mockMvc.perform(get("/shopping-cart/list")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void testSubReduceQuantity() throws Exception {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(2L);
        cart.setUserId(1L);
        cart.setDishId(2L);
        cart.setName("测试菜品2");
        cart.setNumber(3);
        cart.setAmount(new BigDecimal("10.00"));
        shoppingCartService.save(cart);

        mockMvc.perform(withCsrfToken(mockMvc, post("/shopping-cart/sub")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishId\":2}")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.number").value(2));
    }

    @Test
    void testSubRemoveWhenOne() throws Exception {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(3L);
        cart.setUserId(1L);
        cart.setDishId(3L);
        cart.setName("单个菜品");
        cart.setNumber(1);
        cart.setAmount(new BigDecimal("5.00"));
        shoppingCartService.save(cart);

        mockMvc.perform(withCsrfToken(mockMvc, post("/shopping-cart/sub")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishId\":3}")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testClean() throws Exception {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(4L);
        cart.setUserId(1L);
        cart.setDishId(4L);
        cart.setName("待清空菜品");
        cart.setNumber(1);
        cart.setAmount(new BigDecimal("8.00"));
        shoppingCartService.save(cart);

        mockMvc.perform(withCsrfToken(mockMvc, delete("/shopping-cart/clean")
                .sessionAttr("user", 1L).sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("清空购物车成功"));

        org.junit.jupiter.api.Assertions.assertTrue(shoppingCartService.list().isEmpty());
    }
}




