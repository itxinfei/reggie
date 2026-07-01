package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.dto.DishDto;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
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
public class DishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);

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
    void testSave() throws Exception {
        DishDto dto = new DishDto();
        dto.setName("新菜品");
        dto.setCategoryId(1L);
        dto.setPrice(new BigDecimal("15.00"));
        dto.setCode("002");
        dto.setImage("new.jpg");
        dto.setStatus(1);
        dto.setSort(2);

        List<DishFlavor> flavors = new ArrayList<>();
        DishFlavor flavor = new DishFlavor();
        flavor.setName("辣度");
        flavor.setValue("微辣");
        flavors.add(flavor);
        dto.setFlavors(flavors);

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        mockMvc.perform(post("/dish")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("新增菜品成功"));
    }

    @Test
    void testPage() throws Exception {
        mockMvc.perform(get("/dish/page")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("测试菜品"));
    }

    @Test
    void testPageByName() throws Exception {
        mockMvc.perform(get("/dish/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("name", "测试")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/dish/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("测试菜品"));
    }

    @Test
    void testUpdate() throws Exception {
        DishDto dto = new DishDto();
        dto.setId(1L);
        dto.setName("修改后菜品");
        dto.setCategoryId(1L);
        dto.setPrice(new BigDecimal("20.00"));
        dto.setCode("001");
        dto.setImage("test.jpg");
        dto.setStatus(1);
        dto.setSort(1);

        List<DishFlavor> flavors = new ArrayList<>();
        DishFlavor flavor = new DishFlavor();
        flavor.setDishId(1L);
        flavor.setName("辣度");
        flavor.setValue("中辣");
        flavors.add(flavor);
        dto.setFlavors(flavors);

        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

        mockMvc.perform(put("/dish")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("修改菜品成功"));

        org.junit.jupiter.api.Assertions.assertEquals("修改后菜品", dishService.getById(1L).getName());
    }

    @Test
    void testDelete() throws Exception {
        Dish dish2 = new Dish();
        dish2.setId(2L);
        dish2.setName("待删除菜品");
        dish2.setCategoryId(1L);
        dish2.setPrice(new BigDecimal("5.00"));
        dish2.setCode("003");
        dish2.setImage("del.jpg");
        dish2.setStatus(1);
        dish2.setSort(2);
        dishService.save(dish2);

        mockMvc.perform(delete("/dish")
                .param("ids", "2")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("删除成功"));

        org.junit.jupiter.api.Assertions.assertNull(dishService.getById(2L));
    }

    @Test
    void testUpdateStatus() throws Exception {
        mockMvc.perform(post("/dish/status/0")
                .param("ids", "1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("操作成功"));

        Dish updated = dishService.getById(1L);
        org.junit.jupiter.api.Assertions.assertEquals(0, updated.getStatus());
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
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        org.junit.jupiter.api.Assertions.assertEquals(0, dishService.getById(1L).getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(0, dishService.getById(2L).getStatus());
    }

    @Test
    void testList() throws Exception {
        mockMvc.perform(get("/dish/list")
                .param("categoryId", "1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].name").value("测试菜品"));
    }
}
