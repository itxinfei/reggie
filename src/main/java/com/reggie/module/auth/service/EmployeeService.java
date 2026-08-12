package com.reggie.module.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.auth.model.Employee;

/**
 * <p>
 * 员工管理服务接口，提供员工账号的增删改查及登录鉴权功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface EmployeeService extends IService<Employee> {

    /**
     * 根据用户名查询员工
     *
     * @param username 用户名
     * @return 员工信息，不存在则返回null
     */
    Employee getByUsername(String username);

    /**
     * 员工登录验证
     *
     * @param username 用户名
     * @param password 密码（密文）
     * @return 登录成功返回员工信息，失败返回null
     */
    Employee login(String username, String password);
}

