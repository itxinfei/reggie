package com.reggie.config;

import com.reggie.utils.ImageStoragePathResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /uploads/public/** 静态映射：公开图匿名可读，private 不进静态映射。
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PublicUploadsStaticMappingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicUploadIsAnonymousReadable() throws Exception {
        String root = ImageStoragePathResolver.resolveRoot(null);
        Path dir = new File(root, "public/admin/dishes").toPath();
        Files.createDirectories(dir);
        Path probe = dir.resolve("static-mapping-probe.txt");
        Files.write(probe, "hello-public".getBytes("UTF-8"));
        assertTrue(Files.exists(probe));

        mockMvc.perform(get("/uploads/public/admin/dishes/static-mapping-probe.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string("hello-public"));

        Files.deleteIfExists(probe);
    }

    @Test
    void privateUploadNotMapped() throws Exception {
        mockMvc.perform(get("/uploads/private/admin/purchase/202609/whatever.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void sharedScriptAnonymousReadable() throws Exception {
        mockMvc.perform(get("/shared/js/img-path.js"))
                .andExpect(status().isOk());
    }
}
