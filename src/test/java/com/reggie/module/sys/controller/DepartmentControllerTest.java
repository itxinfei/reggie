package com.reggie.module.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.controller.BaseControllerTest;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.auth.service.EmployeeService;
import com.reggie.module.sys.dto.DepartmentSaveDTO;
import com.reggie.module.sys.model.Department;
import com.reggie.module.sys.service.DepartmentService;
import com.reggie.test.TestDatabaseCleaner;
import com.reggie.test.TestIdGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 部门管理 Controller 测试（企业组织架构）
 * <p>鉴权走 request.setAttribute("roleKey", "SUPER_ADMIN") 的超管旁路（与 RoleControllerTest 一致）；
 * 数据挂测试租户 999，@Sql 建表 + TestDatabaseCleaner 按租户清理。</p>
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(scripts = "classpath:schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public class DepartmentControllerTest extends BaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private TestIdGuard testIdGuard;

    private static final String ADMIN_BYPASS_ATTRIBUTE = "SUPER_ADMIN";

    /** 显式主键的测试员工（走 TestIdGuard 防撞库）；部门表自增主键，不硬编码 */
    private static final long TEST_EMP_ID = 990201L;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("department", "employee");
        testIdGuard.assertAbsent("employee", TEST_EMP_ID);
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(999L);
    }

    @Test
    @DisplayName("1. 新增部门 - 成功（挂当前租户 999）")
    void testAdd_success() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/sys/department"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        })
                        .content("{\"name\":\"技术部\",\"code\":\"TECH\",\"sort\":1,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Department dept = departmentService.getOne(
                new LambdaQueryWrapper<Department>().eq(Department::getName, "技术部"));
        assertNotNull(dept, "新增后应能按名称查到部门");
        assertEquals(999L, dept.getTenantId(), "租户应由服务端强制为当前租户");
        assertEquals("TECH", dept.getCode());
    }

    @Test
    @DisplayName("2. 新增部门 - 同租户重名被拒")
    void testAdd_duplicateName() throws Exception {
        departmentService.addTenantDepartment(deptSaveRequest("市场部", "MKT"));

        mockMvc.perform(withCsrfToken(mockMvc, post("/sys/department"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        })
                        .content("{\"name\":\"市场部\"}"))
                // 业务拒绝由全局异常处理映射为 422，body 为 R.error(msg)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").isString());
    }

    @Test
    @DisplayName("3. 新增部门 - 名称缺失被校验拦截")
    void testAdd_blankName() throws Exception {
        mockMvc.perform(withCsrfToken(mockMvc, post("/sys/department"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        })
                        .content("{\"code\":\"NO_NAME\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("4. 分页查询 - 只返回当前租户数据")
    void testPage_tenantIsolation() throws Exception {
        departmentService.addTenantDepartment(deptSaveRequest("技术部", "TECH"));
        departmentService.addTenantDepartment(deptSaveRequest("市场部", "MKT"));

        mockMvc.perform(get("/sys/department/page")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    @DisplayName("5. 修改部门 - 成功更新业务字段")
    void testUpdate_success() throws Exception {
        Department dept = insertDept("技术部", "TECH");

        mockMvc.perform(withCsrfToken(mockMvc, put("/sys/department"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        })
                        .content("{\"id\":" + dept.getId() + ",\"name\":\"研发中心\",\"leaderName\":\"张三\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Department updated = departmentService.getById(dept.getId());
        assertEquals("研发中心", updated.getName());
        assertEquals("张三", updated.getLeaderName());
    }

    @Test
    @DisplayName("6. 删除部门 - 部门下有员工时拒绝")
    void testDelete_blockedByEmployee() throws Exception {
        Department dept = insertDept("技术部", "TECH");
        insertEmployeeUnder(dept.getId());

        mockMvc.perform(withCsrfToken(mockMvc, delete("/sys/department/" + dept.getId()))
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        }))
                // 删除守卫的业务拒绝 → 422，msg 中说明仍有员工
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("员工")));

        assertNotNull(departmentService.getById(dept.getId()), "删除被拒后部门应仍在");
    }

    @Test
    @DisplayName("7. 删除部门 - 无员工挂靠时逻辑删除成功")
    void testDelete_success() throws Exception {
        Department dept = insertDept("行政部", "HR");

        mockMvc.perform(withCsrfToken(mockMvc, delete("/sys/department/" + dept.getId()))
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            request.setAttribute("roleKey", ADMIN_BYPASS_ATTRIBUTE);
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        assertNull(departmentService.getById(dept.getId()), "逻辑删除后 getById 应为 null");
    }

    @Test
    @DisplayName("8. 非超管访问被拒")
    void testAccess_deniedWithoutAdmin() throws Exception {
        mockMvc.perform(get("/sys/department/page")
                        .param("page", "1")
                        .param("pageSize", "10")
                        .with(request -> {
                            request.setAttribute("employeeId", 1L);
                            return request;
                        }))
                // 非超管走鉴权拦截，同样以错误 body 返回
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ==================== 辅助 ====================

    private DepartmentSaveDTO deptSaveRequest(String name, String code) {
        DepartmentSaveDTO dto = new DepartmentSaveDTO();
        dto.setName(name);
        dto.setCode(code);
        dto.setSort(1);
        dto.setStatus(1);
        return dto;
    }

    private Department insertDept(String name, String code) {
        departmentService.addTenantDepartment(deptSaveRequest(name, code));
        Department dept = departmentService.getOne(
                new LambdaQueryWrapper<Department>().eq(Department::getName, name));
        assertNotNull(dept, "部门应已落库: " + name);
        return dept;
    }

    /** 落库一个挂指定部门的测试员工（显式主键，setUp 已做防撞库断言） */
    private void insertEmployeeUnder(Long departmentId) {
        Employee emp = new Employee();
        emp.setId(TEST_EMP_ID);
        emp.setUsername("dept_test_emp");
        emp.setName("部门测试员工");
        emp.setPassword("x");
        emp.setStatus(1);
        emp.setTenantId(999L);
        emp.setDepartmentId(departmentId);
        emp.setCreateTime(LocalDateTime.now());
        emp.setUpdateTime(LocalDateTime.now());
        assertTrue(employeeService.save(emp), "测试员工应落库成功");
    }
}
