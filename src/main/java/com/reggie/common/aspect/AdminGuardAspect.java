package com.reggie.common.aspect;

import com.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 超级管理员鉴权切面
 * 拦截 {@link com.reggie.common.annotation.RequiresAdmin} 标注的接口，
 * 仅允许超级管理员（roleKey=SUPER_ADMIN）访问，其余角色一律拒绝，防止系统管理类接口越权。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-20
 */
@Slf4j
@Aspect
@Component
public class AdminGuardAspect {

    /** 超级管理员角色标识，需与 PermissionAspect.ADMIN_ROLE_KEY 保持一致 */
    private static final String ADMIN_ROLE_KEY = "SUPER_ADMIN";

    @Around("@annotation(com.reggie.common.annotation.RequiresAdmin)")
    public Object checkAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return R.error("未登录或登录已过期");
        }
        HttpServletRequest request = attributes.getRequest();

        Long employeeId = (Long) request.getAttribute("employeeId");
        if (employeeId == null) {
            return R.error("未登录或登录已过期");
        }

        String roleKey = (String) request.getAttribute("roleKey");
        if (!ADMIN_ROLE_KEY.equals(roleKey)) {
            log.warn("[管理员鉴权] 非管理员访问受限接口被拒绝：employeeId={}, roleKey={}", employeeId, roleKey);
            return R.error("权限不足，仅超级管理员可操作");
        }

        return joinPoint.proceed();
    }
}
