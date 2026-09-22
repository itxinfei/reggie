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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.reggie.test.TestDatabaseCleaner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EmployeeControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("employee");

        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setUsername("admin");
        employee.setName("管理员");
        employee.setPassword(PasswordUtils.encodePassword("123456"));
        employee.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        employee.setPhone("13800138000");
        employee.setIdNumber("110101199001011234");
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
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder = post("/employee")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newemp\",\"name\":\"新员工\",\"phone\":\"13700137000\",\"sex\":\"0\"}")
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                });
        mockMvc.perform(withCsrfToken(mockMvc, builder))
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
        mockMvc.perform(withCsrfToken(mockMvc, put("/employee")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"name\":\"修改后管理员\",\"phone\":\"13600136000\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("员工信息修改成功"));

        org.junit.jupiter.api.Assertions.assertEquals("修改后管理员", employeeService.getById(1L).getName());
    }

    /** 构造一个已占用工号 EMP001 的在职员工（绕过控制器直接落库） */
    private void prepareEmployeeWithJobNumber(long id, String username, String jobNumber) {
        Employee emp = new Employee();
        emp.setId(id);
        emp.setUsername(username);
        emp.setName("员工" + id);
        emp.setPassword(PasswordUtils.encodePassword("123456"));
        emp.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        emp.setPhone("13700137" + String.format("%03d", id));
        emp.setStatus(1);
        emp.setSex("1");
        emp.setRole(0);
        emp.setTenantId(1L);
        emp.setJobNumber(jobNumber);
        employeeService.save(emp);
    }

    @Test
    void testSaveDuplicateJobNumberRejected() throws Exception {
        prepareEmployeeWithJobNumber(2L, "emp1", "EMP001");

        // 同租户再建同工号员工，应被应用层唯一校验拦截
        MockHttpServletRequestBuilder duplicate = post("/employee")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"emp2\",\"name\":\"员工乙\",\"phone\":\"13700137003\",\"sex\":\"1\",\"jobNumber\":\"EMP001\"}");
        mockMvc.perform(withCsrfToken(mockMvc, duplicate))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已存在")));

        // 不同工号应正常新增
        MockHttpServletRequestBuilder ok = post("/employee")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"emp3\",\"name\":\"员工丙\",\"phone\":\"13700137004\",\"sex\":\"1\",\"jobNumber\":\"EMP002\"}");
        mockMvc.perform(withCsrfToken(mockMvc, ok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testUpdateAvatarPositionAndJobNumber() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, put("/employee")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"name\":\"管理员\",\"avatar\":\"images/avatar/a1.png\",\"position\":\"店长\",\"jobNumber\":\"EMP009\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Employee updated = employeeService.getById(1L);
        org.junit.jupiter.api.Assertions.assertEquals("images/avatar/a1.png", updated.getAvatar());
        org.junit.jupiter.api.Assertions.assertEquals("店长", updated.getPosition());
        org.junit.jupiter.api.Assertions.assertEquals("EMP009", updated.getJobNumber());
    }

    @Test
    void testUpdateDuplicateJobNumberRejected() throws Exception {
        prepareEmployeeWithJobNumber(2L, "emp1", "EMP001");

        // 把 admin 工号改成已被占用的 EMP001，应失败
        mockMvc.perform(withCsrfToken(mockMvc, put("/employee")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L)
                .with(request -> {
                    request.setAttribute("employeeId", 1L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":1,\"name\":\"管理员\",\"phone\":\"13800138000\",\"jobNumber\":\"EMP001\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已存在")));
    }

    @Test
    void testGetCurrentEmployee() throws Exception {
        // /employee/me 仅需登录，回填当前员工含新增头像/工号/岗位字段
        mockMvc.perform(get("/employee/me")
                .sessionAttr("employee", 1L)
                .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
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




