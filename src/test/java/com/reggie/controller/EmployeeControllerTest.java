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
        // 清理 tenant=2 的跨租户固定夹具（id=50/60），保证连续多轮跑测不撞主键
        cleaner.cleanByCondition("employee", "id IN (50, 60)");

        BaseContext.setCurrentId(990001L);
        BaseContext.setCurrentTenantId(999L);

        Employee employee = new Employee();
        employee.setId(990001L);
        employee.setUsername("test_admin");
        employee.setName("管理员");
        employee.setPassword(PasswordUtils.encodePassword("123456"));
        employee.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        employee.setPhone("13800138000");
        employee.setIdNumber("110101199001011234");
        employee.setStatus(1);
        employee.setSex("1");
        employee.setRole(1);
        employee.setTenantId(999L);
        employeeService.save(employee);
    }

    @Test
    void testLogin() throws Exception {
        mockMvc.perform(post("/employee/login")
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test_admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testLoginWrongPassword() throws Exception {
        mockMvc.perform(post("/employee/login")
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test_admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testLoginDisabledAccount() throws Exception {
        Employee disabled = new Employee();
        disabled.setId(990002L);
        disabled.setUsername("test_disabled");
        disabled.setName("禁用员工");
        disabled.setPassword(PasswordUtils.encodePassword("123456"));
        disabled.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        disabled.setPhone("13900139000");
        disabled.setStatus(0);
        disabled.setSex("1");
        disabled.setTenantId(999L);
        employeeService.save(disabled);

        mockMvc.perform(post("/employee/login")
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test_disabled\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(post("/employee/logout")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("退出成功"));
    }

    @Test
    void testSave() throws Exception {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder = post("/employee")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newemp\",\"name\":\"新员工\",\"phone\":\"13700137000\",\"sex\":\"0\"}")
                .with(request -> {
                    request.setAttribute("employeeId", 990001L);
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
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("test_admin"));
    }

    @Test
    void testPageByName() throws Exception {
        mockMvc.perform(get("/employee/page")
                .param("page", "1")
                .param("pageSize", "10")
                .param("name", "管理")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void testUpdate() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, put("/employee")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L)
                .with(request -> {
                    request.setAttribute("employeeId", 990001L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":990001,\"name\":\"修改后管理员\",\"phone\":\"13600136000\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("员工信息修改成功"));

        org.junit.jupiter.api.Assertions.assertEquals("修改后管理员", employeeService.getById(990001L).getName());
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
        emp.setTenantId(999L);
        emp.setJobNumber(jobNumber);
        employeeService.save(emp);
    }

    @Test
    void testSaveDuplicateJobNumberRejected() throws Exception {
        prepareEmployeeWithJobNumber(2L, "emp1", "EMP001");

        // 同租户再建同工号员工，应被应用层唯一校验拦截
        MockHttpServletRequestBuilder duplicate = post("/employee")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L)
                .with(request -> {
                    request.setAttribute("employeeId", 990001L);
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
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L)
                .with(request -> {
                    request.setAttribute("employeeId", 990001L);
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
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L)
                .with(request -> {
                    request.setAttribute("employeeId", 990001L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":990001,\"name\":\"管理员\",\"avatar\":\"images/avatar/a1.png\",\"position\":\"店长\",\"jobNumber\":\"EMP009\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Employee updated = employeeService.getById(990001L);
        org.junit.jupiter.api.Assertions.assertEquals("images/avatar/a1.png", updated.getAvatar());
        org.junit.jupiter.api.Assertions.assertEquals("店长", updated.getPosition());
        org.junit.jupiter.api.Assertions.assertEquals("EMP009", updated.getJobNumber());
    }

    @Test
    void testUpdateDuplicateJobNumberRejected() throws Exception {
        prepareEmployeeWithJobNumber(2L, "emp1", "EMP001");

        // 把 test_admin 工号改成已被占用的 EMP001，应失败
        mockMvc.perform(withCsrfToken(mockMvc, put("/employee")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L)
                .with(request -> {
                    request.setAttribute("employeeId", 990001L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":990001,\"name\":\"管理员\",\"phone\":\"13800138000\",\"jobNumber\":\"EMP001\"}")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已存在")));
    }

    @Test
    void testGetCurrentEmployee() throws Exception {
        // /employee/me 仅需登录，回填当前员工含新增头像/工号/岗位字段
        mockMvc.perform(get("/employee/me")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("test_admin"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void testGetById() throws Exception {
        mockMvc.perform(get("/employee/990001")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.username").value("test_admin"));
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        mockMvc.perform(get("/employee/999")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /** 直接落库一个他租户（tenantId=2）员工，用于跨租户访问测试 */
    private Employee prepareOtherTenantEmployee() {
        Employee other = new Employee();
        other.setId(50L);
        other.setUsername("othertenant");
        other.setName("他租户员工");
        other.setPassword(PasswordUtils.encodePassword("123456"));
        other.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        other.setPhone("13500135111");
        other.setStatus(1);
        other.setSex("1");
        other.setRole(0);
        other.setTenantId(2L);
        other.setJobNumber("X001");
        employeeService.save(other);
        return other;
    }

    @Test
    void testBadgeQrcodeCrossTenantRejected() throws Exception {
        prepareOtherTenantEmployee();
        // 当前租户为 999，访问租户 2 员工工牌应被拒绝，且措辞与"不存在"一致（无枚举 oracle）
        mockMvc.perform(get("/employee/badge-qrcode/50")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").value("员工不存在"));
    }

    @Test
    void testBadgeQrcodeSameTenantAllowed() throws Exception {
        prepareEmployeeWithJobNumber(2L, "emp1", "EMP001");
        mockMvc.perform(get("/employee/badge-qrcode/2")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(
                        org.hamcrest.Matchers.startsWith("data:image")));
    }

    @Test
    void testSaveBlankJobNumberNormalizedToNull() throws Exception {
        MockHttpServletRequestBuilder builder = post("/employee")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L)
                .with(request -> {
                    request.setAttribute("employeeId", 990001L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"blankjob\",\"name\":\"空工号\",\"phone\":\"13700137005\",\"sex\":\"1\",\"jobNumber\":\"   \"}");
        mockMvc.perform(withCsrfToken(mockMvc, builder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Employee saved = employeeService.lambdaQuery().eq(Employee::getUsername, "blankjob").one();
        org.junit.jupiter.api.Assertions.assertNull(saved.getJobNumber());
    }

    @Test
    void testUpdateWithOwnJobNumberAllowed() throws Exception {
        // 先给 test_admin 设工号 EMP010
        mockMvc.perform(withCsrfToken(mockMvc, put("/employee")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L)
                .with(request -> {
                    request.setAttribute("employeeId", 990001L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":990001,\"name\":\"管理员\",\"jobNumber\":\"EMP010\"}")))
                .andExpect(jsonPath("$.code").value(1));

        // 再次提交自己原工号，excludeId 排除自身不应误报冲突
        mockMvc.perform(withCsrfToken(mockMvc, put("/employee")
                .sessionAttr("employee", 990001L)
                .sessionAttr("tenantId", 999L)
                .with(request -> {
                    request.setAttribute("employeeId", 990001L);
                    request.setAttribute("roleKey", "SUPER_ADMIN");
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":990001,\"name\":\"管理员\",\"jobNumber\":\"EMP010\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testSameJobNumberAllowedAcrossTenants() throws Exception {
        // 直接落库：租户999与租户2各有一个工号 EMP020，复合唯一域为租户内，应共存不冲突
        prepareEmployeeWithJobNumber(2L, "empA", "EMP020");
        Employee other = new Employee();
        other.setId(60L);
        other.setUsername("empB");
        other.setName("他租户");
        other.setPassword(PasswordUtils.encodePassword("123456"));
        other.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        other.setPhone("13500135060");
        other.setStatus(1);
        other.setSex("1");
        other.setRole(0);
        other.setTenantId(2L);
        other.setJobNumber("EMP020");
        employeeService.save(other);

        org.junit.jupiter.api.Assertions.assertEquals(
                "EMP020", employeeService.getById(60L).getJobNumber());
    }
}




