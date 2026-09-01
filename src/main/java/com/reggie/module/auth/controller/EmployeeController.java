package com.reggie.module.auth.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.PasswordUtils;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.common.SecurityConstants;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.annotation.RequiresAdmin;
import com.reggie.dto.auth.EmployeeLoginDTO;
import com.reggie.module.auth.dto.UpdateEmployeeStatusBatchDTO;
import com.reggie.module.auth.dto.UpdateEmployeeStatusDTO;
import com.reggie.module.auth.model.Employee;
import com.reggie.enums.EmployeeRole;
import com.reggie.enums.UserStatus;
import com.reggie.module.auth.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.ArrayList;
import java.util.Arrays;
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
    private com.reggie.utils.SMSUtils smsUtils;

    @Value("${reggie.sms.sign-name:瑞吉外卖}")
    private String smsSignName = "瑞吉外卖";

    @Value("${reggie.sms.template-code:}")
    private String smsTemplateCode;

    /**
     * 短信Mock模式开关（dev环境默认true，跳过短信验证码校验）
     */
    @Value("${reggie.sms.mock-mode:false}")
    private boolean smsMockMode;

    /**
     * 忘记密码 Mock 模式开关（默认 false，仅开发环境通过配置开启）
     * 安全说明：此开关独立于 smsMockMode，用于在开发环境跳过验证码校验；
     * 生产环境必须为 false，防止攻击者绕过验证码重置任意账号密码。
     * 额外保护：即使配置文件中误设 true，代码层面也强制要求 active profile 包含 dev/test 才生效。
     */
    @Value("${reggie.sms.forgot-password.mock-enabled:false}")
    private boolean forgotPasswordMockEnabled;

    /**
     * 当前激活的 Spring Profile（dev/prod/test）
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 员工登录
     * @param request HTTP请求对象
     * @param loginDTO 登录信息
     * @return 登录结果
     */
    @PostMapping("/login")
    @Operation(summary = "员工登录", description = "员工账号密码登录，支持验证码")
    @Parameter(name = "loginDTO", description = "员工登录信息（用户名、密码）", required = true)
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.IP)
    public R<Map<String, Object>> login(HttpServletRequest request, @Valid @RequestBody EmployeeLoginDTO loginDTO) {

        //1、根据页面提交的用户名username查询数据库
        // 修复 P2-1：employee 表在 IGNORE_TABLES 中，需手动附加租户条件，防止跨租户用户名枚举
        Long tenantId = BaseContext.getCurrentTenantId();
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, loginDTO.getUsername());
        if (tenantId != null) {
            queryWrapper.eq(Employee::getTenantId, tenantId);
        }
        Employee emp = employeeService.getOne(queryWrapper);

        //2、如果没有查询到则返回登录失败结果
        if (emp == null) {
            return R.error("用户名或密码错误");
        }

        //2a、员工状态检查（修复P0-5：原检查在密码校验之后，禁用用户可完成登录并触发密码升级）
        // 修复说明：状态检查必须在密码校验和密码升级之前执行，避免禁用用户完成完整登录流程，
        // 且状态检查失败不应记录登录失败次数（避免攻击者通过错误信息探测账号状态）。
        if (emp.getStatus() != null && UserStatus.DISABLED.getValue() == emp.getStatus().intValue()) {
            log.warn("禁用账号尝试登录 - 用户名：{}", LogMaskUtils.maskUsername(loginDTO.getUsername()));
            return R.error("用户名或密码错误");
        }

        //3、密码校验（支持MD5和BCrypt）
        String rawPassword = loginDTO.getPassword();
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
                // 设置当前用户ID，用于MyBatis-Plus自动填充updateUser字段
                BaseContext.setCurrentId(emp.getId());
                employeeService.updateById(emp);
            }
        }

        //5、登录成功，将员工id和租户id存入Session
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

        // 防止Session Fixation攻击：登录成功后切换Session ID
        request.changeSessionId();

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
     * 忘记密码 - 通过用户名+手机号+短信验证码验证后重置密码
     * @param params 包含 username, phone, code, newPassword
     * @param session HTTP会话
     * @return 操作结果
     */
    @PostMapping("/forgot-password")
    @Operation(summary = "忘记密码", description = "通过用户名、手机号和短信验证码验证后重置密码")
    @RateLimit(maxRequestsPerSecond = 1)
    public R<String> forgotPassword(HttpServletRequest request, @RequestBody Map<String, String> params, HttpSession session) {
        String username = params.get("username");
        String phone = params.get("phone");
        String code = params.get("code");
        // 修改点：兼容前端可能发送 password 或 newPassword 字段
        String newPassword = params.get("newPassword");
        if (newPassword == null || newPassword.isEmpty()) {
            newPassword = params.get("password");
        }

        if (username == null || username.trim().isEmpty()) {
            return R.error("请输入用户名");
        }
        if (phone == null || phone.trim().isEmpty()) {
            return R.error("请输入手机号");
        }
        if (newPassword == null || newPassword.length() < 6) {
            return R.error("新密码至少6位");
        }

        // 修改点：忘记密码接口强制校验短信验证码，不受 smsMockMode 影响
        // 安全说明：forgotPassword 是密码重置入口，跳过验证码等价于允许任意人重置任意账号密码，
        // 属严重安全缺陷。Mock 模式仅应用于登录场景的短信发送模拟，不应延伸至密码重置。
        // 如需在开发环境测试，通过 @Value 配置开关单独控制，且代码层面强制要求非生产环境。
        if (forgotPasswordMockEnabled) {
            // 修复 P2-2：白名单模式——仅 dev/test 环境允许 mock，其他环境一律拒绝
            boolean isDevEnv = "dev".equals(activeProfile) || "test".equals(activeProfile)
                    || "local".equals(activeProfile);
            if (!isDevEnv) {
                log.warn("[安全] 非开发环境拒绝使用 forgotPassword mock 模式，强制要求验证码 - activeProfile={}", activeProfile);
            } else {
                log.warn("开发环境：忘记密码跳过验证码校验 - username: {}, phone: {}, activeProfile: {}",
                    LogMaskUtils.maskUsername(username), LogMaskUtils.maskPhone(phone), activeProfile);
                Long tId = BaseContext.getCurrentTenantId();
                LambdaQueryWrapper<Employee> mockQw = new LambdaQueryWrapper<>();
                mockQw.eq(Employee::getUsername, username.trim());
                mockQw.eq(Employee::getPhone, phone.trim());
                if (tId != null) {
                    mockQw.eq(Employee::getTenantId, tId);
                }
                Employee mockEmp = employeeService.getOne(mockQw);
                if (mockEmp == null) {
                    return R.error("用户名与手机号不匹配");
                }
                mockEmp.setPassword(PasswordUtils.encodePassword(newPassword));
                mockEmp.setPasswordType(SecurityConstants.DEFAULT_PASSWORD_TYPE);
                employeeService.updateById(mockEmp);
                log.info("员工重置密码成功（mock模式） - username: {}, empId: {}, ip: {}, activeProfile: {}",
                    username, mockEmp.getId(), request.getRemoteAddr(), activeProfile);
                return R.success("密码重置成功，请使用新密码登录");
            }
        }

        // 验证码校验（mock 模式且非生产环境时已提前返回，此处处理正常流程）
        if (code == null || code.trim().isEmpty()) {
            return R.error("请输入短信验证码");
        }
        String sessionCode = (String) session.getAttribute("smsCode_" + phone);
        if (sessionCode == null) {
            return R.error("请先获取短信验证码");
        }
        if (!sessionCode.equals(code)) {
            return R.error("验证码错误");
        }
        // 验证通过后清除验证码（一次性使用）
        session.removeAttribute("smsCode_" + phone);
        session.removeAttribute("smsCode_" + phone + "_time");

        // 租户隔离：修复 P0-5，forgotPassword 为公开接口（无登录态，tenantId 必然为 null），
        // 原实现 if(tenantId!=null) 条件永不满足导致无租户过滤，攻击者可跨租户重置密码。
        // 修复方案：employee 表含 tenant_id 列时强制匹配当前请求的租户；
        // 因公开接口无租户上下文，改为通过手机号反查该手机号所属租户的唯一员工。
        // 若员工表无 tenant_id 列（IGNORE_TABLES），则 username+phone 全局唯一查找，记录安全日志。
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

        log.info("员工重置密码成功 - username: {}, empId: {}, ip: {}",
            username, emp.getId(), request.getRemoteAddr());
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
        // 修改点：登录角色标识需与 role 表的 role_key 保持一致（SUPER_ADMIN / STORE_MANAGER）。
        // 原实现返回 admin/manager，而 role 表实际存的是 SUPER_ADMIN/STORE_MANAGER，
        // 导致 PermissionAspect.loadPermissionsFromDb 通过 roleKey 查不到角色，
        // RBAC 实际失效（非管理员永远无权限）。现对齐为数据库角色标识。
        if (role == null) return "STORE_MANAGER";
        return role == 1 ? "SUPER_ADMIN" : "STORE_MANAGER";
    }

    /**
     * 员工退出
     * @param request HTTP请求对象
     * @return 退出结果
     */
    @PostMapping("/logout")
    @Operation(summary = "员工退出", description = "退出当前登录账号，清除Session和租户上下文")
    public R<String> logout(HttpServletRequest request){
        // 必须 invalidate 整个 Session，仅 removeAttribute 不会使 Session ID 失效
        try {
            request.getSession().invalidate();
        } catch (IllegalStateException e) {
            log.warn("[登出] Session 已失效或不存在", e);
        }
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
    @RequireEmployee
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增员工", description = "创建新的员工账号，仅管理员可操作，初始密码统一设置")
    @Parameter(name = "employee", description = "员工信息（用户名、姓名、手机号、角色等）", required = true)
    public R<Map<String, Object>> save(HttpServletRequest request,@Valid @RequestBody Employee employee){
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
        // 初始密码通过短信发送给员工
        employee.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);

        employee.setTenantId(BaseContext.getCurrentTenantId());

        employeeService.save(employee);

        // 发送初始密码短信
        if (employee.getPhone() != null && !employee.getPhone().isEmpty()) {
            if (smsUtils != null && smsTemplateCode != null && !smsTemplateCode.isEmpty() && !smsMockMode) {
                try {
                    String smsParam = "{\"name\":\"" + employee.getName() + "\",\"password\":\"" + initialPassword + "\"}";
                    smsUtils.sendMessage(smsSignName, smsTemplateCode, employee.getPhone(), smsParam);
                    log.info("初始密码短信发送成功 - empId: {}, phone: {}", employee.getId(), LogMaskUtils.maskPhone(employee.getPhone()));
                } catch (Exception e) {
                    log.error("初始密码短信发送失败 - empId: {}, phone: {}, error: {}", employee.getId(), LogMaskUtils.maskPhone(employee.getPhone()), e.getMessage(), e);
                }
            } else {
                log.info("【开发环境/Mock模式】员工初始密码已生成 - 姓名：{}，手机号：{}", employee.getName(), LogMaskUtils.maskPhone(employee.getPhone()));
            }
        }

        log.info("新增员工成功，初始密码已通过短信/邮件发送给用户");
        // 返回脱敏的成功信息，不包含密码
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("消息", "新增员工成功，初始密码已通过短信/邮件发送给用户");
        return R.success(result);
    }

    /**
     * 员工信息分页查询
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 员工姓名（可选，模糊查询）
     * @return 分页结果
     */
    @GetMapping("/page")
    @RequireEmployee
    @Operation(summary = "员工分页查询", description = "分页查询员工列表，支持按姓名模糊搜索和状态筛选，自动过滤当前租户数据")
    @Parameter(name = "page", description = "页码，从1开始", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "员工姓名（可选，模糊查询）")
    @Parameter(name = "status", description = "账号状态（可选，1=正常 ,0=禁用）")
    public R<Page<Employee>> page(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) Integer status){
        log.debug("分页查询员工：page={}, pageSize={}, name={}", page, pageSize, name);

        //构造分页构造器
        Page<Employee> pageInfo = PageUtils.of(page, pageSize);

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

        // 脱敏：移除密码、手机号、身份证等敏感字段
        if (pageInfo.getRecords() != null) {
            for (Employee emp : pageInfo.getRecords()) {
                emp.setPassword(null);
                emp.setPasswordType(null);
                emp.setPhone(emp.getPhone() != null ? maskPhone(emp.getPhone()) : null);
                emp.setIdNumber(null);
            }
        }

        return R.success(pageInfo);
    }

    /**
     * 员工统计
     * <p>使用 count() 聚合查询替代前端全量拉取，避免全表扫描与分页截断风险</p>
     *
     * @return 员工总数、正常数、已禁用数、本月新增数
     */
    @GetMapping("/stats")
    @RequireEmployee
    @Operation(summary = "员工统计", description = "获取员工总数、正常数、已禁用数、本月新增数")
    public R<Map<String, Object>> stats() {
        Long tenantId = BaseContext.getCurrentTenantId();

        LambdaQueryWrapper<Employee> totalQw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Employee> activeQw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Employee> disabledQw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<Employee> newQw = new LambdaQueryWrapper<>();

        // employee 表在租户忽略列表中，需手动添加租户过滤
        if (tenantId != null) {
            totalQw.eq(Employee::getTenantId, tenantId);
            activeQw.eq(Employee::getTenantId, tenantId);
            disabledQw.eq(Employee::getTenantId, tenantId);
            newQw.eq(Employee::getTenantId, tenantId);
        }

        activeQw.eq(Employee::getStatus, UserStatus.ENABLED.getValue());
        disabledQw.eq(Employee::getStatus, UserStatus.DISABLED.getValue());

        // 本月新增：createTime >= 当月1日
        java.time.LocalDateTime monthStart = java.time.LocalDate.now()
                .withDayOfMonth(1).atStartOfDay();
        newQw.ge(Employee::getCreateTime, monthStart);

        Map<String, Object> result = new HashMap<>();
        result.put("totalEmployees", employeeService.count(totalQw));
        result.put("activeEmployees", employeeService.count(activeQw));
        result.put("disabledEmployees", employeeService.count(disabledQw));
        result.put("newThisMonth", employeeService.count(newQw));
        return R.success(result);
    }

    /**
     * 根据id修改员工信息
     * @param request HTTP请求对象
     * @param employee 员工信息
     * @return 操作结果
     */
    @PutMapping
    @RequireEmployee
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改员工信息", description = "根据ID更新员工信息，仅管理员可操作")
    @Parameter(name = "employee", description = "员工信息（包含ID）", required = true)
    public R<String> update(HttpServletRequest request, @Valid @RequestBody Employee employee) {
        // 权限校验：仅管理员可修改员工信息
        if (!isAdmin(request)) {
            return R.error("权限不足，仅管理员可修改员工信息");
        }
        if (employee.getId() == null) {
            return R.error("员工ID不能为空");
        }

        // 修改点：先加载已存在记录并校验租户归属，防止跨租户越权改写
        Employee existing = employeeService.getById(employee.getId());
        if (existing == null) {
            return R.error("员工不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(existing.getTenantId())) {
            return R.error("无权操作其他租户的员工");
        }

        // 修改点：白名单字段更新，禁止通过此接口越权改写 role/tenantId/password 等敏感字段
        LambdaUpdateWrapper<Employee> uw = new LambdaUpdateWrapper<>();
        uw.eq(Employee::getId, employee.getId());
        if (employee.getName() != null) {
            uw.set(Employee::getName, employee.getName());
        }
        if (employee.getUsername() != null) {
            uw.set(Employee::getUsername, employee.getUsername());
        }
        if (employee.getPhone() != null) {
            uw.set(Employee::getPhone, employee.getPhone());
        }
        if (employee.getIdNumber() != null) {
            uw.set(Employee::getIdNumber, employee.getIdNumber());
        }
        if (employee.getSex() != null) {
            uw.set(Employee::getSex, employee.getSex());
        }
        if (employee.getStatus() != null) {
            uw.set(Employee::getStatus, employee.getStatus());
        }

        log.info("修改员工信息，手机号={}，身份证号={}",
            LogMaskUtils.maskPhone(employee.getPhone()),
            LogMaskUtils.maskIdCard(employee.getIdNumber()));

        employeeService.update(uw);
        return R.success("员工信息修改成功");
    }

    /**
     * 修改员工状态（启用/禁用）
     */
    @PutMapping("/status")
    @RequireEmployee
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改员工状态", description = "仅更新员工启用/禁用状态，不影响其他字段，自动校验租户权限")
    public R<String> updateStatus(HttpServletRequest request, @Valid @RequestBody UpdateEmployeeStatusDTO dto) {
        if (!isAdmin(request)) {
            return R.error("权限不足");
        }
        Long id = dto.getId();
        Integer status = dto.getStatus();
        if (id == null || status == null) {
            return R.error("参数错误");
        }
        // 租户校验：确保只能修改当前租户的员工
        Employee target = employeeService.getById(id);
        if (target == null) {
            return R.error("员工不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(target.getTenantId())) {
            return R.error("无权操作其他租户的员工");
        }
        Employee emp = new Employee();
        emp.setId(id);
        emp.setStatus(status);
        employeeService.updateById(emp);
        return R.success("状态更新成功");
    }

    /**
     * 批量修改员工状态（启用/禁用）
     */
    @PutMapping("/batch/status")
    @RequireEmployee
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "批量修改员工状态", description = "批量更新员工启用/禁用状态，自动校验租户权限")
    public R<String> updateStatusBatch(HttpServletRequest request, @Valid @RequestBody UpdateEmployeeStatusBatchDTO dto) {
        if (!isAdmin(request)) {
            return R.error("权限不足");
        }
        List<Long> ids = dto.getIds();
        Integer status = dto.getStatus();
        if (ids == null || ids.isEmpty() || status == null) {
            return R.error("参数错误");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        List<Employee> targets = employeeService.listByIds(ids);
        List<Long> unauthorizedIds = new ArrayList<>();
        for (Employee target : targets) {
            if (target == null) {
                continue;
            }
            if (currentTenantId != null && !currentTenantId.equals(target.getTenantId())) {
                unauthorizedIds.add(target.getId());
            }
        }
        if (!unauthorizedIds.isEmpty()) {
            return R.error("以下员工不属于当前租户，无法操作：ID=" + unauthorizedIds);
        }
        LambdaUpdateWrapper<Employee> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Employee::getTenantId, currentTenantId)
                .in(Employee::getId, ids).set(Employee::getStatus, status);
        employeeService.update(updateWrapper);
        return R.success("状态更新成功");
    }

    /**
     * 根据id查询员工信息
     * @param id 员工ID
     * @return 员工详情
     */
    @GetMapping("/{id}")
    @RequireEmployee
    @Operation(summary = "查询员工信息", description = "根据ID查询员工详情，返回脱敏后的信息")
    @Parameter(name = "id", description = "员工ID", required = true)
    public R<Employee> getById(@PathVariable Long id){
        log.debug("根据ID查询员工信息");
        Employee employee = employeeService.getById(id);
        if(employee != null){
            // 租户校验：确保只能查询当前租户的员工
            Long currentTenantId = BaseContext.getCurrentTenantId();
            if (currentTenantId != null && !currentTenantId.equals(employee.getTenantId())) {
                return R.error("没有查询到对应员工信息");
            }
            // 脱敏：移除密码、手机号、身份证等敏感字段
            employee.setPassword(null);
            employee.setPasswordType(null);
            employee.setPhone(employee.getPhone() != null ? maskPhone(employee.getPhone()) : null);
            employee.setIdNumber(null);
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
    @RequireEmployee
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除员工", description = "批量删除员工，仅管理员可操作，不允许删除自己")
    @Parameter(name = "ids", description = "员工ID列表", required = true)
    public R<String> delete(HttpServletRequest request, @RequestParam List<Long> ids) {
        if (!isAdmin(request)) {
            return R.error("权限不足，仅管理员可删除员工");
        }
        if (ids == null || ids.isEmpty()) {
            return R.error("请选择要删除的员工");
        }
        // 空集合传入 MP .in() 会导致 `IN ()` 语法错误
        Object empAttr = request.getSession().getAttribute("employee");
        Long currentEmpId = (empAttr instanceof Number) ? ((Number) empAttr).longValue() : null;
        if (currentEmpId == null) {
            return R.error("登录状态异常，请重新登录");
        }
        if (ids.contains(currentEmpId)) {
            return R.error("不允许删除当前登录账号");
        }
        // 租户校验（employee 在 IGNORE_TABLES 中不自动过滤）：按租户+ID限定删除范围，fail-closed
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId == null) {
            return R.error("租户信息缺失，无法删除");
        }
        log.info("删除员工：ids={}", ids);
        LambdaUpdateWrapper<Employee> deleteWrapper = new LambdaUpdateWrapper<>();
        deleteWrapper.in(Employee::getId, ids)
                     .eq(Employee::getTenantId, currentTenantId);
        boolean removed = employeeService.remove(deleteWrapper);
        if (!removed) {
            return R.error("删除失败：员工不存在或不属于当前租户");
        }
        return R.success("删除成功");
    }

    /**
     * 修改密码
     * @param request HTTP请求对象
     * @param params 包含 oldPassword 和 newPassword
     * @return 操作结果
     */
    @PutMapping("/password")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
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

        // 修复 P2-8：密码修改后失效当前 Session，强制重新登录
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        log.info("员工 {} 修改密码成功，Session 已失效", emp.getUsername());
        return R.success("密码修改成功，请重新登录");
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
    @RequireEmployee
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

    /**
     * 获取当前租户所有员工选项（含id和name），供考勤/排班页面下拉选择使用
     */
    @GetMapping("/list")
    @RequireEmployee
    @Operation(summary = "员工列表选项", description = "获取当前租户所有员工id和name，供下拉选择使用")
    public R<List<Map<String, Object>>> listForSelect() {
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(Employee::getTenantId, tenantId);
        }
        queryWrapper.select(Employee::getId, Employee::getName)
                   .orderByAsc(Employee::getName);
        List<Employee> list = employeeService.list(queryWrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Employee emp : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", emp.getId());
            item.put("name", emp.getName());
            result.add(item);
        }
        return R.success(result);
    }
}

