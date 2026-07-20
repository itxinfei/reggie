package com.reggie.module.printer;

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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:schema-printer.sql")
public class PrinterConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrinterConfigService printerConfigService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
    }

    @Test
    void testSave() throws Exception {
        String json = "{\"name\":\"前台打印机\",\"type\":\"USB\",\"brand\":\"佳博\",\"deviceId\":\"SN12345\",\"paperSize\":\"58mm\",\"printType\":\"BILL\",\"status\":1,\"sort\":0}";
        mockMvc.perform(post("/printer/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testGetById() throws Exception {
        PrinterConfig config = new PrinterConfig();
        config.setId(1L);
        config.setTenantId(1L);
        config.setName("测试打印机");
        config.setType("USB");
        config.setPrintType("BILL");
        config.setStatus(1);
        config.setCreatedTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        printerConfigService.save(config);

        mockMvc.perform(get("/printer/config/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("测试打印机"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/printer/config/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testUpdate() throws Exception {
        PrinterConfig config = new PrinterConfig();
        config.setId(1L);
        config.setTenantId(1L);
        config.setName("测试打印机");
        config.setType("USB");
        config.setPrintType("BILL");
        config.setStatus(1);
        config.setCreatedTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        printerConfigService.save(config);

        String json = "{\"id\":1,\"name\":\"更新后的打印机\",\"type\":\"TCP\",\"printType\":\"KITCHEN\",\"status\":1}";
        mockMvc.perform(put("/printer/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        PrinterConfig updated = printerConfigService.getById(1L);
        assert "更新后的打印机".equals(updated.getName());
    }

    @Test
    void testDelete() throws Exception {
        PrinterConfig config = new PrinterConfig();
        config.setId(1L);
        config.setTenantId(1L);
        config.setName("测试打印机");
        config.setType("USB");
        config.setPrintType("BILL");
        config.setStatus(1);
        config.setCreatedTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        printerConfigService.save(config);

        mockMvc.perform(delete("/printer/config/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assert printerConfigService.getById(1L) == null;
    }

    @Test
    void testPage() throws Exception {
        for (int i = 1; i <= 5; i++) {
            PrinterConfig pc = new PrinterConfig();
            pc.setId((long) i);
            pc.setTenantId(1L);
            pc.setName("打印机" + i);
            pc.setType("USB");
            pc.setPrintType("BILL");
            pc.setStatus(1);
            pc.setSort(i);
            pc.setCreatedTime(LocalDateTime.now());
            pc.setUpdateTime(LocalDateTime.now());
            printerConfigService.save(pc);
        }

        mockMvc.perform(get("/printer/config/page")
                .param("page", "1")
                .param("pageSize", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(3));
    }

    @Test
    void testList() throws Exception {
        PrinterConfig config1 = new PrinterConfig();
        config1.setId(1L);
        config1.setTenantId(1L);
        config1.setName("前台打印机");
        config1.setType("USB");
        config1.setPrintType("BILL");
        config1.setStatus(1);
        config1.setSort(1);
        config1.setCreatedTime(LocalDateTime.now());
        config1.setUpdateTime(LocalDateTime.now());
        printerConfigService.save(config1);

        PrinterConfig config2 = new PrinterConfig();
        config2.setId(2L);
        config2.setTenantId(1L);
        config2.setName("后厨打印机");
        config2.setType("TCP");
        config2.setPrintType("KITCHEN");
        config2.setStatus(1);
        config2.setSort(2);
        config2.setCreatedTime(LocalDateTime.now());
        config2.setUpdateTime(LocalDateTime.now());
        printerConfigService.save(config2);

        mockMvc.perform(get("/printer/config/list")
                .param("printType", "KITCHEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].printType").value("KITCHEN"));
    }
}
