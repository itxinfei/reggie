package com.reggie.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.PasswordUtils;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.auth.mapper.EmployeeMapper;
import com.reggie.module.auth.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 员工服务实现类
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, Employee> implements EmployeeService {

    @Override
    public Employee getByUsername(String username) {
        return this.list(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getUsername, username)
                .eq(Employee::getTenantId, BaseContext.getCurrentTenantId())
                .last("LIMIT 1"))
                .stream().findFirst().orElse(null);
    }

    @Override
    public Employee login(String username, String password) {
        Employee employee = this.getByUsername(username);
        if (employee == null) {
            log.warn("登录失败：用户不存在 - username={}", username);
            return null;
        }
        // 检查账号状态（0=禁用）
        if (employee.getStatus() != null && employee.getStatus() == 0) {
            log.warn("登录失败：账号已禁用 - username={}, empId={}", username, employee.getId());
            return null;
        }
        // 验证密码
        String passwordType = employee.getPasswordType() != null
                ? employee.getPasswordType() : PasswordUtils.PASSWORD_TYPE_BCRYPT;
        if (!PasswordUtils.matches(password, employee.getPassword(), passwordType)) {
            log.warn("登录失败：密码错误 - username={}, empId={}", username, employee.getId());
            return null;
        }
        log.info("登录成功 - username={}, empId={}", username, employee.getId());
        return employee;
    }
}


