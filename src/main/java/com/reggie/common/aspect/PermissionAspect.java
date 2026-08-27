package com.reggie.common.aspect;

import com.reggie.common.R;
import com.reggie.common.BaseContext;
import com.reggie.common.annotation.RequiresPermission;
import com.reggie.module.sys.model.Role;
import com.reggie.module.sys.mapper.RoleMapper;
import com.reggie.module.sys.service.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 权限校验切面
 * </p>
 * <p>
 * 拦截带有 {@link RequiresPermission} 注解的方法，校验当前用户是否具有所需权限。
 * 权限数据优先从 Redis 缓存获取，缓存未命中时从数据库加载并回填缓存。
 * 超级管理员（roleKey=admin）直接放行，无需权限校验。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private RoleMapper roleMapper;

    private static final String PERMISSION_PREFIX = "sys:employee:permissions:";

    /** 缓存过期时间（小时） */
    private static final long CACHE_TTL_HOURS = 1;

    /**
     * 超级管理员角色标识（直接放行）
     */
    // 修改点：与 EmployeeController.resolveRoleKey 对齐，登录管理员角色标识已改为 SUPER_ADMIN
    private static final String ADMIN_ROLE_KEY = "SUPER_ADMIN";

    // 同时拦截方法级(@annotation)与类级(@within) @RequiresPermission，
    // 与下方 getAnnotation() 已支持类级读取保持一致，避免类级注解失效导致越权。
    @Around("@annotation(com.reggie.common.annotation.RequiresPermission) || @within(com.reggie.common.annotation.RequiresPermission)")
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
        Set<String> userPermissions = getUserPermissions(employeeId, roleKey);
        if (userPermissions.isEmpty()) {
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
     * 获取用户权限集合
     * <p>
     * 修改点：优先级：Redis缓存 → 数据库查询（含缓存回填）→ 空集合（安全降级）
     * 不再返回ADMIN_PERMISSIONS，防止缓存未命中导致越权
     *
     * @param employeeId 员工ID
     * @param roleKey    角色标识（admin/manager）
     * @return 权限Key集合，异常时返回空集合
     */
    private Set<String> getUserPermissions(Long employeeId, String roleKey) {
        // 1. 优先从 Redis 缓存获取
        if (redisTemplate != null) {
            try {
                String cacheKey = PERMISSION_PREFIX + employeeId;
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached instanceof Set) {
                    return (Set<String>) cached;
                }
                if (cached instanceof List) {
                    return new HashSet<>((List<String>) cached);
                }
            } catch (Exception e) {
                log.warn("[权限缓存] 读取缓存失败，降级查数据库：employeeId={}", employeeId, e);
            }
        }

        // 2. 缓存未命中，从数据库加载
        Set<String> permissions = loadPermissionsFromDb(employeeId, roleKey);

        // 3. 回填缓存（包含空集合，防止缓存穿透）
        if (redisTemplate != null) {
            try {
                String cacheKey = PERMISSION_PREFIX + employeeId;
                redisTemplate.opsForValue().set(cacheKey, permissions, CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("[权限缓存] 回填缓存失败：employeeId={}", employeeId, e);
            }
        }

        return permissions;
    }

    /**
     * 从数据库加载用户权限
     * <p>
     * 修改点：根据 roleKey 查询角色ID，再查权限列表
     *
     * @param employeeId 员工ID
     * @param roleKey    角色标识
     * @return 权限Key集合，异常时返回空集合（安全降级，不放行）
     */
    private Set<String> loadPermissionsFromDb(Long employeeId, String roleKey) {
        try {
            Role role = roleMapper.findByRoleKeyAndTenantId(BaseContext.getCurrentTenantId(), roleKey);
            if (role == null) {
                log.warn("[权限加载] 未找到角色：roleKey={}, employeeId={}", roleKey, employeeId);
                return Collections.emptySet();
            }

            List<String> permKeys = permissionService.getPermissionKeysByRoleIds(
                    Collections.singletonList(role.getId()));
            if (permKeys == null || permKeys.isEmpty()) {
                log.warn("[权限加载] 角色无权限：roleId={}, roleKey={}, employeeId={}",
                        role.getId(), roleKey, employeeId);
                return Collections.emptySet();
            }

            log.info("[权限加载] 数据库加载成功：employeeId={}, roleKey={}, permCount={}",
                    employeeId, roleKey, permKeys.size());
            return new HashSet<>(permKeys);
        } catch (Exception e) {
            log.error("[权限加载] 数据库查询异常：employeeId={}, roleKey={}, error={}",
                    employeeId, roleKey, e.getMessage(), e);
            // 安全降级：异常时返回空集合，不放行任何权限
            return Collections.emptySet();
        }
    }

    /**
     * 清除指定员工权限缓存
     * 供外部调用（权限变更时）
     *
     * @param employeeId 员工ID
     */
    public void clearEmployeePermissionCache(Long employeeId) {
        if (redisTemplate == null || employeeId == null) {
            return;
        }
        try {
            String cacheKey = PERMISSION_PREFIX + employeeId;
            redisTemplate.delete(cacheKey);
            log.info("[权限缓存] 已清除员工权限缓存：employeeId={}", employeeId);
        } catch (Exception e) {
            log.warn("[权限缓存] 清除缓存失败：employeeId={}", employeeId, e);
        }
    }

    /**
     * 清除所有员工权限缓存
     * <p>修改点：角色权限发生变更（分配/删除角色）后调用，
     * 由于无法快速定位拥有该角色的员工，统一清理全部员工权限缓存，
     * 使其在下一次访问时按最新权限重新加载。</p>
     */
    public void clearAllEmployeePermissionCache() {
        if (redisTemplate == null) {
            return;
        }
        try {
            String pattern = PERMISSION_PREFIX + "*";
            Set<String> keys = redisTemplate.execute(
                (org.springframework.data.redis.core.RedisCallback<Set<String>>) connection -> {
                    Set<String> result = new HashSet<>();
                    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
                    Cursor<byte[]> cursor = connection.scan(options);
                    try {
                        while (cursor.hasNext()) {
                            result.add(new String(cursor.next()));
                        }
                    } finally {
                        try {
                            cursor.close();
                        } catch (IOException ex) {
                            log.warn("[权限缓存] cursor 关闭异常", ex);
                        }
                    }
                    return result;
                }
            );
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("[权限缓存] 已清除全部员工权限缓存，共{}条", keys.size());
            }
        } catch (Exception e) {
            log.warn("[权限缓存] 清除全部员工权限缓存失败：{}", e.getMessage(), e);
        }
    }
}

