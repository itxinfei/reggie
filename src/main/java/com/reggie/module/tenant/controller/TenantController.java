package com.reggie.module.tenant.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.SecurityConstants;
import com.reggie.common.CustomException;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.tenant.dto.TenantRegisterDTO;
import com.reggie.module.tenant.model.Tenant;
import com.reggie.module.tenant.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.Objects;

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
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "租户注册", description = "注册新租户并创建管理员账号")
    @Parameter(name = "tenant", description = "租户信息", required = true)
    @Parameter(name = "username", description = "管理员用户名", required = true)
    @Parameter(name = "password", description = "管理员密码", required = true)
    @Parameter(name = "phone", description = "手机号", required = true)
    @Parameter(name = "verifyCode", description = "短信验证码", required = true)
    public R<String> register(@Valid @RequestBody TenantRegisterDTO dto,
                              @RequestParam("username") String username,
                              @RequestParam("password") String password,
                              @RequestParam("phone") String phone,
                              @RequestParam(required = false, name = "verifyCode") String verifyCode,
                              HttpSession session) {
        // 校验手机号格式
        if (phone == null || !phone.matches(SecurityConstants.PHONE_PATTERN)) {
            return R.error("手机号格式不正确");
        }

        // 校验验证码
        if (verifyCode == null || verifyCode.isEmpty()) {
            return R.error("验证码不能为空");
        }

        // DTO 转换为 Tenant 实体，设置默认状态为正常
        Tenant tenant = new Tenant();
        tenant.setName(dto.getShopName());
        tenant.setPhone(dto.getPhone());
        tenant.setAddress(dto.getAddress());
        tenant.setStatus(1);

        try {
            tenantService.registerWithAdmin(tenant, username, password, phone, verifyCode, session);
            return R.success("注册成功");
        } catch (CustomException e) {
            log.warn("租户注册失败：{}", e.getMessage(), e);
            return R.error("注册失败：" + e.getMessage());
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.error("租户注册异常", e);
            return R.error("注册失败，请稍后重试");
        }
    }

    /**
     * 分页查询租户（平台侧，超管可查看全部租户，tenant 表不走租户隔离）
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param name 租户名称（模糊，可选）
     * @param status 状态（可选）
     * @return 租户分页
     */
    @GetMapping("/page")
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "分页查询租户", description = "支持按名称模糊、状态筛选")
    public R<Page<Tenant>> page(@RequestParam(defaultValue = "1") @Min(1) int page,
                                @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                                @RequestParam(required = false) String name,
                                @RequestParam(required = false) Integer status) {
        Page<Tenant> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Tenant> qw = new LambdaQueryWrapper<>();
        qw.like(name != null && !name.isEmpty(), Tenant::getName, name);
        qw.eq(status != null, Tenant::getStatus, status);
        qw.orderByDesc(Tenant::getCreateTime);
        tenantService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 查询租户详情
     *
     * @param id 租户ID
     * @return 租户信息
     */
    @GetMapping("/{id}")
    @RequiresAdmin
    @Operation(summary = "查询租户详情", description = "根据ID查询租户完整信息")
    @Parameter(name = "id", description = "租户ID", required = true)
    public R<Tenant> getInfo(@PathVariable Long id) {
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) {
            return R.error("租户不存在");
        }
        return R.success(tenant);
    }

    /**
     * 编辑租户基本信息与套餐（不含状态，状态走专用接口）。
     * 仅回写白名单字段，防止越权改状态/密码类型。
     *
     * @param tenant 租户信息（需携带 id）
     * @return 操作结果
     */
    @PutMapping
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "编辑租户", description = "编辑租户联系信息、Logo、资质与套餐，不含状态")
    public R<String> update(@RequestBody Tenant tenant) {
        if (tenant == null || tenant.getId() == null) {
            return R.error("租户ID不能为空");
        }
        Tenant existing = tenantService.getById(tenant.getId());
        if (existing == null) {
            return R.error("租户不存在");
        }
        Tenant toUpdate = new Tenant();
        toUpdate.setId(existing.getId());
        if (tenant.getName() != null) {
            toUpdate.setName(tenant.getName());
        }
        if (tenant.getPhone() != null) {
            toUpdate.setPhone(tenant.getPhone());
        }
        if (tenant.getAddress() != null) {
            toUpdate.setAddress(tenant.getAddress());
        }
        if (tenant.getContact() != null) {
            toUpdate.setContact(tenant.getContact());
        }
        if (tenant.getLogo() != null) {
            toUpdate.setLogo(tenant.getLogo());
        }
        if (tenant.getLicenseImage() != null) {
            toUpdate.setLicenseImage(tenant.getLicenseImage());
        }
        if (tenant.getPackageName() != null) {
            toUpdate.setPackageName(tenant.getPackageName());
        }
        if (tenant.getExpireTime() != null) {
            toUpdate.setExpireTime(tenant.getExpireTime());
        }
        boolean ok = tenantService.updateById(toUpdate);
        if (!ok) {
            return R.error("修改失败，请刷新后重试");
        }
        return R.success("修改成功");
    }

    /**
     * 启用/禁用租户。禁止禁用当前登录账号所属租户，避免自锁无法再登录。
     *
     * @param id 租户ID
     * @param status 目标状态：0=禁用，1=启用
     * @return 操作结果
     */
    @PutMapping("/status")
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "启用/禁用租户", description = "切换租户状态，不能禁用自己所属租户")
    public R<String> updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return R.error("状态值非法，仅支持 0=禁用 或 1=启用");
        }
        Tenant existing = tenantService.getById(id);
        if (existing == null) {
            return R.error("租户不存在");
        }
        if (status == 0 && Objects.equals(BaseContext.getCurrentTenantId(), id)) {
            return R.error("不能禁用当前登录账号所属租户");
        }
        Tenant toUpdate = new Tenant();
        toUpdate.setId(id);
        toUpdate.setStatus(status);
        boolean ok = tenantService.updateById(toUpdate);
        if (!ok) {
            return R.error("状态更新失败，请刷新后重试");
        }
        return R.success(status == 1 ? "已启用" : "已禁用");
    }
}

