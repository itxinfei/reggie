package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.entity.ShoppingCart;
import com.reggie.service.ShoppingCartService;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ShoppingCartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
    }

    @Test
    void testSubReduceQuantity() throws Exception {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1L);
        cart.setUserId(1L);
        cart.setDishId(1L);
        cart.setName("测试菜品");
        cart.setNumber(3);
        cart.setAmount(new BigDecimal("10.00"));
        shoppingCartService.save(cart);

        mockMvc.perform(post("/shoppingCart/sub")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishId\":1}")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.number").value(2));
    }

    @Test
    void testSubRemoveWhenOne() throws Exception {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(2L);
        cart.setUserId(1L);
        cart.setDishId(2L);
        cart.setName("单个菜品");
        cart.setNumber(1);
        cart.setAmount(new BigDecimal("5.00"));
        shoppingCartService.save(cart);

        mockMvc.perform(post("/shoppingCart/sub")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishId\":2}")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
