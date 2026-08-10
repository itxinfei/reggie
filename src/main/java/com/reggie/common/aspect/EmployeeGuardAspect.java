package com.reggie.common.aspect;

import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * <p>
 * 员工会话鉴权切面
 * 拦截 {@link RequireEmployee} 标注的接口，仅放行 employee 会话
 * （{@code LoginCheckFilter} 在员工登录时写入 request 属性 employeeId），
 * 拒绝 C 端顾客会话（user 会话无 employeeId 属性），
 * 修复"顾客登录后可越权调用后台管理接口"这一越权根因。
 * </p>
 * <p>
 * 注意：本切面只校验"是否为员工会话"，不限制具体角色/权限。
 * 需要超级管理员或具体权限的接口，请改用 {@code @RequiresAdmin} / {@code @RequiresPermission}。
 * </p>
 *
 * @author reggie
 * @since 2026-07-20
 */
@Slf4j
@Aspect
@Component
public class EmployeeGuardAspect {

    @Around("@annotation(com.reggie.common.annotation.RequireEmployee)")
    public Object checkEmployee(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return R.error("未登录或登录已过期");
        }
        HttpServletRequest request = attributes.getRequest();

        // employeeId 仅在员工登录时由 LoginCheckFilter 写入；顾客会话（user）无此属性
        Long employeeId = (Long) request.getAttribute("employeeId");
        // 兜底：LoginCheckFilter 通过 @WebFilter 注册，在 MockMvc 测试中不生效，
        // 此时从 session 属性 "employee" 获取（测试通过 .sessionAttr("employee", id) 设置）
        if (employeeId == null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                Object empAttr = session.getAttribute("employee");
                if (empAttr instanceof Long) {
                    employeeId = (Long) empAttr;
                }
            }
        }
        if (employeeId == null) {
            log.warn("[员工鉴权] 非员工会话访问后台接口被拒绝：uri={}", request.getRequestURI());
            return R.error("无权限，请使用员工账号登录");
        }
        return joinPoint.proceed();
    }
}
