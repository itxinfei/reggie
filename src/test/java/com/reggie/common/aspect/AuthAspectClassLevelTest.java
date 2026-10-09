package com.reggie.common.aspect;

import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.annotation.RequiresAdmin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 类级安全注解鉴权回归测试（P0 漏洞修复守护）。
 *
 * <p>修复前：三个切面的切点只写 {@code @annotation(...)}，仅命中方法级注解；
 * 类级 {@code @RequireEmployee}/{@code @RequiresAdmin} 的 Controller 不会被切面拦截，
 * 顾客登录后可越权访问后台/系统管理接口。本测试用两个类级注解的测试 Controller 直接验证切面已在类级生效。</p>
 *
 * <p>使用 {@code @WebMvcTest} 仅加载 Web 切片 + 显式导入的两个切面，避免依赖 MySQL/Redis，
 * 直接断言「类级注解即触发鉴权」这一关键不变量。</p>
 *
 * @author SoftwareArchitect
 * @since 2026-08-13
 */
@WebMvcTest(controllers = {EmployeeTestController.class, AdminTestController.class})
@Import({EmployeeGuardAspect.class, AdminGuardAspect.class})
@EnableAspectJAutoProxy(proxyTargetClass = true)
class AuthAspectClassLevelTest {

    @Autowired
    private MockMvc mockMvc;

    // ===== @RequireEmployee 类级：必须拒绝顾客会话，放行员工会话 =====

    @Test
    void employeeClassLevel_rejectsCustomerSession() throws Exception {
        // 顾客会话既无 request.employeeId，也无 session.employee -> 切面应拒绝（code=0）
        mockMvc.perform(get("/__test/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void employeeClassLevel_allowsEmployeeSession() throws Exception {
        // 员工会话：LoginCheckFilter 在真实环境写入 session.employee，切面兜底读取该属性
        mockMvc.perform(get("/__test/employee").sessionAttr("employee", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    // ===== @RequiresAdmin 类级：仅超级管理员可访问 =====

    @Test
    void adminClassLevel_rejectsNoEmployee() throws Exception {
        // 无员工会话 -> 切面应拒绝
        mockMvc.perform(get("/__test/admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void adminClassLevel_rejectsNonAdmin() throws Exception {
        // 普通员工（非 SUPER_ADMIN）-> 切面应拒绝
        mockMvc.perform(get("/__test/admin")
                        .requestAttr("employeeId", 1L)
                        .requestAttr("roleKey", "STORE_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void adminClassLevel_allowsSuperAdmin() throws Exception {
        // 超级管理员 -> 切面放行
        mockMvc.perform(get("/__test/admin")
                        .requestAttr("employeeId", 1L)
                        .requestAttr("roleKey", "SUPER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}

// ===== 类级 @RequireEmployee 测试 Controller（顶层包级类，确保被 @WebMvcTest 注册）=====
@RestController
@RequireEmployee
class EmployeeTestController {
    @GetMapping("/__test/employee")
    public R<String> emp() {
        return R.success("ok");
    }
}

// ===== 类级 @RequiresAdmin 测试 Controller =====
@RestController
@RequiresAdmin
class AdminTestController {
    @GetMapping("/__test/admin")
    public R<String> admin() {
        return R.success("ok");
    }
}
