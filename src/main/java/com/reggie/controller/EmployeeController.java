package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.PasswordUtils;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.common.SecurityConstants;
import com.reggie.entity.Employee;
import com.reggie.enums.UserStatus;
import com.reggie.service.EmployeeService;
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
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/employee")
@Tag(name = "员工管理", description = "员工CRUD及登录接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "员工登录", description = "员工账号密码登录")
    @Parameter(name = "employee", description = "员工登录信息", required = true)
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.IP)
    public R<Employee> login(HttpServletRequest request,@RequestBody Employee employee){

        //1、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //2、如果没有查询到则返回登录失败结果
        if (emp == null) {
            return R.error("用户名或密码错误");
        }

        //3、密码校验（支持MD5和BCrypt）
        String rawPassword = employee.getPassword();
        String encodedPassword = emp.getPassword();
        String passwordType = emp.getPasswordType() != null ? emp.getPasswordType() : SecurityConstants.PASSWORD_TYPE_MD5;

        boolean passwordMatches = PasswordUtils.matches(rawPassword, encodedPassword, passwordType);

        if (!passwordMatches) {
            return R.error("用户名或密码错误");
        }

        //4、密码类型升级（如果是MD5且校验通过，自动升级为BCrypt）
        if (SecurityConstants.PASSWORD_TYPE_MD5.equals(passwordType)) {
            String newEncoded = PasswordUtils.upgradeIfNeeded(rawPassword, encodedPassword, passwordType);
            if (newEncoded != null) {
                emp.setPassword(newEncoded);
                emp.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
                employeeService.updateById(emp);
            }
        }

        //5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if (emp.getStatus() != null && emp.getStatus() == UserStatus.DISABLED.getValue()) {
            return R.error("账号已禁用");
        }

        //6、登录成功，将员工id和租户id存入Session并返回登录成功结果
        BaseContext.setCurrentTenantId(employee.getTenantId());
        request.getSession().setAttribute("employee", emp.getId());
        request.getSession().setAttribute("tenantId", employee.getTenantId());
        return R.success(emp);
    }

    /**
     * 员工退出
     * @param request
     * @return
     */
    @PostMapping("/logout")
    @Operation(summary = "员工退出", description = "退出当前登录账号")
    public R<String> logout(HttpServletRequest request){
        request.getSession().removeAttribute("employee");
        request.getSession().removeAttribute("tenantId");
        return R.success("退出成功");
    }

    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping
    @Operation(summary = "新增员工", description = "创建新的员工账号")
    @Parameter(name = "employee", description = "员工信息", required = true)
    public R<String> save(HttpServletRequest request,@Valid @RequestBody Employee employee){
        log.info("新增员工，员工信息：手机号={}，身份证号={}",
            LogMaskUtils.maskPhone(employee.getPhone()),
            LogMaskUtils.maskIdCard(employee.getIdNumber()));

        // 设置初始密码（使用BCrypt加密）
        employee.setPassword(PasswordUtils.encodePassword(SecurityConstants.DEFAULT_PASSWORD));
        employee.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);

        //employee.setCreateTime(LocalDateTime.now());
        //employee.setUpdateTime(LocalDateTime.now());

        //获得当前登录用户的id
        //Long empId = (Long) request.getSession().getAttribute("employee");

        //employee.setCreateUser(empId);
        //employee.setUpdateUser(empId);

        employee.setTenantId(BaseContext.getCurrentTenantId());

        employeeService.save(employee);

        return R.success("新增员工成功");
    }

    /**
     * 员工信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "员工分页查询", description = "分页查询员工列表")
    @Parameter(name = "page", description = "页码", required = true)
    @Parameter(name = "pageSize", description = "每页数量", required = true)
    @Parameter(name = "name", description = "员工姓名（可选）")
    public R<Page<Employee>> page(int page,int pageSize,String name){
        log.info("page = {},pageSize = {},name = {}" ,page,pageSize,name);

        //构造分页构造器
        Page<Employee> pageInfo = new Page<>(page, pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null && !name.isEmpty(),Employee::getName,name);
        //添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);

        //手动添加租户过滤（employee表在忽略列表中）
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null) {
            queryWrapper.eq(Employee::getTenantId, currentTenantId);
        }

        //执行查询
        employeeService.page(pageInfo,queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 根据id修改员工信息
     * @param employee
     * @return
     */
    @PutMapping
    @Operation(summary = "修改员工信息", description = "根据ID更新员工信息")
    @Parameter(name = "employee", description = "员工信息", required = true)
    public R<String> update(HttpServletRequest request,@RequestBody Employee employee){
        log.info("修改员工信息，手机号={}，身份证号={}",
            LogMaskUtils.maskPhone(employee.getPhone()),
            LogMaskUtils.maskIdCard(employee.getIdNumber()));

        long id = Thread.currentThread().getId();
        log.info("线程id为：{}",id);
        //Long empId = (Long)request.getSession().getAttribute("employee");
        //employee.setUpdateTime(LocalDateTime.now());
        //employee.setUpdateUser(empId);
        employeeService.updateById(employee);

        return R.success("员工信息修改成功");
    }

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询员工信息", description = "根据ID查询员工详情")
    @Parameter(name = "id", description = "员工ID", required = true)
    public R<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息...");
        Employee employee = employeeService.getById(id);
        if(employee != null){
            return R.success(employee);
        }
        return R.error("没有查询到对应员工信息");
    }
}
