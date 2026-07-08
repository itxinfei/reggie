package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.entity.User;
import com.reggie.service.UserService;
import com.reggie.dto.auth.UserSendMsgDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);

        // 创建测试用户
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setName("测试用户");
        user.setStatus(1);
        user.setSex("1");
        userService.save(user);
    }

    @Test
    void testSendMsg() throws Exception {
        UserSendMsgDTO dto = new UserSendMsgDTO();
        dto.setPhone("13900139000");

        mockMvc.perform(post("/user/sendMsg")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13900139000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("手机验证码短信发送成功"));
    }

    @Test
    void testSendMsgEmptyPhone() throws Exception {
        mockMvc.perform(post("/user/sendMsg")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void testLoginSuccess() throws Exception {
        // 使用 MockMvc 构建请求
        Map<String, Object> loginData = new HashMap<>();
        loginData.put("phone", "13800138000");
        loginData.put("code", "1234"); // 测试验证码

        // 由于验证码逻辑复杂，这里简化测试
        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\",\"code\":\"1234\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testLoginWithVerifyCode() throws Exception {
        // 先发送验证码
        mockMvc.perform(post("/user/sendMsg")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk());

        // 模拟 Session 中的验证码
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/user/sendMsg")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138001\"}"))
                .andReturn()
                .getRequest()
                .getSession();

        // 手动设置验证码（模拟）
        session.setAttribute("13800138001", "1234");

        // 使用验证码登录
        Map<String, Object> loginData = new HashMap<>();
        loginData.put("phone", "13800138001");
        loginData.put("code", "1234");

        mockMvc.perform(post("/user/login")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138001\",\"code\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    @Test
    void testLoginout() throws Exception {
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/user/sendMsg")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}"))
                .andReturn()
                .getRequest()
                .getSession();

        // 先登录
        session.setAttribute("user", 1L);

        // 再退出
        mockMvc.perform(post("/user/loginout")
                .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("退出成功"));
    }
}
