package com.reggie.controller;

import com.reggie.module.auth.model.Employee;
import com.reggie.module.auth.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.reggie.test.TestDatabaseCleaner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EmployeeLoginSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("employee");

        // 创建测试专用账号 test_md5_login（隔离租户 999），密码为 123456 的 MD5
        Employee md5User = new Employee();
        md5User.setId(990001L);
        md5User.setUsername("test_md5_login");
        md5User.setName("管理员");
        md5User.setPassword("e10adc3949ba59abbe56e057f20f883e");
        md5User.setPasswordType("MD5");
        md5User.setPhone("13800138000");
        md5User.setSex("1");
        md5User.setIdNumber("110101199001011234");
        md5User.setStatus(1);
        md5User.setTenantId(999L);
        md5User.setCreateUser(990001L);
        md5User.setUpdateUser(990001L);
        employeeService.save(md5User);
    }

    @Test
    void testLoginWithBCryptPassword() throws Exception {
        mockMvc.perform(post("/employee/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test_md5_login\",\"password\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testLoginWithWrongPassword() throws Exception {
        mockMvc.perform(post("/employee/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test_md5_login\",\"password\":\"wrong\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }
}



