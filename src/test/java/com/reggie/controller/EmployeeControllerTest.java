package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.PasswordUtils;
import com.reggie.common.SecurityConstants;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setUsername("admin");
        employee.setName("管理员");
        employee.setPassword(PasswordUtils.encodePassword("123456"));
        employee.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        employee.setPhone("13800138000");
        employee.setStatus(1);
        employee.setSex("1");
        employee.setRole(1);
        employee.setTenantId(1L);
        employeeService.save(employee);
    }

    @Test
    void testLogin() throws Exception {
        mockMvc.perform(post("/employee/login")
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testLoginWrongPassword() throws Exception {
        mockMvc.perform(post("/employee/login")
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testLoginDisabledAccount() throws Exception {
        Employee disabled = new Employee();
        disabled.setId(2L);
        disabled.setUsername("disabled");
        disabled.setName("禁用员工");
        disabled.setPassword(PasswordUtils.encodePassword("123456"));
        disabled.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        disabled.setPhone("13900139000");
        disabled.setStatus(0);
        disabled.setSex("1");
        disabled.setTenantId(1L);
        employeeService.save(disabled);

        mockMvc.perform(post("/employee/login")
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"disabled\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(post("/employee/logout")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("退出成功"));
    }

    @Test
    void testSave() throws Exception {
        mockMvc.perform(post("/employee")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newemp\",\"name\":\"新员工\",\"phone\":\"13700137000\",\"sex\":\"0\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.消息").value("新增员工成功，初始密码已通过短信/邮件发送给用户"));
    }

    @Test
    void testPage() throws Exception {
        mockMvc.perform(get("/employee/page")
                .param("page", "1")
                .param("pageSize", "10")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("admin"));
    }

    @Test
    void testPageByName() throws Exception {
        mockMvc.perform(get("/employee/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("name", "管理")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testUpdate() throws Exception {
        mockMvc.perform(put("/employee")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"name\":\"修改后管理员\",\"phone\":\"13600136000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("员工信息修改成功"));

        org.junit.jupiter.api.Assertions.assertEquals("修改后管理员", employeeService.getById(1L).getName());
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/employee/1")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/employee/999")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}




