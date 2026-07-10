package com.reggie.controller;

import com.reggie.common.R;
import com.reggie.common.SecurityConstants;
import com.reggie.common.CustomException;
import com.reggie.entity.Tenant;
import com.reggie.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

/**
 * 租户管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/tenant")
@Tag(name = "租户管理", description = "租户注册接口")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    /**
     * 租户注册
     *
     * @param tenant 租户信息
     * @param username 管理员用户名
     * @param password 管理员密码
     * @param phone 手机号
     * @param verifyCode 短信验证码
     * @param session HTTP会话
     * @return 注册结果
     */
    @PostMapping("/register")
    @Operation(summary = "租户注册", description = "注册新租户并创建管理员账号")
    @Parameter(name = "tenant", description = "租户信息", required = true)
    @Parameter(name = "username", description = "管理员用户名", required = true)
    @Parameter(name = "password", description = "管理员密码", required = true)
    @Parameter(name = "phone", description = "手机号", required = true)
    @Parameter(name = "verifyCode", description = "短信验证码", required = true)
    public R<String> register(@Valid @RequestBody Tenant tenant,
                              String username,
                              String password,
                              String phone,
                              String verifyCode,
                              HttpSession session) {
        // 校验手机号格式
        if (phone == null || !phone.matches(SecurityConstants.PHONE_PATTERN)) {
            return R.error("手机号格式不正确");
        }

        // 校验验证码
        if (verifyCode == null || verifyCode.isEmpty()) {
            return R.error("验证码不能为空");
        }

        try {
            tenantService.registerWithAdmin(tenant, username, password, phone, verifyCode, session);
            return R.success("注册成功");
        } catch (CustomException e) {
            log.warn("租户注册失败：{}", e.getMessage());
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("租户注册异常", e);
            return R.error("注册失败，请稍后重试");
        }
    }
}
