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
 * 方法级安全注解鉴权回归测试（与 {@link AuthAspectClassLevelTest} 互补）。
 *
 * <p>修复 P0 时切点由纯 {@code @annotation(...)} 改为 {@code @annotation(...) || @within(...)}。
 * 本测试锁定两个不变量：</p>
 * <ol>
 *   <li><b>方法级注解分支仍生效</b>：仅方法上标注 {@code @RequireEmployee}/{@code @RequiresAdmin} 时，
 *       切面按方法拦截；未标注的方法（公开端点）不被类级 {@code @within} 误伤。</li>
 *   <li><b>类级 + 方法级共存时行为正确</b>：类级注解作用于全部方法（含未标注方法），
 *       方法上的更严格注解（如 {@code @RequiresAdmin}）按方法覆盖。</li>
 * </ol>
 *
 * <p>使用 {@code @WebMvcTest} 仅加载 Web 切片 + 显式导入切面，避免依赖 MySQL/Redis。</p>
 *
 * @author SoftwareArchitect
 * @since 2026-08-13
 */
@WebMvcTest(controllers = {
        EmployeeMethodController.class,
        AdminMethodController.class,
        MixedController.class
})
@Import({EmployeeGuardAspect.class, AdminGuardAspect.class})
@EnableAspectJAutoProxy(proxyTargetClass = true)
class AuthAspectMethodLevelTest {

    @Autowired
    private MockMvc mockMvc;

    // ===== 仅方法级 @RequireEmployee：标注方法拦截，未标注方法保持公开 =====

    @Test
    void methodLevelEmployee_blocksCustomerOnAnnotatedMethod() throws Exception {
        // 顾客会话无 employeeId -> 标注方法应被拒绝（code=0）
        mockMvc.perform(get("/__m/emp-secured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void methodLevelEmployee_allowsEmployeeOnAnnotatedMethod() throws Exception {
        // 员工会话 -> 标注方法放行（code=1）
        mockMvc.perform(get("/__m/emp-secured").sessionAttr("employee", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void methodLevelEmployee_keepsUnannotatedMethodPublic() throws Exception {
        // 未标注方法：顾客会话也应放行（code=1），证明 @within 未误伤公开端点
        mockMvc.perform(get("/__m/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    // ===== 仅方法级 @RequiresAdmin：按方法拦截 =====

    @Test
    void methodLevelAdmin_blocksEmployeeOnAnnotatedMethod() throws Exception {
        // 普通员工访问标注 @RequiresAdmin 的方法 -> 拒绝（code=0）
        mockMvc.perform(get("/__m/admin-secured")
                        .requestAttr("employeeId", 1L)
                        .requestAttr("roleKey", "STORE_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void methodLevelAdmin_allowsSuperAdminOnAnnotatedMethod() throws Exception {
        // 超级管理员访问标注 @RequiresAdmin 的方法 -> 放行（code=1）
        mockMvc.perform(get("/__m/admin-secured")
                        .requestAttr("employeeId", 1L)
                        .requestAttr("roleKey", "SUPER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    // ===== 类级 @RequireEmployee + 方法级 @RequiresAdmin 共存 =====

    @Test
    void mixed_classLevelBlocksCustomerEvenOnUnannotatedMethod() throws Exception {
        // 类级 @RequireEmployee 作用于全部方法：顾客访问未标注方法也应被拒绝（code=0）
        mockMvc.perform(get("/__m/mixed-public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void mixed_methodLevelAdminOverridesClassEmployee() throws Exception {
        // 方法级 @RequiresAdmin 覆盖类级 @RequireEmployee：普通员工被拒（code=0）
        mockMvc.perform(get("/__m/mixed-admin")
                        .requestAttr("employeeId", 1L)
                        .requestAttr("roleKey", "STORE_MANAGER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void mixed_superAdminPassesMethodLevelAdmin() throws Exception {
        // 超级管理员通过方法级 @RequiresAdmin（code=1）
        mockMvc.perform(get("/__m/mixed-admin")
                        .requestAttr("employeeId", 1L)
                        .requestAttr("roleKey", "SUPER_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}

// ===== 仅方法级 @RequireEmployee 的测试 Controller（无类级注解）=====
@RestController
class EmployeeMethodController {
    @RequireEmployee
    @GetMapping("/__m/emp-secured")
    public R<String> secured() {
        return R.success("ok");
    }

    @GetMapping("/__m/public")
    public R<String> open() {
        return R.success("ok");
    }
}

// ===== 仅方法级 @RequiresAdmin 的测试 Controller（无类级注解）=====
@RestController
class AdminMethodController {
    @RequiresAdmin
    @GetMapping("/__m/admin-secured")
    public R<String> secured() {
        return R.success("ok");
    }
}

// ===== 类级 @RequireEmployee + 方法级 @RequiresAdmin 共存的测试 Controller =====
@RestController
@RequireEmployee
class MixedController {
    @RequiresAdmin
    @GetMapping("/__m/mixed-admin")
    public R<String> adminOnly() {
        return R.success("ok");
    }

    @GetMapping("/__m/mixed-public")
    public R<String> open() {
        return R.success("ok");
    }
}
