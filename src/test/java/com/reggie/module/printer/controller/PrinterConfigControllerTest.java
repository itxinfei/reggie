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
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Sql(scripts = "classpath:schema-printer.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class PrinterConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PrinterConfigService printerConfigService;

    private static final long EMPLOYEE_ID = 1L;
    private static final long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        // 服务层/权限切面需要用到的上下文（ThreadLocal）
        BaseContext.setCurrentId(EMPLOYEE_ID);
        BaseContext.setCurrentTenantId(TENANT_ID);
    }

    /**
     * 员工会话：EmployeeGuardAspect 在 MockMvc 下读不到 @WebFilter 写入的 request 属性
     * employeeId，回退到 session 属性 "employee"（Long）。每个请求都必须带上该会话属性，
     * 否则切面返回 code=0（无权限），导致接口被拒。
     */
    private RequestBuilder withEmployee(RequestBuilder builder) {
        return ((MockHttpServletRequestBuilder) builder)
                .sessionAttr("employee", EMPLOYEE_ID)
                .sessionAttr("tenantId", TENANT_ID);
    }

    @Test
    void testSavePrinterConfig() throws Exception {
        mockMvc.perform(withEmployee(post("/printer/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tenantId\":1,\"name\":\"测试打印机\",\"type\":\"NETWORK\",\"printType\":\"KITCHEN\",\"sort\":1}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testUpdatePrinterConfig() throws Exception {
        PrinterConfig config = new PrinterConfig();
        config.setName("原名称");
        config.setType("NETWORK");
        config.setPrintType("KITCHEN");
        config.setTenantId(1L);
        printerConfigService.save(config);

        mockMvc.perform(withEmployee(put("/printer/config")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":" + config.getId() + ",\"name\":\"新名称\",\"type\":\"NETWORK\",\"printType\":\"KITCHEN\",\"sort\":1}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testDeletePrinterConfig() throws Exception {
        PrinterConfig config = new PrinterConfig();
        config.setName("待删除打印机");
        config.setType("NETWORK");
        config.setPrintType("KITCHEN");
        config.setTenantId(1L);
        printerConfigService.save(config);

        mockMvc.perform(withEmployee(delete("/printer/config/" + config.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testGetPrinterConfigById() throws Exception {
        PrinterConfig config = new PrinterConfig();
        config.setName("查询测试打印机");
        config.setType("NETWORK");
        config.setPrintType("KITCHEN");
        config.setTenantId(1L);
        printerConfigService.save(config);

        mockMvc.perform(withEmployee(get("/printer/config/" + config.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.name").value("查询测试打印机"));
    }

    @Test
    void testGetPrinterConfigByIdNotFound() throws Exception {
        mockMvc.perform(withEmployee(get("/printer/config/999")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
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
        config2.setPrintType("BILL");
        config2.setTenantId(1L);
        config2.setSort(2);
        printerConfigService.save(config2);

        // 不过滤：返回全部
        mockMvc.perform(withEmployee(get("/printer/config/list")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));

        // 按 printType 过滤（print_types 字段，逗号分隔集合）
        mockMvc.perform(withEmployee(get("/printer/config/list")
                .param("printType", "KITCHEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].printType").value("KITCHEN"));
    }

    @Test
    void testPagePrinterConfigs() throws Exception {
        mockMvc.perform(withEmployee(get("/printer/config/page")
                .param("page", "1")
                .param("pageSize", "10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void testPagePrinterConfigsWithName() throws Exception {
        mockMvc.perform(withEmployee(get("/printer/config/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("name", "测试")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").exists());
    }
}
