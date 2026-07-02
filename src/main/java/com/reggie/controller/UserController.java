package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.R;
import com.reggie.common.LogMaskUtils;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.entity.User;
import com.reggie.enums.UserStatus;
import com.reggie.service.UserService;
import com.reggie.utils.ValidateCodeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "用户管理", description = "移动端用户及验证码接口")
public class UserController {

    private static final int SMS_CODE_LENGTH = 4;

    @Autowired
    private UserService userService;

    /**
     * 发送手机短信验证码
     * @param user
     * @return
     */
    @PostMapping("/sendMsg")
    @Operation(summary = "发送短信验证码", description = "向指定手机号发送验证码")
    @Parameter(name = "user", description = "用户手机号信息", required = true)
    @RateLimit(maxRequestsPerSecond = 2, type = RateLimitType.IP)
    public R<String> sendMsg(@RequestBody User user, HttpSession session){
        //获取手机号
        String phone = user.getPhone();

        if(phone != null && !phone.isEmpty()){
            //生成随机的4位验证码
            String code = ValidateCodeUtils.generateValidateCode(SMS_CODE_LENGTH).toString();
            // 验证码已保存到Session，无需打印日志

            //调用阿里云提供的短信服务API完成发送短信
            //SMSUtils.sendMessage("瑞吉外卖","",phone,code);

            //需要将生成的验证码保存到Session
            session.setAttribute(phone,code);

            return R.success("手机验证码短信发送成功");
        }

        return R.error("短信发送失败");
    }

    /**
     * 移动端用户登录
     * @param map
     * @param session
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "手机号验证码登录")
    @Parameter(name = "map", description = "登录参数（手机号、验证码）", required = true)
    @RateLimit(maxRequestsPerSecond = 10, type = RateLimitType.IP)
    public R<User> login(@RequestBody Map<String, Object> map, HttpSession session){
        //获取手机号
        String phone = (String) map.get("phone");
        log.info("用户登录，手机号={}", LogMaskUtils.maskPhone(phone));

        //获取验证码
        String code = map.get("code").toString();

        //获取租户ID（前端可能不传，容错为null）
        Long tenantId = map.get("tenantId") != null ? Long.valueOf(map.get("tenantId").toString()) : null;

        //从Session中获取保存的验证码
        Object codeInSession = session.getAttribute(phone);

        //进行验证码的比对（页面提交的验证码和Session中保存的验证码比对）
        if(codeInSession != null && codeInSession.equals(code)){
            //如果能够比对成功，说明登录成功

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getPhone,phone);

            User user = userService.getOne(queryWrapper);
            if(user == null){
                //判断当前手机号对应的用户是否为新用户，如果是新用户就自动完成注册
                user = new User();
                user.setPhone(phone);
                user.setStatus(UserStatus.ENABLED.getValue());
                if (tenantId != null) {
                    user.setTenantId(tenantId);
                }
                userService.save(user);
            }
            session.setAttribute("user",user.getId());
            if (tenantId != null) {
                session.setAttribute("tenantId", tenantId);
            }
            return R.success(user);
        }
        return R.error("登录失败");
    }

    @PostMapping("/loginout")
    @Operation(summary = "用户退出", description = "退出当前登录账号")
    public R<String> loginout(HttpSession session) {
        session.removeAttribute("user");
        return R.success("退出成功");
    }

}
