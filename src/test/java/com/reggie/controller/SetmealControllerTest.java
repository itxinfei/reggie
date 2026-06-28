package com.reggie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.dto.SetmealDto;
import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
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
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SetmealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private Setmeal createTestSetmeal() {
        BaseContext.setCurrentTenantId(1L);

        Setmeal setmeal = new Setmeal();
        setmeal.setId(1L);
        setmeal.setName("测试套餐");
        setmeal.setCategoryId(1L);
        setmeal.setPrice(new BigDecimal("50.00"));
        setmeal.setCode("S001");
        setmeal.setStatus(1);
        setmealService.save(setmeal);
        return setmeal;
    }

    @Test
    void testGetSetmealById() throws Exception {
        createTestSetmeal();

        mockMvc.perform(get("/setmeal/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("测试套餐"));
    }

    @Test
    void testUpdateSetmeal() throws Exception {
        createTestSetmeal();

        SetmealDto dto = new SetmealDto();
        dto.setId(1L);
        dto.setName("修改后的套餐");
        dto.setCategoryId(1L);
        dto.setPrice(new BigDecimal("60.00"));
        dto.setCode("S001");
        dto.setStatus(1);

        SetmealDish dish = new SetmealDish();
        dish.setDishId(1L);
        dish.setName("测试菜品");
        dish.setPrice(new BigDecimal("20.00"));
        dish.setCopies(2);
        List<SetmealDish> dishes = new ArrayList<>();
        dishes.add(dish);
        dto.setSetmealDishes(dishes);

        mockMvc.perform(put("/setmeal")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("修改套餐成功"));

        org.junit.jupiter.api.Assertions.assertEquals("修改后的套餐", setmealService.getById(1L).getName());
    }

    @Test
    void testUpdateStatus() throws Exception {
        Setmeal setmeal = new Setmeal();
        setmeal.setId(2L);
        setmeal.setName("状态测试套餐");
        setmeal.setCategoryId(1L);
        setmeal.setPrice(new BigDecimal("30.00"));
        setmeal.setCode("S002");
        setmeal.setStatus(1);
        setmealService.save(setmeal);

        mockMvc.perform(post("/setmeal/status/0")
                .param("ids", "2")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("操作成功"));

        org.junit.jupiter.api.Assertions.assertEquals(0, setmealService.getById(2L).getStatus());
    }

    @Test
    void testDishList() throws Exception {
        createTestSetmeal();

        SetmealDish dish = new SetmealDish();
        dish.setId(1L);
        dish.setSetmealId(1L);
        dish.setDishId(1L);
        dish.setName("套餐内菜品");
        dish.setPrice(new BigDecimal("20.00"));
        dish.setCopies(2);
        setmealDishService.save(dish);

        mockMvc.perform(get("/setmeal/dish/1")
                .sessionAttr("user", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].name").value("套餐内菜品"));
    }
}
