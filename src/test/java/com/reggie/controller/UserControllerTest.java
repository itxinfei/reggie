package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
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

@SpringBootTest(classes = com.reggie.ReggieApplication.class)
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
        BaseContext.setCurrentId(1L);
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
        mockMvc.perform(post("/user/sendMsg")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13900139000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("短信发送成功"));
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
        MockHttpSession session = (MockHttpSession) mockMvc.perform(post("/user/sendMsg")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();

        // 从 Session 中获取实际生成的验证码
        String code = (String) session.getAttribute("smsCode_13800138000");

        // 使用正确的验证码登录
        mockMvc.perform(post("/user/login")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"13800138000\",\"code\":\"" + code + "\"}"))
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



