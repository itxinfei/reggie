package com.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.common.PasswordUtils;
import com.reggie.common.SecurityConstants;
import com.reggie.common.VerifyCodeUtils;
import com.reggie.entity.Employee;
import com.reggie.entity.Tenant;
import com.reggie.enums.UserStatus;
import com.reggie.mapper.TenantMapper;
import com.reggie.service.EmployeeService;
import com.reggie.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * 租户服务实现类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
@Slf4j
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    /** 员工服务 */
    @Autowired
    private EmployeeService employeeService;

    /** 验证码工具 */
    @Autowired
    private VerifyCodeUtils verifyCodeUtils;

    /**
     * 注册新租户并创建管理员账号（事务保护）
     * 如果租户保存成功但管理员创建失败，会自动回滚
     *
     * 验证码存储：优先使用 Redis（支持集群），降级到 Session（单机）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerWithAdmin(Tenant tenant, String username, String password, String phone, String verifyCode, HttpSession session) {
        // 1. 校验验证码
        if (phone == null || verifyCode == null || verifyCode.isEmpty()) {
            throw new CustomException("请先获取短信验证码");
        }

        // 2. 使用统一工具类校验验证码（支持 Redis + Session）
        if (!verifyCodeUtils.verifyCode(phone, verifyCode, session)) {
            throw new CustomException("验证码错误或已过期");
        }

        // 3. 保存租户信息
        this.save(tenant);

        // 4. 创建租户管理员
        Employee employee = new Employee();
        employee.setUsername(username);
        employee.setPassword(PasswordUtils.encodePassword(password));
        employee.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        employee.setName(tenant.getName());
        employee.setPhone(tenant.getPhone());
        employee.setStatus(UserStatus.ENABLED.getValue());
        employee.setRole(1); // 租户管理员默认超级管理员角色
        employee.setTenantId(tenant.getId());
        employee.setSex("1");
        employee.setIdNumber("");

        employeeService.save(employee);

        log.info("租户注册成功：tenantId={}, username={}", tenant.getId(), username);
    }

    /**
     * 保存验证码（供 Controller 调用）
     *
     * @param phone      手机号
     * @param verifyCode 验证码
     * @param session    HTTP Session
     */
    public void saveVerifyCode(String phone, String verifyCode, HttpSession session) {
        verifyCodeUtils.saveVerifyCode(phone, verifyCode, session);
    }

    /**
     * 查询所有活跃租户（状态=1 正常）
     * 供定时任务等多租户批处理场景使用
     */
    @Override
    public List<Tenant> listActiveTenants() {
        return this.lambdaQuery()
                .eq(Tenant::getStatus, 1)
                .list();
    }
}
