package com.reggie.module.customer.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.module.customer.model.Complaint;
import com.reggie.module.customer.model.CsMessage;
import com.reggie.module.customer.model.CsSession;
import com.reggie.module.customer.service.CustomerServiceInterface;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 顾客侧客服 / 投诉控制器单元测试（纯 Mockito，不连库、不起 Spring）。
 * 重点覆盖：会话 / 投诉归属校验（防同租户越权互访）、发送消息强制以用户身份、
 * 投诉白名单字段与真实用户身份落库、各种内容与状态校验。
 */
@ExtendWith(MockitoExtension.class)
class CustomerPortalControllerTest {

    @Mock
    private CustomerServiceInterface customerService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CustomerPortalController controller;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        BaseContext.setCurrentId(null);
        BaseContext.setCurrentTenantId(null);
    }

    private User user(long id, String name, String phone) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setPhone(phone);
        return u;
    }

    private CsSession session(long id, long userId, int status) {
        CsSession s = new CsSession();
        s.setId(id);
        s.setUserId(userId);
        s.setUserName("张三");
        s.setStatus(status);
        return s;
    }

    private Map<String, Object> body(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    // ---------- 会话 ----------

    @Test
    void createSession_loggedIn_usesRealUserName() {
        when(userService.getById(1L)).thenReturn(user(1L, "张三", "13800138000"));
        CsSession created = session(10, 1, CsSession.STATUS_WAITING);
        when(customerService.createSession(1L, "张三", 1, null, 1L)).thenReturn(created);

        R<CsSession> r = controller.createSession(1, null);

        verify(customerService).createSession(eq(1L), eq("张三"), eq(1), eq(null), eq(1L));
        assertEquals(created, r.getData());
    }

    @Test
    void createSession_notLoggedIn_throws() {
        BaseContext.setCurrentId(null);
        assertThrows(CustomException.class, () -> controller.createSession(1, null));
        verify(customerService, never()).createSession(any(), any(), any(), any(), any());
    }

    @Test
    void listMySessions_filtersByCurrentUser() {
        when(customerService.getSessionList(null, 1L)).thenReturn(Arrays.asList(
                session(1, 1, CsSession.STATUS_IN_PROGRESS),
                session(2, 999, CsSession.STATUS_IN_PROGRESS),
                session(3, 1, CsSession.STATUS_CLOSED)));

        R<List<CsSession>> r = controller.listMySessions(null);

        assertEquals(2, r.getData().size());
        assertEquals(1L, r.getData().get(0).getUserId());
    }

    @Test
    void getSession_otherOwned_throws() {
        when(customerService.getSessionById(5L)).thenReturn(session(5, 999, CsSession.STATUS_IN_PROGRESS));
        assertThrows(CustomException.class, () -> controller.getSession(5L));
    }

    @Test
    void getSession_missing_throws() {
        when(customerService.getSessionById(5L)).thenReturn(null);
        assertThrows(CustomException.class, () -> controller.getSession(5L));
    }

    @Test
    void closeSession_owned_invokesClose() {
        when(customerService.getSessionById(5L)).thenReturn(session(5, 1, CsSession.STATUS_IN_PROGRESS));
        when(customerService.closeSession(5L, 4, "满意")).thenReturn(true);

        R<String> r = controller.closeSession(5L, 4, "满意");

        verify(customerService).closeSession(5L, 4, "满意");
        assertEquals("会话已关闭", r.getData());
    }

    // ---------- 消息 ----------

    @Test
    void sendMessage_ownedText_forcedAsUser() {
        when(customerService.getSessionById(5L)).thenReturn(session(5, 1, CsSession.STATUS_IN_PROGRESS));

        controller.sendMessage(body("sessionId", 5, "content", "你好"));

        verify(customerService).sendMessage(eq(5L), eq(CsMessage.SENDER_USER), eq(1L), eq("张三"),
                eq(CsMessage.TYPE_TEXT), eq("你好"), eq(null), eq(1L));
    }

    @Test
    void sendMessage_otherOwnedSession_throwsAndNeverSends() {
        when(customerService.getSessionById(5L)).thenReturn(session(5, 999, CsSession.STATUS_IN_PROGRESS));

        assertThrows(CustomException.class,
                () -> controller.sendMessage(body("sessionId", 5, "content", "你好")));

        verify(customerService, never()).sendMessage(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void sendMessage_closedSession_throws() {
        when(customerService.getSessionById(5L)).thenReturn(session(5, 1, CsSession.STATUS_CLOSED));
        assertThrows(CustomException.class,
                () -> controller.sendMessage(body("sessionId", 5, "content", "你好")));
    }

    @Test
    void sendMessage_emptyContent_throws() {
        when(customerService.getSessionById(5L)).thenReturn(session(5, 1, CsSession.STATUS_IN_PROGRESS));
        assertThrows(CustomException.class,
                () -> controller.sendMessage(body("sessionId", 5, "content", "   ")));
    }

    @Test
    void sendMessage_imageTypeWithoutImage_throws() {
        when(customerService.getSessionById(5L)).thenReturn(session(5, 1, CsSession.STATUS_IN_PROGRESS));
        assertThrows(CustomException.class,
                () -> controller.sendMessage(body("sessionId", 5, "messageType", 2, "content", "x")));
    }

    @Test
    void listMessages_owned_returnsList() {
        when(customerService.getSessionById(5L)).thenReturn(session(5, 1, CsSession.STATUS_IN_PROGRESS));
        controller.listMessages(5L);
        verify(customerService).getSessionMessages(5L);
    }

    @Test
    void unread_andMarkRead_ownedUseUserPerspective() {
        when(customerService.getSessionById(5L)).thenReturn(session(5, 1, CsSession.STATUS_IN_PROGRESS));
        when(customerService.markMessagesAsRead(5L, 1)).thenReturn(true);

        controller.unread(5L);
        controller.markRead(5L);

        verify(customerService).getUnreadMessageCount(5L, 1);
        verify(customerService).markMessagesAsRead(5L, 1);
    }

    // ---------- 投诉 ----------

    @Test
    void createComplaint_setsRealIdentityAndIgnoresClientPrivilegedFields() {
        when(userService.getById(1L)).thenReturn(user(1L, "张三", "13800138000"));

        // 恶意携带 status / handlerId / compensationAmount，Controller 走 Map 白名单应全部忽略
        Map<String, Object> req = body(
                "complaintType", 2, "title", "配送问题", "content", "送餐太慢",
                "status", 99, "handlerId", 888, "compensationAmount", 999);
        controller.createComplaint(req);

        ArgumentCaptor<Complaint> captor = ArgumentCaptor.forClass(Complaint.class);
        verify(customerService).createComplaint(captor.capture());
        Complaint c = captor.getValue();
        assertEquals(1L, c.getUserId());
        assertEquals("张三", c.getUserName());
        assertEquals("13800138000", c.getUserPhone());
        assertEquals(Complaint.TYPE_DELIVERY_SERVICE, c.getComplaintType());
        // 特权字段不接受客户端注入（Service 随后强制 PENDING）
        assertNull(c.getStatus());
        assertNull(c.getHandlerId());
        assertNull(c.getCompensationAmount());
    }

    @Test
    void createComplaint_blankContent_throws() {
        assertThrows(CustomException.class,
                () -> controller.createComplaint(body("complaintType", 1, "content", "")));
        verify(customerService, never()).createComplaint(any());
    }

    @Test
    void listMyComplaints_filtersByCurrentUser() {
        Complaint mine = new Complaint();
        mine.setId(1L);
        mine.setUserId(1L);
        Complaint other = new Complaint();
        other.setId(2L);
        other.setUserId(999L);
        when(customerService.getComplaintList(null, null, 1L)).thenReturn(Arrays.asList(mine, other));

        R<List<Complaint>> r = controller.listMyComplaints(null, null);

        assertEquals(1, r.getData().size());
        assertEquals(1L, r.getData().get(0).getUserId());
    }

    @Test
    void getComplaint_otherOwned_throws() {
        Complaint c = new Complaint();
        c.setId(5L);
        c.setUserId(999L);
        when(customerService.getComplaintById(5L)).thenReturn(c);
        assertThrows(CustomException.class, () -> controller.getComplaint(5L));
    }

    @Test
    void rateComplaint_owned_validatesRangeAndRates() {
        Complaint c = new Complaint();
        c.setId(5L);
        c.setUserId(1L);
        when(customerService.getComplaintById(5L)).thenReturn(c);
        when(customerService.rateComplaint(5L, 5, "很好")).thenReturn(true);

        assertThrows(CustomException.class, () -> controller.rateComplaint(5L, 6, null));
        R<String> r = controller.rateComplaint(5L, 5, "很好");

        verify(customerService).rateComplaint(5L, 5, "很好");
        assertEquals("评价已提交", r.getData());
    }

    @Test
    void rateComplaint_otherOwned_throws() {
        Complaint c = new Complaint();
        c.setId(5L);
        c.setUserId(999L);
        when(customerService.getComplaintById(5L)).thenReturn(c);

        assertThrows(CustomException.class, () -> controller.rateComplaint(5L, 5, null));
        verify(customerService, never()).rateComplaint(any(), any(), any());
    }
}
