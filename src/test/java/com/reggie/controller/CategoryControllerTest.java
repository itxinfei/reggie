package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.category.model.Category;
import com.reggie.module.category.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        Category category = new Category();
        category.setId(1L);
        category.setName("测试分类");
        category.setType(1);
        category.setSort(1);
        categoryService.save(category);
    }

    @Test
    void testSave() throws Exception {
        Category category = new Category();
        category.setName("新增分类");
        category.setType(2);
        category.setSort(2);

        mockMvc.perform(post("/category")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"新增分类\",\"type\":2,\"sort\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("新增分类成功"));
    }

    @Test
    void testPage() throws Exception {
        mockMvc.perform(get("/category/page")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("测试分类"));
    }

    @Test
    void testDelete() throws Exception {
        Category cat2 = new Category();
        cat2.setName("待删除分类");
        cat2.setType(1);
        cat2.setSort(10);
        categoryService.save(cat2);
        long generatedId = cat2.getId();

        mockMvc.perform(delete("/category/" + generatedId)
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("分类删除成功"));
    }

    @Test
    void testUpdate() throws Exception {
        mockMvc.perform(put("/category")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"name\":\"修改后分类\",\"type\":1,\"sort\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("分类修改成功"));

        org.junit.jupiter.api.Assertions.assertEquals("修改后分类", categoryService.getById(1L).getName());
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/category/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("测试分类"));
    }

    @Test
    void testList() throws Exception {
        mockMvc.perform(get("/category/list")
                .param("type", "1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].name").value("测试分类"));
    }
}



