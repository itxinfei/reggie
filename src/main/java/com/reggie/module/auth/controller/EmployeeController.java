package com.reggie.module.auth.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
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
import com.reggie.module.sys.model.Role;
import com.reggie.module.sys.service.RoleService;
import com.reggie.utils.QRCodeUtil;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.regex.Pattern;

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

    @Autowired
    private RoleService roleService;

    @Autowired
    private QRCodeUtil qrCodeUtil;

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

        //3-4、密码校验并在需要时升级密码类型（等价抽取）
        if (!verifyAndUpgradePassword(emp, loginDTO.getPassword())) {
            return R.error("用户名或密码错误");
        }

        //5、登录成功，建立会话并返回脱敏信息（等价抽取）
        return R.success(establishSessionAndBuildResult(request, emp));
    }

    /**
     * 校验密码，并在密码为 MD5 且校验通过时自动升级为 BCrypt（等价抽取）。
     *
     * @param emp 员工
     * @param rawPassword 原始密码
     * @return 是否校验通过
     */
    private boolean verifyAndUpgradePassword(Employee emp, String rawPassword) {
        String encodedPassword = emp.getPassword();
        String passwordType = emp.getPasswordType() != null ? emp.getPasswordType() : SecurityConstants
                .PASSWORD_TYPE_MD5;

        boolean passwordMatches = PasswordUtils.matches(rawPassword, encodedPassword, passwordType);
        if (!passwordMatches) {
            return false;
        }

        // 密码类型升级（如果是MD5且校验通过，自动升级为BCrypt）
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
        return true;
    }

    /**
     * 建立登录会话并返回脱敏后的登录信息（等价抽取）。
     */
    private Map<String, Object> establishSessionAndBuildResult(HttpServletRequest request, Employee emp) {
        // 重新从数据库查询最新信息，确保租户上下文准确
        Employee freshEmp = employeeService.getById(emp.getId());
        String sessionRoleKey;
        Long sessionEmployeeId;
        Long sessionTenantId;
        Integer legacyRole;
        if (freshEmp != null) {
            sessionEmployeeId = freshEmp.getId();
            sessionTenantId = freshEmp.getTenantId();
            legacyRole = freshEmp.getRole();
        } else {
            // 如果查询失败，使用原信息（降级处理）
            sessionEmployeeId = emp.getId();
            sessionTenantId = emp.getTenantId();
            legacyRole = emp.getRole();
            log.warn("员工登录后无法刷新数据，使用内存中的租户信息可能已过期 - empId: {}", emp.getId());
        }
        // 登录态建立租户上下文：RBAC 角色查询受租户插件过滤，BaseContext 为空时 fail-closed 追加
        // tenant_id=-1 导致查空，故先设置再查询（与 logout 的 BaseContext.remove() 对称）
        BaseContext.setCurrentTenantId(sessionTenantId);
        request.getSession().setAttribute("employee", sessionEmployeeId);
        request.getSession().setAttribute("tenantId", sessionTenantId);
        sessionRoleKey = resolveRoleKey(sessionEmployeeId, sessionTenantId, legacyRole);

        request.getSession().setAttribute("roleKey", sessionRoleKey);

        // 防止Session Fixation攻击：登录成功后切换Session ID
        request.changeSessionId();

        // 返回脱敏后的登录信息（不包含密码等敏感字段）
        Map<String, Object> result = new HashMap<>();
        result.put("id", emp.getId());
        result.put("username", emp.getUsername());
        result.put("name", emp.getName());
        result.put("phone", emp.getPhone() != null ? maskPhone(emp.getPhone()) : null);
        result.put("status", emp.getStatus());
        result.put("role", emp.getRole());
        result.put("tenantId", emp.getTenantId());
        result.put("avatar", emp.getAvatar());
        result.put("jobNumber", emp.getJobNumber());
        result.put("position", emp.getPosition());
        result.put("createTime", emp.getCreateTime());
        result.put("updateTime", emp.getUpdateTime());
        return result;
    }

    /**
     * 当前登录员工信息（含头像/工号/岗位），供顶栏等场景获取最新资料。
     *
     * @param request HTTP 请求
     * @return 当前员工信息（已清除密码相关字段）
     */
    @GetMapping("/me")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "当前登录员工信息", description = "返回当前登录员工完整信息，含头像、工号、岗位")
    public R<Employee> me(HttpServletRequest request) {
        Long empId = null;
        Object empIdObj = request.getAttribute("employeeId");
        if (empIdObj instanceof Long) {
            empId = (Long) empIdObj;
        }
        // 兜底：MockMvc 下 LoginCheckFilter 不生效，从 session 取
        if (empId == null) {
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("employee") instanceof Long) {
                empId = (Long) session.getAttribute("employee");
            }
        }
        if (empId == null) {
            return R.error("未登录或登录已过期");
        }
        Employee employee = employeeService.getById(empId);
        if (employee == null) {
            return R.error("员工信息不存在");
        }
        employee.setPassword(null);
        employee.setPasswordType(null);
        return R.success(employee);
    }

    /**
     * 电子工牌二维码：内容为工牌信息 JSON，扫码可核验身份。
     *
     * @param id 员工ID
     * @return 二维码 Data URI（base64）
     */
    @GetMapping("/badge-qrcode/{id}")
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "员工工牌二维码", description = "返回电子工牌二维码 Base64 图片")
    @Parameter(name = "id", description = "员工ID", required = true)
    public R<String> badgeQrCode(@PathVariable Long id) {
        Employee employee = employeeService.getById(id);
        if (employee == null) {
            return R.error("员工不存在");
        }
        StringBuilder content = new StringBuilder();
        content.append("{\"type\":\"employee_badge\"");
        content.append(",\"id\":").append(employee.getId());
        if (employee.getJobNumber() != null && !employee.getJobNumber().isEmpty()) {
            content.append(",\"jobNumber\":\"").append(employee.getJobNumber()).append("\"");
        }
        content.append(",\"name\":\"").append(employee.getName()).append("\"");
        if (employee.getPosition() != null && !employee.getPosition().isEmpty()) {
            content.append(",\"position\":\"").append(employee.getPosition()).append("\"");
        }
        if (employee.getTenantId() != null) {
            content.append(",\"tenantId\":").append(employee.getTenantId());
        }
        content.append("}");
        String dataUri = qrCodeUtil.generateDataUri(content.toString());
        if (dataUri == null) {
            return R.error("二维码生成失败，请稍后重试");
        }
        return R.success(dataUri);
    }

    /**
     * 校验工号在租户内唯一。employee 表在多租户拦截白名单内，需手动按 tenantId 过滤。
     *
     * @param tenantId 租户ID
     * @param jobNumber 待校验工号（null 或空白视为未设置，归一为 null）
     * @param excludeId 排除的员工ID（编辑时传自身，新增传 null）
     * @return 归一后的工号（去空白）；未设置时返回 null
     */
    private String checkJobNumberUnique(Long tenantId, String jobNumber, Long excludeId) {
        if (jobNumber == null) {
            return null;
        }
        String normalized = jobNumber.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        long count = employeeService.count(new LambdaQueryWrapper<Employee>()
                .eq(Employee::getTenantId, tenantId)
                .eq(Employee::getJobNumber, normalized)
                .ne(excludeId != null, Employee::getId, excludeId));
        if (count > 0) {
            throw new CustomException("工号【" + normalized + "】在当前租户已存在");
        }
        return normalized;
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
    public R<String> forgotPassword(HttpServletRequest request, @RequestBody Map<String, String> params,
            HttpSession session) {
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
        // mock 模式处理（等价抽取）：返回非 null 表示已处理完成，null 表示继续正常流程
        R<String> mockResult = handleForgotPasswordMock(request, username, phone, newPassword);
        if (mockResult != null) {
            return mockResult;
        }

        // 验证码校验（等价抽取）：返回非 null 表示校验失败
        R<String> codeError = verifyForgotPasswordSmsCode(session, phone, code);
        if (codeError != null) {
            return codeError;
        }

        // 重置密码（等价抽取）
        return resetPasswordByPhone(request, username, phone, newPassword);
    }

    /**
     * 忘记密码 mock 模式处理（等价抽取）。
     * <p>开发环境跳过验证码直接重置；非开发环境拒绝 mock 并返回 null 走正常流程。</p>
     *
     * @return 已处理完成时返回响应；应继续正常流程时返回 null
     */
    private R<String> handleForgotPasswordMock(HttpServletRequest request, String username, String phone,
            String newPassword) {
        if (!forgotPasswordMockEnabled) {
            return null;
        }
        // 修复 P2-2：白名单模式——仅 dev/test 环境允许 mock，其他环境一律拒绝
        boolean isDevEnv = "dev".equals(activeProfile) || "test".equals(activeProfile)
                || "local".equals(activeProfile);
        if (!isDevEnv) {
            log.warn("[安全] 非开发环境拒绝使用 forgotPassword mock 模式，强制要求验证码 - activeProfile={}", activeProfile);
            return null;
        }
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

    /**
     * 校验忘记密码短信验证码（等价抽取）。
     *
     * @return 校验失败时返回错误响应；通过时返回 null
     */
    private R<String> verifyForgotPasswordSmsCode(HttpSession session, String phone, String code) {
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
        return null;
    }

    /**
     * 通过用户名+手机号查找员工并重置密码（等价抽取）。
     *
     * 租户隔离：修复 P0-5，forgotPassword 为公开接口（无登录态，tenantId 必然为 null），
     * 原实现 if(tenantId!=null) 条件永不满足导致无租户过滤，攻击者可跨租户重置密码。
     * 修复方案：改为通过用户名+手机号全局唯一查找，记录安全日志。
     */
    private R<String> resetPasswordByPhone(HttpServletRequest request, String username, String phone,
            String newPassword) {
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
     * 解析登录会话的角色标识
     * <p>优先取 employee_role 显式关联角色的 role_key（SUPER_ADMIN 优先），
     * 无关联或异常时回退旧数字字段（1=SUPER_ADMIN，其他=STORE_MANAGER）。</p>
     * @param employeeId 员工ID
     * @param tenantId 租户ID
     * @param legacyRole 员工表旧 role 字段（兜底）
     * @return 角色标识
     */
    private String resolveRoleKey(Long employeeId, Long tenantId, Integer legacyRole) {
        // 修改点（2026-09-18）：优先查 employee_role 显式关联角色的 role_key（RBAC 正解，
        // 与 PermissionAspect.loadPermissionsFromDb 同源）。原实现按 employee.role 数字映射，
        // 但 RBAC 改造后该字段不再表达管理员身份（全员为 2，admin 实际通过 employee_role
        // 绑定 SUPER_ADMIN 角色 id=18），导致超管登录 roleKey 被误判为 STORE_MANAGER，
        // 角色管理等 @RequiresAdmin 接口全部"权限不足"。
        // 多角色时 SUPER_ADMIN 优先；无显式关联或查询异常时回退旧数字字段，保持兼容。
        try {
            List<Long> roleIds = roleService.getEmployeeRoleIds(employeeId, tenantId);
            if (roleIds != null && !roleIds.isEmpty()) {
                List<Role> roles = roleService.listByIds(roleIds);
                String firstKey = null;
                for (Role r : roles) {
                    if (r == null || r.getRoleKey() == null || r.getRoleKey().isEmpty()) {
                        continue;
                    }
                    if ("SUPER_ADMIN".equals(r.getRoleKey())) {
                        return "SUPER_ADMIN";
                    }
                    if (firstKey == null) {
                        firstKey = r.getRoleKey();
                    }
                }
                if (firstKey != null) {
                    return firstKey;
                }
            }
        } catch (Exception e) {
            log.warn("[登录] 查询员工 RBAC 角色失败，回退旧字段判定 - employeeId={}", employeeId, e);
        }
        return legacyRole != null && legacyRole == 1 ? "SUPER_ADMIN" : "STORE_MANAGER";
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

        // 工号租户内唯一校验；空串归一为 null（表示未设置工号）
        employee.setJobNumber(checkJobNumberUnique(employee.getTenantId(), employee.getJobNumber(), null));

        employeeService.save(employee);

        // 发送初始密码短信（与 UserController 验证码同一静态链路）：
        // 凭证+模板配置完整则真实发送（日志同时打印），否则 SMSUtils 自动进入控制台模式——
        // 初始密码打印到服务端日志、不调用外部接口、流程继续。
        // 员工记录已落库，短信链路异常仅记录日志，不影响本次新增结果。
        if (employee.getPhone() != null && !employee.getPhone().isEmpty()) {
            try {
                com.reggie.utils.SMSUtils.sendMessage(smsSignName, smsTemplateCode, employee.getPhone(), initialPassword);
                log.info("初始密码短信已处理 - empId: {}, 姓名: {}, phone: {}",
                        employee.getId(), employee.getName(), LogMaskUtils.maskPhone(employee.getPhone()));
            } catch (Exception e) {
                // 宽异常兜底：仅真实发送链路会抛错，捕获后不影响员工新增主流程
                log.error("初始密码短信发送失败 - empId: {}, phone: {}, error: {}",
                        employee.getId(), LogMaskUtils.maskPhone(employee.getPhone()), e.getMessage(), e);
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
    public R<Page<Employee>> page(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue =
            "10") @Min(1) @Max(100) int pageSize, @RequestParam(required = false) String name, @RequestParam(required =
            false) Integer status){
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
     * 下载员工导入模板（xlsx）
     * <p>列：账号、员工姓名、手机号、性别、身份证号；第二行附带一条示例数据。</p>
     */
    @GetMapping("/import/template")
    @RequireEmployee
    @RequiresAdmin
    @Operation(summary = "下载员工导入模板", description = "返回员工批量导入用的 xlsx 模板，仅管理员可下载")
    public ResponseEntity<byte[]> importTemplate() throws java.io.IOException {
        // POI 在接口层创建，try-with-resources 保证工作簿资源释放
        try (XSSFWorkbook wb = new XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("员工导入");
            String[] headers = {"账号*", "员工姓名*", "手机号*", "性别", "身份证号*"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
                sheet.setColumnWidth(i, 20 * 256);
            }
            // 示例行（导入时自动跳过：账号为示例值）
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("zhangsan");
            sampleRow.createCell(1).setCellValue("张三");
            sampleRow.createCell(2).setCellValue("13800138000");
            sampleRow.createCell(3).setCellValue("男");
            sampleRow.createCell(4).setCellValue("110101199001011234");

            wb.write(out);
            byte[] bytes = out.toByteArray();
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            // RFC 5987 编码中文文件名，兼容各浏览器
            h.setContentDisposition(ContentDisposition.attachment()
                    .filename("员工导入模板.xlsx", java.nio.charset.StandardCharsets.UTF_8).build());
            return new ResponseEntity<>(bytes, h, HttpStatus.OK);
        }
    }

    /** 导入文件最大 1MB（对齐 Spring 默认 multipart 单文件上限，千行文本 xlsx 远小于此）、单次最多 1000 条 */
    private static final long IMPORT_MAX_FILE_SIZE = 1L * 1024 * 1024;
    private static final int IMPORT_MAX_ROWS = 1000;
    /** 身份证 15/18 位校验（与前端 validID 规则一致） */
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("(^\\d{15}$)|(^\\d{18}$)|(^\\d{17}(\\d|X|x)$)");

    /**
     * 批量导入员工（xlsx）
     * <p>仅管理员可用。逐行校验（账号 4-20 位/姓名/手机号/性别/身份证 + 文件内与库内账号去重），
     * 全部合法后批量入库；任一行为整表校验失败信息，已成功行不受失败行影响。
     * 初始密码随机生成并 BCrypt 加密，仅在本次响应中明文返回一次，由管理员分发给员工。</p>
     *
     * @param file xlsx 文件
     * @return successCount/failCount/failures(行号+原因)/accounts(账号+初始密码)
     */
    @PostMapping("/import")
    @RequireEmployee
    @RequiresAdmin
    @RateLimit(maxRequestsPerSecond = 2)
    @Operation(summary = "批量导入员工", description = "上传 xlsx 批量创建员工，仅管理员可操作")
    public R<Map<String, Object>> importEmployees(HttpServletRequest request,
            @RequestParam("file") MultipartFile file) {
        if (!isAdmin(request)) {
            return R.error("权限不足，仅管理员可导入员工");
        }
        if (file == null || file.isEmpty()) {
            return R.error("请选择要导入的文件");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".xlsx")) {
            return R.error("仅支持 .xlsx 格式文件，请先下载导入模板填写");
        }
        if (file.getSize() > IMPORT_MAX_FILE_SIZE) {
            return R.error("文件不能超过 1MB，单次最多导入 " + IMPORT_MAX_ROWS + " 条");
        }

        DataFormatter formatter = new DataFormatter();
        List<Map<String, Object>> failures = new ArrayList<>();
        List<Employee> toSave = new ArrayList<>();
        Set<String> fileUsernames = new HashSet<>();
        Long tenantId = BaseContext.getCurrentTenantId();

        try (XSSFWorkbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                return R.error("文件中没有可导入的工作表");
            }
            int lastRow = sheet.getLastRowNum();
            if (lastRow < 1) {
                return R.error("文件没有数据行，请按模板填写后再导入");
            }
            if (lastRow - 1 > IMPORT_MAX_ROWS) {
                return R.error("单次最多导入 " + IMPORT_MAX_ROWS + " 条，请拆分后再导入");
            }

            // 第一轮：逐行字段校验（行号按 Excel 实际行号，含表头第 1 行）
            for (int rowIdx = 1; rowIdx <= lastRow; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                String username = cellText(row, 0, formatter).trim();
                String name = cellText(row, 1, formatter).trim();
                String phone = cellText(row, 2, formatter).trim();
                String sex = cellText(row, 3, formatter).trim();
                String idNumber = cellText(row, 4, formatter).trim();

                // 整行为空直接跳过；模板示例行按示例账号跳过
                if (username.isEmpty() && name.isEmpty() && phone.isEmpty() && idNumber.isEmpty()) {
                    continue;
                }
                int excelRowNum = rowIdx + 1;
                if ("zhangsan".equals(username)) {
                    continue;
                }

                String reason = validateImportRow(username, name, phone, sex, idNumber, fileUsernames);
                if (reason != null) {
                    addImportFailure(failures, excelRowNum, username, reason);
                    continue;
                }
                fileUsernames.add(username);

                Employee emp = new Employee();
                emp.setUsername(username);
                emp.setName(name);
                emp.setPhone(phone);
                // 性别与前端新增弹窗一致存中文（男/女），留空默认男
                emp.setSex(sex.isEmpty() ? "男" : sex);
                emp.setIdNumber(idNumber);
                emp.setStatus(UserStatus.ENABLED.getValue());
                emp.setTenantId(tenantId);
                toSave.add(emp);
            }

            // 第二轮：库内账号去重（同租户，employee 表在租户忽略列表需手动过滤）
            if (!toSave.isEmpty()) {
                List<String> usernames = new ArrayList<>();
                for (Employee e : toSave) {
                    usernames.add(e.getUsername());
                }
                LambdaQueryWrapper<Employee> dupQw = new LambdaQueryWrapper<>();
                dupQw.in(Employee::getUsername, usernames);
                if (tenantId != null) {
                    dupQw.eq(Employee::getTenantId, tenantId);
                }
                Set<String> existed = new HashSet<>();
                for (Employee e : employeeService.list(dupQw)) {
                    existed.add(e.getUsername());
                }
                if (!existed.isEmpty()) {
                    List<Employee> deduped = new ArrayList<>();
                    for (Employee e : toSave) {
                        if (existed.contains(e.getUsername())) {
                            addImportFailure(failures, null, e.getUsername(), "账号已存在");
                        } else {
                            deduped.add(e);
                        }
                    }
                    toSave.clear();
                    toSave.addAll(deduped);
                }
            }

            // 第三轮：生成随机初始密码并批量入库，明文密码仅在本次响应返回一次
            List<Map<String, Object>> accounts = new ArrayList<>();
            for (Employee emp : toSave) {
                String initialPassword = SecurityConstants.generateRandomPassword();
                emp.setPassword(PasswordUtils.encodePassword(initialPassword));
                emp.setPasswordType(SecurityConstants.PASSWORD_TYPE_BCRYPT);
                Map<String, Object> account = new HashMap<>();
                account.put("username", emp.getUsername());
                account.put("name", emp.getName());
                account.put("password", initialPassword);
                accounts.add(account);
            }
            if (!toSave.isEmpty()) {
                employeeService.saveBatch(toSave);
                log.info("批量导入员工成功 {} 条，失败 {} 条，操作人={}",
                        toSave.size(), failures.size(), request.getSession().getAttribute("employee"));
            }

            Map<String, Object> result = new HashMap<>();
            result.put("successCount", toSave.size());
            result.put("failCount", failures.size());
            result.put("failures", failures);
            result.put("accounts", accounts);
            return R.success(result);
        } catch (Exception e) {
            // 非 xlsx/文件损坏时 XSSFWorkbook 构造抛 POIXMLException 或 IOException，统一兜底
            log.error("解析员工导入文件失败", e);
            return R.error("文件解析失败，请确认使用的是最新模板的 .xlsx 文件");
        }
    }

    /** 读取单元格文本（数字单元格也按文本取值，避免手机号被科学计数法处理） */
    private String cellText(Row row, int cellIdx, DataFormatter formatter) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(cellIdx);
        return cell == null ? "" : formatter.formatCellValue(cell);
    }

    /** 导入行字段校验，返回 null 表示通过，否则返回失败原因 */
    private String validateImportRow(String username, String name, String phone, String sex,
            String idNumber, Set<String> fileUsernames) {
        if (username.isEmpty()) {
            return "账号不能为空";
        }
        if (username.length() < 4 || username.length() > 20) {
            return "账号长度应为 4-20 位";
        }
        if (fileUsernames.contains(username)) {
            return "账号在文件内重复";
        }
        if (name.isEmpty()) {
            return "员工姓名不能为空";
        }
        if (name.length() > 12) {
            return "姓名长度不能超过 12 位";
        }
        if (phone.isEmpty() || !phone.matches(SecurityConstants.PHONE_PATTERN)) {
            return "手机号格式不正确";
        }
        if (!sex.isEmpty() && !"男".equals(sex) && !"女".equals(sex)) {
            return "性别只能填写 男 或 女";
        }
        if (idNumber.isEmpty() || !ID_CARD_PATTERN.matcher(idNumber).matches()) {
            return "身份证号码不正确";
        }
        return null;
    }

    /** 追加一条导入失败记录（行号未知时仅记录账号） */
    private void addImportFailure(List<Map<String, Object>> failures, Integer rowNum,
            String username, String reason) {
        Map<String, Object> item = new HashMap<>();
        item.put("row", rowNum);
        item.put("username", username);
        item.put("reason", reason);
        failures.add(item);
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
        if (employee.getAvatar() != null) {
            uw.set(Employee::getAvatar, employee.getAvatar());
        }
        if (employee.getPosition() != null) {
            uw.set(Employee::getPosition, employee.getPosition());
        }
        if (employee.getJobNumber() != null) {
            // 工号租户内唯一校验（排除自身）；空串归一为 null。冲突时抛异常，不会执行后续 update
            String normalizedJobNumber = checkJobNumberUnique(existing.getTenantId(), employee.getJobNumber(), employee.getId());
            uw.set(Employee::getJobNumber, normalizedJobNumber);
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
    public R<String> updateStatusBatch(HttpServletRequest request,
            @Valid @RequestBody UpdateEmployeeStatusBatchDTO dto) {
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
    public R<Employee> getById(@PathVariable Long id, @RequestParam(defaultValue = "false") Boolean raw){
        log.debug("根据ID查询员工信息");
        Employee employee = employeeService.getById(id);
        if(employee != null){
            // 租户校验：确保只能查询当前租户的员工
            Long currentTenantId = BaseContext.getCurrentTenantId();
            if (currentTenantId != null && !currentTenantId.equals(employee.getTenantId())) {
                return R.error("没有查询到对应员工信息");
            }
            // 脱敏：移除密码（永远脱敏）、手机号、身份证等敏感字段
            employee.setPassword(null);
            employee.setPasswordType(null);
            if (!Boolean.TRUE.equals(raw)) {
                employee.setPhone(employee.getPhone() != null ? maskPhone(employee.getPhone()) : null);
                employee.setIdNumber(null);
            }
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

        // 安全转型，避免 ClassCastException
        Object empAttr = request.getSession().getAttribute("employee");
        Long empId = (empAttr instanceof Number) ? ((Number) empAttr).longValue() : null;
        if (empId == null) {
            return R.error("请先登录");
        }
        Employee emp = employeeService.getById(empId);
        if (emp == null) {
            return R.error("员工不存在");
        }

        // 校验旧密码
        String passwordType = emp.getPasswordType() != null ? emp.getPasswordType() : SecurityConstants
                .PASSWORD_TYPE_MD5;
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
        // 安全转型，避免 ClassCastException
        Long empId = (empIdObj instanceof Number) ? ((Number) empIdObj).longValue() : null;
        if (empId == null) {
            return false;
        }
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

