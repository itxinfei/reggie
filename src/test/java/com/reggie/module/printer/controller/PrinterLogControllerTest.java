package com.reggie.module.printer.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.printer.model.PrinterLog;
import com.reggie.module.printer.service.PrinterLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PrinterLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrinterLogService printerLogService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testPagePrinterLogs() throws Exception {
        mockMvc.perform(get("/printer/log/page")
                .param("page", "1")
                .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }

    @Test
    void testPagePrinterLogsWithOrderId() throws Exception {
        mockMvc.perform(get("/printer/log/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("orderId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists());
    }
}
