package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.entity.User;
import com.reggie.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "用户管理", description = "C端用户管理")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/sendMsg")
    @Operation(summary = "发送短信验证码")
    public R<String> sendMsg(@RequestBody Map<String, Object> map, HttpSession session){
        String phone = map.get("phone") != null ? map.get("phone").toString() : null;
        if(phone != null && !phone.isEmpty()){
            log.info("发送验证码到手机：{}", phone);
            return R.success("短信发送成功");
        }
        return R.error("手机号不能为空");
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public R<User> login(@RequestBody Map<String, Object> map, HttpSession session){
        String phone = map.get("phone") != null ? map.get("phone").toString() : null;
        String code = map.get("code") != null ? map.get("code").toString() : null;

        if (phone == null || phone.isEmpty()) {
            return R.error("手机号不能为空");
        }
        if (code == null || code.isEmpty()) {
            return R.error("验证码不能为空");
        }

        log.info("用户登录，手机号={}", phone);

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getPhone, phone);
        User user = userService.getOne(queryWrapper);

        if(user == null){
            user = new User();
            user.setPhone(phone);
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
                    .orderByDesc(User::getCreatedTime);

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
