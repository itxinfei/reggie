package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.BruteForceProtectionFilter;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.PasswordUtils;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.common.SecurityConstants;
import com.reggie.dto.auth.EmployeeLoginDTO;
import com.reggie.entity.Employee;
import com.reggie.enums.EmployeeRole;
import com.reggie.enums.UserStatus;
import com.reggie.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 员工管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/employee")
@Tag(name = "员工管理", description = "员工CRUD及登录接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired(required = false)
    private BruteForceProtectionFilter bruteForceProtectionFilter;

    /**
     * 员工登录
     * @param request HTTP请求对象
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "员工登录", description = "员工账号密码登录，支持验证码和防暴力破解保护")
    @Parameter(name = "loginDTO", description = "员工登录信息（用户名、密码）", required = true)
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.IP)
    public R<Map<String, Object>> login(HttpServletRequest request, @Valid @RequestBody EmployeeLoginDTO loginDTO) {

        // 检查账号是否被锁定（同时检查IP和用户名维度）
        if (bruteForceProtectionFilter != null
            && (bruteForceProtectionFilter.isAccountLocked(request)
                || bruteForceProtectionFilter.isAccountLocked(loginDTO.getUsername()))) {
            log.warn("账号已被锁定 - 用户名：{}",
                LogMaskUtils.maskUsername(loginDTO.getUsername()));
            return R.error("登录失败次数过多，请5分钟后重试");
        }

        //1、根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, loginDTO.getUsername());
        Employee emp = employeeService.getOne(queryWrapper);

        //2、如果没有查询到则返回登录失败结果
        if (emp == null) {
            recordLoginFailure(request, loginDTO.getUsername());
            return R.error("用户名或密码错误");
        }

        //3、密码校验（支持MD5和BCrypt）
        String rawPassword = loginDTO.getPassword();
        String encodedPassword = emp.getPassword();
        String passwordType = emp.getPasswordType() != null ? emp.getPasswordType() : SecurityConstants.PASSWORD_TYPE_MD5;

        boolean passwordMatches = PasswordUtils.matches(rawPassword, encodedPassword, passwordType);

        if (!passwordMatches) {
            recordLoginFailure(request, loginDTO.getUsername());
            return R.error("用户名或密码错误");
        }

        //4、密码类型升级（如果是MD5且校验通过，自动升级为BCrypt）
        if (SecurityConstants.PASSWORD_TYPE_MD5.equals(passwordType)) {
            String newEncoded = PasswordUtils.upgradeIfNeeded(rawPassword, encodedPassword, passwordType);
            if (newEncoded != null) {
                emp.setPassword(newEncoded);
                emp.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
                // 设置当前用户ID，用于MyBatis-Plus自动填充updateUser字段
                BaseContext.setCurrentId(emp.getId());
                employeeService.updateById(emp);
            }
        }

        //5、查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if (emp.getStatus() != null && UserStatus.DISABLED.getValue() == emp.getStatus().intValue()) {
            return R.error("账号已禁用");
        }

        //6、登录成功，重置失败计数
        if (bruteForceProtectionFilter != null) {
            bruteForceProtectionFilter.resetLoginAttempts(request);
        }

        //7、登录成功，将员工id和租户id存入Session
        // 注意：重新从数据库查询最新信息，确保租户上下文准确
        Employee freshEmp = employeeService.getById(emp.getId());
        Long sessionTenantId;
        String sessionRoleKey;
        if (freshEmp != null) {
            request.getSession().setAttribute("employee", freshEmp.getId());
            request.getSession().setAttribute("tenantId", freshEmp.getTenantId());
            sessionTenantId = freshEmp.getTenantId();
            sessionRoleKey = resolveRoleKey(freshEmp.getRole());
        } else {
            // 如果查询失败，使用原信息（降级处理）
            request.getSession().setAttribute("employee", emp.getId());
            request.getSession().setAttribute("tenantId", emp.getTenantId());
            sessionTenantId = emp.getTenantId();
            sessionRoleKey = resolveRoleKey(emp.getRole());
            log.warn("员工登录后无法刷新数据，使用内存中的租户信息可能已过期 - empId: {}", emp.getId());
        }

        request.getSession().setAttribute("roleKey", sessionRoleKey);

        // 返回脱敏后的登录信息（不包含密码等敏感字段）
        java.util.HashMap<String, Object> result = new java.util.HashMap<>();
        result.put("id", emp.getId());
        result.put("username", emp.getUsername());
        result.put("name", emp.getName());
        result.put("phone", emp.getPhone() != null ? maskPhone(emp.getPhone()) : null);
        result.put("status", emp.getStatus());
        result.put("role", emp.getRole());
        result.put("tenantId", emp.getTenantId());
        result.put("createTime", emp.getCreateTime());
        result.put("updateTime", emp.getUpdateTime());

        return R.success(result);
    }

    /**
     * 忘记密码 - 通过用户名+手机号验证后重置密码
     * @param params 包含 username, phone, newPassword
     * @return 操作结果
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "忘记密码", description = "通过用户名和手机号验证后重置密码")
    public R<String> forgotPassword(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String phone = params.get("phone");
        String newPassword = params.get("newPassword");

        if (username == null || username.trim().isEmpty()) {
            return R.error("请输入用户名");
        }
        if (phone == null || phone.trim().isEmpty()) {
            return R.error("请输入手机号");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return R.error("新密码至少6位");
        }

        LambdaQueryWrapper<Employee> qw = new LambdaQueryWrapper<>();
        qw.eq(Employee::getUsername, username.trim());
        qw.eq(Employee::getPhone, phone.trim());
        Employee emp = employeeService.getOne(qw);

        if (emp == null) {
            return R.error("用户名与手机号不匹配");
        }

        emp.setPassword(PasswordUtils.encodePassword(newPassword));
        emp.setPasswordType(SecurityConstants.DEFAULT_PASSWORD_TYPE);
        employeeService.updateById(emp);

        log.info("员工重置密码成功 - username: {}, empId: {}", username, emp.getId());
        return R.success("密码重置成功，请使用新密码登录");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 根据员工角色整数值解析角色标识
     * 1=admin(超级管理员), 2=manager(店长/普通员工)
     */
    private String resolveRoleKey(Integer role) {
        if (role == null) return "manager";
        return role == 1 ? "admin" : "manager";
    }

    /**
     * 记录登录失败
     */
    private void recordLoginFailure(HttpServletRequest request, String username) {
        if (bruteForceProtectionFilter != null) {
            // 如果有用户名，记录用户名；否则记录 IP
            if (username != null && !username.trim().isEmpty()) {
                bruteForceProtectionFilter.recordFailedAttempt(username);
            } else {
                bruteForceProtectionFilter.recordLoginFailure(request);
            }

            // 记录失败日志（脱敏）
            int attempts = bruteForceProtectionFilter.getFailedAttempts(request);
            log.warn("员工登录失败 - 用户名：{}, 失败次数：{}/{}",
                LogMaskUtils.maskUsername(username), attempts, SecurityConstants.MAX_LOGIN_FAIL_COUNT);
        }
    }

    /**
     * 员工退出
     * @param request HTTP请求对象
     * @return 退出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "员工退出", description = "退出当前登录账号，清除Session和租户上下文")
    public R<String> logout(HttpServletRequest request){
        request.getSession().removeAttribute("employee");
        request.getSession().removeAttribute("tenantId");
        BaseContext.remove(); // 清理 ThreadLocal，防止租户信息泄露
        return R.success("退出成功");
    }

    /**
     * 新增员工
     * @param request HTTP请求对象
     * @param employee 员工信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增员工", description = "创建新的员工账号，仅管理员可操作，初始密码统一设置")
    @Parameter(name = "employee", description = "员工信息（用户名、姓名、手机号、角色等）", required = true)
    public R<String> save(HttpServletRequest request,@Valid @RequestBody Employee employee){
        // 权限校验：仅管理员可新增员工
        if (!isAdmin(request)) {
            return R.error("权限不足，仅管理员可新增员工");
        }

        log.info("新增员工，员工信息：手机号={}，身份证号={}",
            LogMaskUtils.maskPhone(employee.getPhone()),
            LogMaskUtils.maskIdCard(employee.getIdNumber()));

        // 生成随机初始密码（使用BCrypt加密）
        String initialPassword = SecurityConstants.generateRandomPassword();
        employee.setPassword(PasswordUtils.encodePassword(initialPassword));
        employee.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);

        employee.setTenantId(BaseContext.getCurrentTenantId());

        employeeService.save(employee);

        log.info("新增员工成功，初始密码已生成（请妥善保管）");
        return R.success("新增员工成功，初始密码：" + initialPassword);
    }

    /**
     * 员工信息分页查询
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 员工姓名（可选，模糊查询）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "员工分页查询", description = "分页查询员工列表，支持按姓名模糊搜索和状态筛选，自动过滤当前租户数据")
    @Parameter(name = "page", description = "页码，从1开始", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "员工姓名（可选，模糊查询）")
    @Parameter(name = "status", description = "账号状态（可选，1=正常 ,0=禁用）")
    public R<Page<Employee>> page(int page,int pageSize,String name, @RequestParam(required = false) Integer status){
        log.info("page = {},pageSize = {},name = {}" ,page,pageSize,name);

        //构造分页构造器
        Page<Employee> pageInfo = new Page<>(page, pageSize);

        //构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null && !name.isEmpty(),Employee::getName,name);
        queryWrapper.eq(status != null, Employee::getStatus, status);
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
     * @param request HTTP请求对象
     * @param employee 员工信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改员工信息", description = "根据ID更新员工信息，仅管理员可操作")
    @Parameter(name = "employee", description = "员工信息（包含ID）", required = true)
    public R<String> update(HttpServletRequest request,@RequestBody Employee employee){
        // 权限校验：仅管理员可修改员工信息
        if (!isAdmin(request)) {
            return R.error("权限不足，仅管理员可修改员工信息");
        }

        // 安全检查：禁止通过此接口修改密码和密码类型
        employee.setPassword(null);
        employee.setPasswordType(null);

        log.info("修改员工信息，手机号={}，身份证号={}",
            LogMaskUtils.maskPhone(employee.getPhone()),
            LogMaskUtils.maskIdCard(employee.getIdNumber()));

        employeeService.updateById(employee);

        return R.success("员工信息修改成功");
    }

    /**
     * 修改员工状态（启用/禁用）
     */
    @PutMapping("/status")
    @Operation(summary = "修改员工状态", description = "仅更新员工启用/禁用状态，不影响其他字段")
    public R<String> updateStatus(HttpServletRequest request, @RequestBody Map<String, Object> params) {
        if (!isAdmin(request)) {
            return R.error("权限不足");
        }
        Long id = params.get("id") != null ? Long.valueOf(params.get("id").toString()) : null;
        Integer status = params.get("status") != null ? Integer.valueOf(params.get("status").toString()) : null;
        if (id == null || status == null) {
            return R.error("参数错误");
        }
        Employee emp = new Employee();
        emp.setId(id);
        emp.setStatus(status);
        employeeService.updateById(emp);
        return R.success("状态更新成功");
    }

    /**
     * 根据id查询员工信息
     * @param id 员工ID
     * @return 员工详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询员工信息", description = "根据ID查询员工详情，返回脱敏后的信息")
    @Parameter(name = "id", description = "员工ID", required = true)
    public R<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息...");
        Employee employee = employeeService.getById(id);
        if(employee != null){
            return R.success(employee);
        }
        return R.error("没有查询到对应员工信息");
    }

    /**
     * 删除员工
     * @param request HTTP请求对象
     * @param ids 员工ID列表
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "删除员工", description = "批量删除员工，仅管理员可操作，不允许删除自己")
    @Parameter(name = "ids", description = "员工ID列表", required = true)
    public R<String> delete(HttpServletRequest request, @RequestParam List<Long> ids) {
        if (!isAdmin(request)) {
            return R.error("权限不足，仅管理员可删除员工");
        }
        Long currentEmpId = (Long) request.getSession().getAttribute("employee");
        if (ids.contains(currentEmpId)) {
            return R.error("不允许删除当前登录账号");
        }
        log.info("删除员工：ids={}", ids);
        employeeService.removeByIds(ids);
        return R.success("删除成功");
    }

    /**
     * 修改密码
     * @param request HTTP请求对象
     * @param params 包含 oldPassword 和 newPassword
     * @return 操作结果
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录员工的密码，需验证旧密码")
    public R<String> updatePassword(HttpServletRequest request, @RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            return R.error("旧密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return R.error("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return R.error("新密码长度不能少于6位");
        }

        Long empId = (Long) request.getSession().getAttribute("employee");
        if (empId == null) {
            return R.error("请先登录");
        }
        Employee emp = employeeService.getById(empId);
        if (emp == null) {
            return R.error("员工不存在");
        }

        // 校验旧密码
        String passwordType = emp.getPasswordType() != null ? emp.getPasswordType() : SecurityConstants.PASSWORD_TYPE_MD5;
        if (!PasswordUtils.matches(oldPassword, emp.getPassword(), passwordType)) {
            return R.error("旧密码错误");
        }

        // 更新为新密码（BCrypt加密）
        emp.setPassword(PasswordUtils.encodePassword(newPassword));
        emp.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
        employeeService.updateById(emp);

        log.info("员工 {} 修改密码成功", emp.getUsername());
        return R.success("密码修改成功");
    }

    /**
     * 检查当前登录用户是否为管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        Object empIdObj = request.getSession().getAttribute("employee");
        if (empIdObj == null) {
            return false;
        }
        Long empId = (Long) empIdObj;
        Employee currentEmp = employeeService.getById(empId);

        // 更新 Session 中的租户信息（确保租户上下文最新）
        if (currentEmp != null) {
            request.getSession().setAttribute("tenantId", currentEmp.getTenantId());
            return EmployeeRole.isAdmin(currentEmp.getRole());
        }

        return false;
    }

    /**
     * 获取筛选下拉选项（员工姓名列表）
     * <p>从数据库动态查询当前租户的所有员工姓名，供前端下拉框使用</p>
     *
     * @return 包含 names 列表的 Map
     */
    @GetMapping("/options")
    @Operation(summary = "筛选选项", description = "获取当前租户所有员工姓名，供搜索条件下拉框使用")
    public R<Map<String, List<String>>> options() {
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(Employee::getTenantId, tenantId);
        }
        queryWrapper.orderByAsc(Employee::getName);
        List<Employee> list = employeeService.list(queryWrapper);

        Set<String> nameSet = new HashSet<>();
        for (Employee emp : list) {
            if (emp.getName() != null && !emp.getName().isEmpty()) {
                nameSet.add(emp.getName());
            }
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("names", new ArrayList<>(nameSet));
        return R.success(result);
    }
}
