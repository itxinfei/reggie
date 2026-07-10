package com.reggie.common.aspect;

import com.reggie.common.R;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.common.BaseContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 权限校验切面
 * 拦截带有 @RequiresPermission 注解的方法，校验当前用户是否有权限
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PERMISSION_PREFIX = "sys:employee:permissions:";

    /**
     * 超级管理员角色标识（直接放行）
     */
    private static final String ADMIN_ROLE_KEY = "admin";

    /**
     * 超级管理员权限标识集合（所有权限）
     */
    private static final Set<String> ADMIN_PERMISSIONS = new HashSet<>(Arrays.asList(
            "system", "role:view", "role:add", "role:edit", "role:delete",
            "employee:view", "employee:add", "employee:edit", "employee:delete",
            "config:view", "config:edit", "template:view", "template:add",
            "log:view",
            "category:view", "category:add", "category:edit", "category:delete",
            "dish:view", "dish:add", "dish:edit", "dish:delete", "dish:enable",
            "setmeal:view", "setmeal:add", "setmeal:edit", "setmeal:delete",
            "order:view", "order:cancel", "order:deliver", "order:complete", "order:refund",
            "payment:view",
            "table:view", "table:add", "table:edit", "table:delete",
            "queue:view", "reservation:view",
            "material:view", "purchase:view", "stockcheck:view",
            "member:view", "points:view", "coupon:view", "user:view",
            "report:daily", "report:ranking", "report:payment", "report:timeslot",
            "recommend:view", "campaign:view", "campaign:push",
            "store:view", "store:create", "store:sync", "store:dashboard"
    ));

    @Around("@annotation(com.reggie.common.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresPermission annotation = getAnnotation(joinPoint);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 获取当前请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return R.error("未登录或登录已过期");
        }
        HttpServletRequest request = attributes.getRequest();

        // 获取当前员工信息
        Long employeeId = (Long) request.getAttribute("employeeId");
        if (employeeId == null) {
            return R.error("未登录或登录已过期");
        }

        // 超级管理员直接放行
        String roleKey = (String) request.getAttribute("roleKey");
        if (ADMIN_ROLE_KEY.equals(roleKey)) {
            return joinPoint.proceed();
        }

        // 获取用户权限列表
        Set<String> userPermissions = getUserPermissions(employeeId);
        if (userPermissions == null || userPermissions.isEmpty()) {
            log.warn("[权限拦截] 用户无权限: employeeId={}, required={}", employeeId, annotation.value());
            return R.error("权限不足");
        }

        // 校验权限
        String[] requiredPerms = annotation.value().split(",");
        boolean hasPermission;
        if (annotation.requireAll()) {
            // 需要所有权限
            hasPermission = Arrays.stream(requiredPerms)
                    .allMatch(p -> userPermissions.contains(p.trim()));
        } else {
            // 只需任一权限
            hasPermission = Arrays.stream(requiredPerms)
                    .anyMatch(p -> userPermissions.contains(p.trim()));
        }

        if (!hasPermission) {
            log.warn("[权限拦截] 权限不足: employeeId={}, roleKey={}, required={}, userPerms={}",
                    employeeId, roleKey, annotation.value(), userPermissions);
            return R.error("权限不足，请联系管理员");
        }

        return joinPoint.proceed();
    }

    /**
     * 获取方法上的 @RequiresPermission 注解
     */
    private RequiresPermission getAnnotation(ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            // 方法级注解
            RequiresPermission methodAnn = signature.getMethod().getAnnotation(RequiresPermission.class);
            if (methodAnn != null) {
                return methodAnn;
            }
            // 类级注解
            return joinPoint.getTarget().getClass().getAnnotation(RequiresPermission.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取用户权限集合（从Redis缓存读取）
     */
    private Set<String> getUserPermissions(Long employeeId) {
        String cacheKey = PERMISSION_PREFIX + employeeId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof Set) {
            return (Set<String>) cached;
        }
        if (cached instanceof List) {
            return new HashSet<>((List<String>) cached);
        }
        // 缓存未命中时，从数据库加载（简化：临时放行，后续由LoginCheckFilter预加载）
        return ADMIN_PERMISSIONS;
    }
}
