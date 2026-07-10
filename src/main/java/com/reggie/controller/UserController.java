package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.SendMsgDTO;
import com.reggie.dto.UserLoginDTO;
import com.reggie.entity.User;
import com.reggie.service.UserService;
import com.reggie.utils.SMSUtils;
import com.reggie.common.BruteForceProtectionFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "用户管理", description = "C端用户管理")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private BruteForceProtectionFilter bruteForceProtectionFilter;

    /**
     * 当前激活的Spring Profile（dev / prod），用于区分开发/生产环境
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 短信签名（从配置文件注入，生产环境需配置）
     */
    @Value("${reggie.sms.sign-name:瑞吉外卖}")
    private String smsSignName;

    /**
     * 短信模板编码（从配置文件注入，生产环境需配置）
     */
    @Value("${reggie.sms.template-code:}")
    private String smsTemplateCode;

    /**
     * 验证码有效期（5分钟，单位：毫秒）
     */
    private static final long CODE_EXPIRE_MS = 5 * 60 * 1000;

    /**
     * 验证码发送间隔（60秒，同一手机号）
     */
    private static final long CODE_INTERVAL_MS = 60 * 1000;

    /** 安全随机数生成器 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 手机号正则 */
    private static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 发送短信验证码
     *
     * @param dto 发送短信请求
     * @param session HTTP会话
     * @return 发送结果
     */
    @com.reggie.common.RateLimit(maxRequestsPerSecond = 3)
    @PostMapping("/sendMsg")
    @Operation(summary = "发送短信验证码", description = "向指定手机号发送登录验证码，60秒内不可重复发送")
    public R<String> sendMsg(@Valid @RequestBody SendMsgDTO dto, HttpSession session){
        String phone = dto.getPhone();

        if(phone == null || phone.isEmpty()){
            return R.error("手机号不能为空");
        }
        if(!phone.matches(PHONE_REGEX)){
            return R.error("手机号格式不正确");
        }

        Long lastSendTime = (Long) session.getAttribute("smsCode_" + phone + "_time");
        if(lastSendTime != null && System.currentTimeMillis() - lastSendTime < CODE_INTERVAL_MS){
            long remaining = (CODE_INTERVAL_MS - (System.currentTimeMillis() - lastSendTime)) / 1000;
            return R.error("请" + remaining + "秒后再试");
        }

        int code = SECURE_RANDOM.nextInt(9000) + 1000;
        String codeStr = String.valueOf(code);

        // 存储验证码及生成时间到Session
        session.setAttribute("smsCode_" + phone, codeStr);
        session.setAttribute("smsCode_" + phone + "_time", System.currentTimeMillis());

        if("dev".equals(activeProfile)){
            // 开发环境：在控制台打印完整验证码，方便调试
            log.info("【开发环境】验证码已生成 -> 手机号：{}，验证码：{}", phone, codeStr);
        } else {
            // 生产环境：仅记录脱敏日志；若配置了短信模板则通过阿里云发送真实短信
            log.info("【生产环境】验证码已生成 -> 手机号：{}，验证码：****", phone);
            if(smsTemplateCode != null && !smsTemplateCode.isEmpty()){
                try {
                    SMSUtils.sendMessage(smsSignName, smsTemplateCode, phone, codeStr);
                } catch (Exception e){
                    log.error("短信发送失败，phone={}, error={}", phone, e.getMessage());
                    // 短信发送失败时清除Session中的验证码，避免无效验证码残留
                    session.removeAttribute("smsCode_" + phone);
                    session.removeAttribute("smsCode_" + phone + "_time");
                    return R.error("短信发送失败，请稍后再试");
                }
            } else {
                log.warn("【生产环境】短信模板未配置，验证码不会实际发送。请在application-prod.yml中配置reggie.sms.template-code");
            }
        }
        return R.success("短信发送成功");
    }

    /**
     * 用户登录
     *
     * @param dto 用户登录信息
     * @param session HTTP会话
     * @return 用户信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "手机号+验证码登录，新用户自动注册，支持防暴力破解保护")
    public R<User> login(@Valid @RequestBody UserLoginDTO dto, HttpSession session){
        String phone = dto.getPhone();
        String code = dto.getCode();

        if (phone == null || phone.isEmpty()) {
            return R.error("手机号不能为空");
        }
        if (code == null || code.isEmpty()) {
            return R.error("验证码不能为空");
        }

        String sessionCode = (String) session.getAttribute("smsCode_" + phone);
        Long codeTime = (Long) session.getAttribute("smsCode_" + phone + "_time");

        if (sessionCode == null || codeTime == null) {
            recordLoginFailure(session, phone);
            return R.error("请先获取验证码");
        }
        if (System.currentTimeMillis() - codeTime > CODE_EXPIRE_MS) {
            session.removeAttribute("smsCode_" + phone);
            session.removeAttribute("smsCode_" + phone + "_time");
            recordLoginFailure(session, phone);
            return R.error("验证码已过期，请重新获取");
        }
        if (!sessionCode.equals(code)) {
            recordLoginFailure(session, phone);
            return R.error("验证码错误");
        }

        // 验证通过，清除Session中的验证码（一次性使用）
        session.removeAttribute("smsCode_" + phone);
        session.removeAttribute("smsCode_" + phone + "_time");

        // 登录成功，重置失败计数
        resetLoginAttempts(session, phone);

        log.info("用户登录，手机号={}", phone);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = userService.getOne(queryWrapper);

        if(user == null){
            user = new User();
            user.setPhone(phone);
            user.setStatus(1);
            userService.save(user);
        }

        session.setAttribute("user", user.getId());
        if (user.getTenantId() != null) {
            session.setAttribute("tenantId", user.getTenantId());
        }
        return R.success(user);
    }

    /**
     * 记录登录失败（调用暴力破解防护）
     * @param session HTTP会话
     * @param phone 手机号
     */
    private void recordLoginFailure(HttpSession session, String phone) {
        if (bruteForceProtectionFilter != null) {
            bruteForceProtectionFilter.recordFailedAttempt(phone);
        }
    }

    /**
     * 重置登录失败计数（调用暴力破解防护）
     * @param session HTTP会话
     * @param phone 手机号
     */
    private void resetLoginAttempts(HttpSession session, String phone) {
        if (bruteForceProtectionFilter != null) {
            bruteForceProtectionFilter.resetFailedAttempts(phone);
        }
    }

    /**
     * 用户退出
     *
     * @param session HTTP会话
     * @return 退出结果
     */
    @PostMapping("/loginout")
    @Operation(summary = "用户退出", description = "退出当前登录账号，清除会话信息")
    public R<String> loginout(HttpSession session) {
        session.removeAttribute("user");
        return R.success("退出成功");
    }

    /**
     * 获取当前登录用户信息
     *
     * @param session HTTP会话
     * @return 用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息", description = "返回当前登录用户的详细信息，需携带有效会话")
    public R<User> getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("user");
        if (userId == null) {
            return R.error("NOTLOGIN");
        }
        User user = userService.getById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }
        return R.success(user);
    }

    /**
     * 用户分页查询
     *
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 姓名
     * @param phone 手机号
     * @param status 状态：0禁用 1正常
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "用户分页查询", description = "分页查询用户列表，支持按姓名、手机号模糊搜索和状态筛选，自动过滤当前租户数据")
    public R<Page<User>> page(
            @Parameter(name = "page", description = "页码", required = true, example = "1") int page,
            @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10") int pageSize,
            @Parameter(name = "name", description = "姓名") String name,
            @Parameter(name = "phone", description = "手机号") String phone,
            @Parameter(name = "status", description = "状态：0禁用 1正常") Integer status) {

        log.info("用户分页查询：page={}, pageSize={}, name={}, phone={}, status={}",
            page, pageSize, name, phone, status);

        Page<User> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null && !name.isEmpty(), User::getName, name)
                    .like(phone != null && !phone.isEmpty(), User::getPhone, phone)
                    .eq(status != null, User::getStatus, status)
                    .orderByDesc(User::getId);

        // 多租户隔离：仅查询当前租户的用户
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(User::getTenantId, tenantId);
        }

        userService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 修改用户状态
     *
     * @param id 用户ID
     * @param status 状态：0禁用 1正常
     * @return 操作结果
     */
    @PutMapping("/status")
    @Operation(summary = "修改用户状态", description = "启用或禁用指定用户账号，自动校验租户权限")
    public R<String> updateStatus(
            @Parameter(name = "id", description = "用户ID", required = true) Long id,
            @Parameter(name = "status", description = "状态：0禁用 1正常", required = true) Integer status) {

        log.info("修改用户状态：id={}, status={}", id, status);

        User user = userService.getById(id);
        if (user == null) {
            return R.error("用户不存在");
        }

        // 多租户校验：确保只能操作当前租户的用户
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(user.getTenantId())) {
            return R.error("无权操作其他租户的用户");
        }

        user.setStatus(status);
        boolean success = userService.updateById(user);

        return success ? R.success("操作成功") : R.error("操作失败");
    }

    /**
     * 用户统计
     *
     * @return 统计信息
     */
    @GetMapping("/stats")
    @Operation(summary = "用户统计", description = "获取用户总数、正常数、已禁用数、本月新增数")
    public R<Map<String, Object>> stats() {
        Long tenantId = BaseContext.getCurrentTenantId();

        LambdaQueryWrapper<User> totalQw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<User> activeQw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<User> disabledQw = new LambdaQueryWrapper<>();
        LambdaQueryWrapper<User> newQw = new LambdaQueryWrapper<>();

        if (tenantId != null) {
            totalQw.eq(User::getTenantId, tenantId);
            activeQw.eq(User::getTenantId, tenantId);
            disabledQw.eq(User::getTenantId, tenantId);
            newQw.eq(User::getTenantId, tenantId);
        }

        activeQw.eq(User::getStatus, 1);
        disabledQw.eq(User::getStatus, 0);

        // 本月新增：createTime >= 当月1日
        java.time.LocalDateTime monthStart = java.time.LocalDate.now()
                .withDayOfMonth(1).atStartOfDay();
        newQw.ge(User::getCreateTime, monthStart);

        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", userService.count(totalQw));
        result.put("activeUsers", userService.count(activeQw));
        result.put("disabledUsers", userService.count(disabledQw));
        result.put("newUsersThisMonth", userService.count(newQw));
        return R.success(result);
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "删除用户", description = "删除指定用户，自动校验租户权限")
    public R<String> delete(@Parameter(name = "id", description = "用户ID", required = true) Long id) {
        log.info("删除用户：id={}", id);

        User user = userService.getById(id);
        if (user == null) {
            return R.error("删除失败，用户不存在");
        }

        // 多租户校验：确保只能删除当前租户的用户
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(user.getTenantId())) {
            return R.error("无权删除其他租户的用户");
        }

        boolean success = userService.removeById(id);
        return success ? R.success("删除成功") : R.error("删除失败");
    }

}
