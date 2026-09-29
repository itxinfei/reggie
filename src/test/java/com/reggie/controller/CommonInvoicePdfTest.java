package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.utils.ImageStoragePathResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2-5 发票 PDF 上传 / 跨角色下载鉴权集成测试。
 * 员工上传落 private/admin/invoice；员工与购买顾客(任一登录)可下载，未登录 401。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CommonInvoicePdfTest {

    @Autowired
    private MockMvc mockMvc;

    @Value("${reggie.path:}")
    private String configPath;

    private String uploadedRel;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(999L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.remove();
        // 清理本次落盘的发票文件
        if (uploadedRel != null) {
            File f = new File(ImageStoragePathResolver.resolveRoot(configPath), uploadedRel);
            if (f.exists() && !f.delete()) {
                f.deleteOnExit();
            }
            uploadedRel = null;
        }
    }

    private MockMultipartFile pdfFile(String name, byte[] content) {
        return new MockMultipartFile("file", name, MediaType.APPLICATION_PDF_VALUE, content);
    }

    @Test
    void uploadAndDownload_crossRole() throws Exception {
        byte[] pdf = "%PDF-1.4\n%test invoice payload".getBytes(StandardCharsets.UTF_8);

        // 1. 员工上传发票 PDF
        MvcResult result = mockMvc.perform(multipart("/common/upload")
                        .file(pdfFile("invoice.pdf", pdf))
                        .param("bizType", "invoice")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(
                        org.hamcrest.Matchers.startsWith("private/admin/invoice/")))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        uploadedRel = body.replaceAll(".*\"data\":\"([^\"]+)\".*", "$1");

        // 2. 员工下载放行
        mockMvc.perform(get("/common/download").param("name", uploadedRel)
                        .sessionAttr("employee", 1L))
                .andExpect(status().isOk());

        // 3. 顾客（user 会话）下载放行——跨角色
        mockMvc.perform(get("/common/download").param("name", uploadedRel)
                        .sessionAttr("user", 9001L))
                .andExpect(status().isOk());

        // 4. 未登录拒绝
        mockMvc.perform(get("/common/download").param("name", uploadedRel))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadFakePdf_magicRejected() throws Exception {
        // 扩展名 .pdf 但内容非 %PDF- 文件头，魔数校验应拒绝
        byte[] fake = "not a real pdf content".getBytes(StandardCharsets.UTF_8);
        mockMvc.perform(multipart("/common/upload")
                        .file(pdfFile("fake.pdf", fake))
                        .param("bizType", "invoice")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
