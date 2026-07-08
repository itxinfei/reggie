package com.reggie.module.printer.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.printer.model.PrinterConfig;
import com.reggie.module.printer.service.PrinterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PrinterConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrinterConfigService printerConfigService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testSavePrinterConfig() throws Exception {
        mockMvc.perform(post("/printer/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":1,\"name\":\"测试打印机\",\"type\":\"NETWORK\",\"printType\":\"KITCHEN\",\"sort\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testUpdatePrinterConfig() throws Exception {
        PrinterConfig config = new PrinterConfig();
        config.setName("原名称");
        config.setType("NETWORK");
        config.setPrintType("KITCHEN");
        config.setTenantId(1L);
        printerConfigService.save(config);

        mockMvc.perform(put("/printer/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + config.getId() + ",\"name\":\"新名称\",\"type\":\"NETWORK\",\"printType\":\"KITCHEN\",\"sort\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testDeletePrinterConfig() throws Exception {
        PrinterConfig config = new PrinterConfig();
        config.setName("待删除打印机");
        config.setType("NETWORK");
        config.setPrintType("KITCHEN");
        config.setTenantId(1L);
        printerConfigService.save(config);

        mockMvc.perform(delete("/printer/config/" + config.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testGetPrinterConfigById() throws Exception {
        PrinterConfig config = new PrinterConfig();
        config.setName("查询测试打印机");
        config.setType("NETWORK");
        config.setPrintType("KITCHEN");
        config.setTenantId(1L);
        printerConfigService.save(config);

        mockMvc.perform(get("/printer/config/" + config.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testGetPrinterConfigByIdNotFound() throws Exception {
        mockMvc.perform(get("/printer/config/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testListPrinterConfigs() throws Exception {
        PrinterConfig config1 = new PrinterConfig();
        config1.setName("打印机1");
        config1.setType("NETWORK");
        config1.setPrintType("KITCHEN");
        config1.setTenantId(1L);
        config1.setSort(1);
        printerConfigService.save(config1);

        PrinterConfig config2 = new PrinterConfig();
        config2.setName("打印机2");
        config2.setType("BLUETOOTH");
        config2.setPrintType("KITCHEN");
        config2.setTenantId(1L);
        config2.setSort(2);
        printerConfigService.save(config2);

        mockMvc.perform(get("/printer/config/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testPagePrinterConfigs() throws Exception {
        mockMvc.perform(get("/printer/config/page")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testPagePrinterConfigsWithName() throws Exception {
        mockMvc.perform(get("/printer/config/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("name", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }
}
