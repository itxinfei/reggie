package com.reggie.controller;

import com.reggie.entity.Employee;
import com.reggie.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class EmployeeLoginSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        // 创建测试用户 admin，密码为 123456 的 MD5 加密
        Employee admin = new Employee();
        admin.setId(1L);
        admin.setUsername("admin");
        admin.setName("管理员");
        admin.setPassword("e10adc3949ba59abbe56e057f20f883e");
        admin.setPasswordType("MD5");
        admin.setStatus(1);
        admin.setTenantId(1L);
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
