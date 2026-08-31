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

        // 创建测试用户 admin，密码为 123456 的 MD5 加密
        Employee admin = new Employee();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setName("管理员");
        admin.setPassword("e10adc3949ba59abbe56e057f20f883e");
        admin.setPasswordType("MD5");
        admin.setPhone("13800138000");
        admin.setSex("1");
        admin.setIdNumber("110101199001011234");
        admin.setStatus(1);
        admin.setTenantId(1L);
        admin.setCreateUser(1L);
        admin.setUpdateUser(1L);
        employeeService.save(admin);
    }

    @Test
    void testLoginWithBCryptPassword() throws Exception {
        mockMvc.perform(post("/employee/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testLoginWithWrongPassword() throws Exception {
        mockMvc.perform(post("/employee/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }
}



