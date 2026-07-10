package com.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.Employee;

/**
 * 员工管理服务接口，提供员工账号的增删改查及登录鉴权功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface EmployeeService extends IService<Employee> {
}
