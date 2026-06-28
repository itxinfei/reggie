package com.reggie.controller;

import com.reggie.common.R;
import com.reggie.entity.Employee;
import com.reggie.entity.Tenant;
import com.reggie.service.EmployeeService;
import com.reggie.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Slf4j
@RestController
@RequestMapping("/tenant")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private EmployeeService employeeService;

    @PostMapping("/register")
    public R<String> register(@RequestBody Tenant tenant, String username, String password, HttpSession session) {
        tenantService.save(tenant);

        Employee employee = new Employee();
        employee.setUsername(username);
        employee.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));
        employee.setName(tenant.getName());
        employee.setPhone(tenant.getPhone());
        employee.setStatus(1);
        employee.setTenantId(tenant.getId());
        employeeService.save(employee);

        session.setAttribute("employee", employee.getId());
        session.setAttribute("tenantId", tenant.getId());

        return R.success("注册成功");
    }
}
