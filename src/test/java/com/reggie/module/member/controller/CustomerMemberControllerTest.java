package com.reggie.module.member.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.RechargeRecord;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.RechargeRecordService;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CustomerMemberController 纯 Mockito 单元测试。
 * 覆盖自助开通、发起充值、充值状态查询的正常路径与登录/归属/参数校验。
 *
 * @author reggie
 * @since 2026-09-21
 */
@ExtendWith(MockitoExtension.class)
class CustomerMemberControllerTest {

    @Mock
    private MemberService memberService;

    @Mock
    private RechargeRecordService rechargeRecordService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CustomerMemberController controller;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.remove();
    }

    // ---------- 自助开通 ----------
    @Test
    void open_loggedIn_callsRegisterForUserWithRealInfo() {
        User user = new User();
        user.setId(1L);
        user.setPhone("13800138000");
        user.setName("张三");
        when(userService.getById(1L)).thenReturn(user);
        Member member = new Member();
        member.setId(10L);
        when(memberService.registerForUser(1L, "13800138000", "张三")).thenReturn(member);

        R<Member> r = controller.open();
        assertEquals(member, r.getData());
        verify(memberService).registerForUser(1L, "13800138000", "张三");
    }

    @Test
    void open_notLoggedIn_throws() {
        BaseContext.remove();
        assertThrows(CustomException.class, () -> controller.open());
        verify(memberService, never()).registerForUser(any(), any(), any());
    }

    @Test
    void open_userMissing_throws() {
        when(userService.getById(1L)).thenReturn(null);
        CustomException ex = assertThrows(CustomException.class, () -> controller.open());
        assertEquals("用户信息不存在，请重新登录", ex.getMessage());
    }

    // ---------- 发起充值 ----------
    @Test
    void createRecharge_numberAmount_ok() {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", 100);
        body.put("paymentMethod", "WECHAT");
        RechargeRecord record = buildRecord("RC1", "PENDING", "100", "WECHAT");
        record.setUserId(1L);
        when(rechargeRecordService.createPendingRecharge(eq(1L), eq(new BigDecimal("100.0")), eq("WECHAT")))
                .thenReturn(record);

        R<Map<String, Object>> r = controller.createRecharge(body);
        assertEquals("RC1", r.getData().get("rechargeNo"));
        assertEquals("PENDING", r.getData().get("status"));
    }

    @Test
    void createRecharge_stringAmount_ok() {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", "200.50");
        RechargeRecord record = buildRecord("RC2", "PENDING", "200.50", "WECHAT");
        when(rechargeRecordService.createPendingRecharge(eq(1L), eq(new BigDecimal("200.50")), any()))
                .thenReturn(record);

        R<Map<String, Object>> r = controller.createRecharge(body);
        assertEquals("RC2", r.getData().get("rechargeNo"));
    }

    @Test
    void createRecharge_amountMissing_throws() {
        CustomException ex = assertThrows(CustomException.class,
                () -> controller.createRecharge(new HashMap<>()));
        assertEquals("请填写充值金额", ex.getMessage());
    }

    @Test
    void createRecharge_badAmount_throws() {
        Map<String, Object> body = new HashMap<>();
        body.put("amount", "abc");
        CustomException ex = assertThrows(CustomException.class, () -> controller.createRecharge(body));
        assertEquals("充值金额格式不正确", ex.getMessage());
    }

    @Test
    void createRecharge_notLoggedIn_throws() {
        BaseContext.remove();
        Map<String, Object> body = new HashMap<>();
        body.put("amount", 50);
        assertThrows(CustomException.class, () -> controller.createRecharge(body));
    }

    // ---------- 充值状态 ----------
    @Test
    void rechargeStatus_owned_ok() {
        RechargeRecord record = buildRecord("RC1", "SUCCESS", "100", "WECHAT");
        record.setUserId(1L);
        when(rechargeRecordService.getByRechargeNo("RC1")).thenReturn(record);

        R<Map<String, Object>> r = controller.rechargeStatus("RC1");
        assertEquals("SUCCESS", r.getData().get("status"));
    }

    @Test
    void rechargeStatus_missing_throws() {
        when(rechargeRecordService.getByRechargeNo("RCX")).thenReturn(null);
        CustomException ex = assertThrows(CustomException.class, () -> controller.rechargeStatus("RCX"));
        assertEquals("充值单不存在", ex.getMessage());
    }

    @Test
    void rechargeStatus_otherUser_throws() {
        RechargeRecord record = buildRecord("RC1", "PENDING", "100", "WECHAT");
        record.setUserId(2L);
        when(rechargeRecordService.getByRechargeNo("RC1")).thenReturn(record);

        CustomException ex = assertThrows(CustomException.class, () -> controller.rechargeStatus("RC1"));
        assertEquals("充值单不存在或无权查看", ex.getMessage());
    }

    @Test
    void rechargeStatus_notLoggedIn_throws() {
        BaseContext.remove();
        assertThrows(CustomException.class, () -> controller.rechargeStatus("RC1"));
    }

    private RechargeRecord buildRecord(String no, String status, String amount, String pm) {
        RechargeRecord record = new RechargeRecord();
        record.setRechargeNo(no);
        record.setStatus(status);
        record.setAmount(new BigDecimal(amount));
        record.setPaymentMethod(pm);
        return record;
    }
}
