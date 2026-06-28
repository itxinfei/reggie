package com.reggie.controller;

import com.reggie.entity.Dish;
import com.reggie.service.DishService;
import org.junit.jupiter.api.Assertions;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DishService dishService;

    @BeforeEach
    void setUp() {
        Dish dish = new Dish();
        dish.setId(1L);
        dish.setName("测试菜品");
        dish.setCategoryId(1L);
        dish.setPrice(new BigDecimal("10.00"));
        dish.setCode("001");
        dish.setImage("test.jpg");
        dish.setStatus(1);
        dish.setSort(1);
        dishService.save(dish);
    }

    @Test
    void testUpdateStatus() throws Exception {
        mockMvc.perform(post("/dish/status/0")
                .param("ids", "1")
                .sessionAttr("employee", 1L)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("操作成功"));

        Dish updated = dishService.getById(1L);
        Assertions.assertEquals(0, updated.getStatus());
    }

    @Test
    void testUpdateStatusBatch() throws Exception {
        Dish dish2 = new Dish();
        dish2.setId(2L);
        dish2.setName("测试菜品2");
        dish2.setCategoryId(1L);
        dish2.setPrice(new BigDecimal("20.00"));
        dish2.setCode("002");
        dish2.setImage("test2.jpg");
        dish2.setStatus(1);
        dish2.setSort(2);
        dishService.save(dish2);

        mockMvc.perform(post("/dish/status/0")
                .param("ids", "1,2")
                .sessionAttr("employee", 1L)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Assertions.assertEquals(0, dishService.getById(1L).getStatus());
        Assertions.assertEquals(0, dishService.getById(2L).getStatus());
    }
}
