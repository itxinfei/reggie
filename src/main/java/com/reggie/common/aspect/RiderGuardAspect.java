package com.reggie.common.aspect;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.annotation.RequireRider;
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
 * 骑手会话鉴权切面。拦截 {@link RequireRider} 标注的接口，仅放行骑手会话
 * （{@code LoginCheckFilter} 在骑手登录时写入 request 属性 riderId），
 * 拒绝员工、C 端顾客会话。
 * </p>
 *
 * @author reggie
 * @since 2026-09-23
 */
@Slf4j
@Aspect
@Component
public class RiderGuardAspect {

    // 同时拦截方法级(@annotation)与类级(@within)，类级注解才能命中（与员工切面同理）。
    /**
     * 校验 rider。
     * @param joinPoint 参数 joinPoint
     * @return 返回结果
     */
    @Around("@annotation(com.reggie.common.annotation.RequireRider) || " +
            "@within(com.reggie.common.annotation.RequireRider)")
    /**
     * 校验 rider。
     * @param joinPoint 参数 joinPoint
     * @return 返回结果
     */
    public Object checkRider(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return R.error("未登录或登录已过期");
        }
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);

        // riderId 仅在骑手登录时由 LoginCheckFilter 写入
        Long riderId = (Long) request.getAttribute("riderId");
        // 兜底：MockMvc 测试中 @WebFilter 不生效，从 session 属性 "rider" 获取
        if (riderId == null && session != null) {
            Object riderAttr = session.getAttribute("rider");
            if (riderAttr instanceof Long) {
                riderId = (Long) riderAttr;
            }
        }
        if (riderId == null) {
            log.warn("[骑手鉴权] 非骑手会话访问被拒绝：uri={}", request.getRequestURI());
            return R.error("无权限，请使用骑手账号登录");
        }

        // 将骑手身份写入上下文：真实环境 LoginCheckFilter 已设（同值幂等）；
        // MockMvc / 异步等 filter 未生效场景由此兜底，保证 Controller 能取到当前骑手与其租户
        BaseContext.setCurrentId(riderId);
        if (session != null && session.getAttribute("tenantId") instanceof Long) {
            BaseContext.setCurrentTenantId((Long) session.getAttribute("tenantId"));
        }
        return joinPoint.proceed();
    }
}
