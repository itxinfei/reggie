package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.SendMsgDTO;
import com.reggie.dto.UserLoginDTO;
import com.reggie.entity.User;
import com.reggie.service.UserService;
import com.reggie.utils.SMSUtils;
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

@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "用户管理", description = "C端用户管理")
public class UserController {

    @Autowired
    private UserService userService;

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

    @PostMapping("/sendMsg")
    @Operation(summary = "发送短信验证码")
    public R<String> sendMsg(@Valid @RequestBody SendMsgDTO dto, HttpSession session){
        String phone = dto.getPhone();

        // 修改点：手机号格式校验
        if(phone == null || phone.isEmpty()){
            return R.error("手机号不能为空");
        }
        if(!phone.matches(PHONE_REGEX)){
            return R.error("手机号格式不正确");
        }

        // 修改点：同一手机号发送频率限制（60秒内只能发一次）
        Long lastSendTime = (Long) session.getAttribute("smsCode_" + phone + "_time");
        if(lastSendTime != null && System.currentTimeMillis() - lastSendTime < CODE_INTERVAL_MS){
            long remaining = (CODE_INTERVAL_MS - (System.currentTimeMillis() - lastSendTime)) / 1000;
            return R.error("请" + remaining + "秒后再试");
        }

        // 修改点：使用SecureRandom生成随机验证码
        int code = SECURE_RANDOM.nextInt(9000) + 1000;
        String codeStr = String.valueOf(code);

        // 存储验证码及生成时间到Session
        session.setAttribute("smsCode_" + phone, codeStr);
        session.setAttribute("smsCode_" + phone + "_time", System.currentTimeMillis());

        // 修改点：根据环境区分验证码处理策略
        if("dev".equals(activeProfile)){
            // 开发环境：在控制台打印完整验证码，方便调试
            log.info("【开发环境】验证码已生成 -> 手机号：{}，验证码：{}", phone, codeStr);
        } else {
            // 生产环境：仅记录脱敏日志；若配置了短信模板则通过阿里云发送真实短信
            log.info("【生产环境】验证码已生成 -> 手机号：{}，验证码：****", phone);
            // 修改点：对接真实短信服务（需在application-prod.yml中配置sign-name和template-code）
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

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public R<User> login(@Valid @RequestBody UserLoginDTO dto, HttpSession session){
        String phone = dto.getPhone();
        String code = dto.getCode();

        if (phone == null || phone.isEmpty()) {
            return R.error("手机号不能为空");
        }
        if (code == null || code.isEmpty()) {
            return R.error("验证码不能为空");
        }

        // 修改点：从Session校验验证码，修复原有空壳校验问题
        String sessionCode = (String) session.getAttribute("smsCode_" + phone);
        Long codeTime = (Long) session.getAttribute("smsCode_" + phone + "_time");

        if (sessionCode == null || codeTime == null) {
            return R.error("请先获取验证码");
        }
        if (System.currentTimeMillis() - codeTime > CODE_EXPIRE_MS) {
            session.removeAttribute("smsCode_" + phone);
            session.removeAttribute("smsCode_" + phone + "_time");
            return R.error("验证码已过期，请重新获取");
        }
        if (!sessionCode.equals(code)) {
            return R.error("验证码错误");
        }

        // 验证通过，清除Session中的验证码（一次性使用）
        session.removeAttribute("smsCode_" + phone);
        session.removeAttribute("smsCode_" + phone + "_time");

        log.info("用户登录，手机号={}", phone);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = userService.getOne(queryWrapper);

        // 修改点：新用户注册时设置status=1（正常），避免DB默认值0导致用户被禁用
        if(user == null){
            user = new User();
            user.setPhone(phone);
            user.setStatus(1);
            userService.save(user);
        }

        session.setAttribute("user", user.getId());
        return R.success(user);
    }

    @PostMapping("/loginout")
    @Operation(summary = "用户退出")
    public R<String> loginout(HttpSession session) {
        session.removeAttribute("user");
        return R.success("退出成功");
    }

    // 修改点：新增获取当前登录用户信息接口，供前端个人中心使用
    @GetMapping("/info")
    @Operation(summary = "获取当前登录用户信息")
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

    @GetMapping("/page")
    @Operation(summary = "用户分页查询")
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

        userService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }

    @PutMapping("/status")
    @Operation(summary = "修改用户状态")
    public R<String> updateStatus(
            @Parameter(name = "id", description = "用户ID", required = true) Long id,
            @Parameter(name = "status", description = "状态：0禁用 1正常", required = true) Integer status) {

        log.info("修改用户状态：id={}, status={}", id, status);

        User user = userService.getById(id);
        if (user == null) {
            return R.error("用户不存在");
        }

        user.setStatus(status);
        boolean success = userService.updateById(user);

        return success ? R.success("操作成功") : R.error("操作失败");
    }

    @DeleteMapping
    @Operation(summary = "删除用户")
    public R<String> delete(@Parameter(name = "id", description = "用户ID", required = true) Long id) {
        log.info("删除用户：id={}", id);
        boolean success = userService.removeById(id);
        return success ? R.success("删除成功") : R.error("删除失败，用户不存在");
    }

}
