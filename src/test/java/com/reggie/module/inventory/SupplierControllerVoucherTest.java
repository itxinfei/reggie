package com.reggie.module.inventory;

import com.reggie.common.BaseContext;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.module.inventory.service.SupplierService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 供应商编辑接口的资质图片回归测试：
 * 验证 PUT /api/inventory/supplier 白名单包含 licenseImages，改动不被静默丢弃。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema-inventory.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class SupplierControllerVoucherTest extends com.reggie.controller.BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierService supplierService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(999L);
    }

    @Test
    void testUpdateLicenseImages() throws Exception {
        Supplier supplier = new Supplier();
        supplier.setTenantId(999L);
        supplier.setName("凭证供应商");
        supplier.setContact("旧联系人");
        supplier.setPhone("13500135000");
        supplier.setAddress("旧地址");
        supplier.setStatus(1);
        supplier.setLicenseImages("images/supplier/old.png");
        supplierService.save(supplier);

        String body = "{\"id\":" + supplier.getId()
                + ",\"name\":\"凭证供应商\",\"contact\":\"新联系人\",\"phone\":\"13600136000\","
                + "\"address\":\"新地址\",\"status\":1,"
                + "\"licenseImages\":\"images/supplier/new1.png,images/supplier/new2.png\"}";

        MockHttpServletRequestBuilder request = put("/api/inventory/supplier")
                .sessionAttr("employee", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        mockMvc.perform(withCsrfToken(mockMvc, request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Supplier updated = supplierService.getById(supplier.getId());
        org.junit.jupiter.api.Assertions.assertEquals(
                "images/supplier/new1.png,images/supplier/new2.png", updated.getLicenseImages());
    }
}
