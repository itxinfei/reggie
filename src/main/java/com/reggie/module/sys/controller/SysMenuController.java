package com.reggie.module.sys.controller;

import com.reggie.common.R;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.sys.service.MenuAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

/**
 * 后台菜单权限控制器：返回当前登录员工的角色与权限，供前端按角色过滤菜单。
 * <p>
 * 说明：菜单过滤仅为 UX 收敛，真正的越权拦截由后端
 * {@code PermissionAspect} / {@code @RequiresAdmin} / {@code @RequireEmployee} 保证；
 * 两者共用 {@link MenuAccessService} 作为权限数据源，避免口径不一致。
 * </p>
 *
 * @author reggie
 * @since 2026-09-28
 */
@Slf4j
@RestController
@RequestMapping("/sys/menu")
@Tag(name = "系统管理-菜单权限", description = "当前员工角色与权限（后台菜单按角色过滤）")
public class SysMenuController {

    @Autowired
    private MenuAccessService menuAccessService;

    /**
     * 当前登录员工的菜单访问信息。
     *
     * @param request 当前请求（读取过滤器写入的 employeeId / roleKey）
     * @return roleKey / isSuperAdmin / permissions
     */
    @GetMapping("/access")
    @RequireEmployee
    @Operation(summary = "当前员工菜单权限", description = "返回角色标识、是否超管与权限 key 列表，供后台菜单按角色过滤")
    public R<Map<String, Object>> access(HttpServletRequest request) {
        Long employeeId = (Long) request.getAttribute("employeeId");
        String roleKey = (String) request.getAttribute("roleKey");
        if (employeeId == null) {
            // 未登录：返回空权限（前端据此不展示受限菜单，登录页由拦截器跳转）
            return R.success(Collections.emptyMap());
        }
        Map<String, Object> data = menuAccessService.myAccess(employeeId, roleKey);
        log.info("[菜单权限] employeeId={}, roleKey={}, permCount={}",
                employeeId, roleKey, ((java.util.Set<?>) data.get("permissions")).size());
        return R.success(data);
    }
}
