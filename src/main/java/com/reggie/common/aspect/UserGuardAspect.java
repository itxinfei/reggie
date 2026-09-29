package com.reggie.common.aspect;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireUser;
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
 * 顾客（C 端用户）会话鉴权切面。拦截 {@link RequireUser} 标注的接口，仅放行顾客会话
 * （{@code LoginCheckFilter} 在顾客登录时写入 request 属性 userId / session 属性 user），
 * 拒绝员工、骑手会话。
 *
 * <p>与 {@link RiderGuardAspect} 同理：生产环境 {@code LoginCheckFilter} 已写入上下文（同值幂等），
 * 本切面在其未生效的 MockMvc / 异步等场景下兜底，保证 Controller 能取到当前用户与其租户。</p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Slf4j
@Aspect
@Component
public class UserGuardAspect {

    @Around("@annotation(com.reggie.common.annotation.RequireUser) || " +
            "@within(com.reggie.common.annotation.RequireUser)")
    public Object checkUser(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return R.error("未登录或登录已过期");
        }
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);

        // userId 由 LoginCheckFilter 在顾客登录时写入 request 属性；MockMvc 兜底从 session "user" 取
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null && session != null) {
            Object userAttr = session.getAttribute("user");
            if (userAttr instanceof Long) {
                userId = (Long) userAttr;
            }
        }
        if (userId == null) {
            log.warn("[顾客鉴权] 非顾客会话访问被拒绝：uri={}", request.getRequestURI());
            return R.error("请先登录");
        }

        BaseContext.setCurrentId(userId);
        if (session != null && session.getAttribute("tenantId") instanceof Long) {
            BaseContext.setCurrentTenantId((Long) session.getAttribute("tenantId"));
        }
        return joinPoint.proceed();
    }
}
