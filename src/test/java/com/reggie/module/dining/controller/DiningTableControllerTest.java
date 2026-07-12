package com.reggie.module.dining.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.service.DiningTableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
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
@Sql(scripts = "classpath:schema-dining.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DiningTableControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DiningTableService diningTableService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);

        DiningTable table = new DiningTable();
        table.setId(1L);
        table.setTenantId(1L);
        table.setAreaId(1L);
        table.setName("桌台1");
        table.setSeatCount(4);
        table.setStatus("FREE");
        table.setMinAmount(new BigDecimal("100.00"));
        table.setSort(1);
        table.setCreatedTime(LocalDateTime.now());
        diningTableService.save(table);
    }

    @Test
    void testPage() throws Exception {
        mockMvc.perform(get("/api/dining/table/page")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("桌台1"));
    }

    @Test
    void testSave() throws Exception {
        mockMvc.perform(post("/api/dining/table")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"新桌台\",\"seatCount\":2,\"areaId\":1,\"minAmount\":\"50.00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("新桌台"));
    }

    @Test
    void testUpdate() throws Exception {
        mockMvc.perform(put("/api/dining/table")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"name\":\"修改后桌台\",\"seatCount\":6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("修改桌台成功"));
    }

    @Test
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/dining/table/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("删除桌台成功"));
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/api/dining/table/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("桌台1"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/dining/table/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testChangeStatus() throws Exception {
        mockMvc.perform(put("/api/dining/table/status")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"status\":\"OCCUPIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("修改状态成功"));
    }

    @Test
    void testQrcode() throws Exception {
        mockMvc.perform(get("/api/dining/table/qrcode/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isString());
    }

    @Test
    void testQrcodeNotFound() throws Exception {
        mockMvc.perform(get("/api/dining/table/qrcode/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}