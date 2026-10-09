package com.reggie.module.customer.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.module.customer.model.Complaint;
import com.reggie.module.customer.model.CsMessage;
import com.reggie.module.customer.model.CsSession;
import com.reggie.module.customer.service.CustomerServiceInterface;
import com.reggie.module.user.model.User;
import com.reggie.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * C 端顾客侧在线客服 / 投诉控制器。
 *
 * <p>与员工侧 {@link CustomerServiceController}（类级 @RequireEmployee）区分：
 * 本控制器不挂员工鉴权，仅依赖 {@code LoginCheckFilter} 的登录态拦截。
 * 现有 Service 的查询只按 tenantId 过滤、不校验归属，故此处统一补
 * "只能操作本人会话 / 投诉"的归属校验，防止同租户顾客越权互访；
 * 发送消息的 senderType 由服务端强制为用户，不接受前端传入。</p>
 */
@Slf4j
@RestController
@RequestMapping("/cs/portal")
@Tag(name = "C端客服", description = "顾客侧在线客服会话、消息与投诉")
public class CustomerPortalController {

    @Autowired
    private CustomerServiceInterface customerService;

    @Autowired
    private UserService userService;

    // ==================== 会话 ====================

    /** 创建（或发起）客服会话；顾客名取用户真实姓名，避免前端伪造 */
    @PostMapping("/session/create")
    @RateLimit
    @Operation(summary = "创建客服会话")
    public R<CsSession> createSession(
            @RequestParam(value = "sessionType", required = false, defaultValue = "1") Integer sessionType,
            @RequestParam(value = "orderId", required = false) Long orderId) {
        Long userId = requireUser();
        Long tenantId = BaseContext.getCurrentTenantId();
        User user = userService.getById(userId);
        String userName = (user != null && user.getName() != null) ? user.getName() : "用户" + userId;
        log.info("[客服-顾客] 创建会话: userId={}, sessionType={}, orderId={}", userId, sessionType, orderId);
        CsSession session = customerService.createSession(userId, userName, sessionType, orderId, tenantId);
        return R.success(session);
    }

    /** 我的会话列表（Service 按租户返回，此处再按当前用户过滤） */
    @GetMapping("/session/list")
    @Operation(summary = "我的会话列表")
    public R<List<CsSession>> listMySessions(
            @RequestParam(value = "status", required = false) Integer status) {
        Long userId = requireUser();
        Long tenantId = BaseContext.getCurrentTenantId();
        List<CsSession> list = customerService.getSessionList(status, tenantId).stream()
                .filter(s -> userId.equals(s.getUserId()))
                .collect(Collectors.toList());
        return R.success(list);
    }

    /** 会话详情（归属校验） */
    @GetMapping("/session/{id}")
    @Operation(summary = "会话详情")
    public R<CsSession> getSession(@PathVariable Long id) {
        return R.success(getOwnedSession(id));
    }

    /** 顾客主动关闭会话（归属校验） */
    @PostMapping("/session/{id}/close")
    @RateLimit
    @Operation(summary = "关闭会话")
    public R<String> closeSession(
            @PathVariable Long id,
            @RequestParam(value = "rating", required = false) Integer rating,
            @RequestParam(value = "feedback", required = false) String feedback) {
        getOwnedSession(id);
        boolean ok = customerService.closeSession(id, rating, feedback);
        return ok ? R.success("会话已关闭") : R.error("关闭失败");
    }

    // ==================== 消息 ====================

    /** 发送消息：归属校验 + 强制以用户身份 + 内容非空校验 */
    @PostMapping("/message/send")
    @RateLimit
    @Operation(summary = "发送消息")
    public R<CsMessage> sendMessage(@RequestBody Map<String, Object> body) {
        Long userId = requireUser();
        Long tenantId = BaseContext.getCurrentTenantId();
        Long sessionId = parseLong(body.get("sessionId"));
        if (sessionId == null) {
            throw new CustomException("缺少会话参数");
        }
        CsSession session = getOwnedSession(sessionId);
        if (session.getStatus() != null && session.getStatus() == CsSession.STATUS_CLOSED) {
            throw new CustomException("会话已关闭，请发起新会话");
        }
        Integer messageType = parseInteger(body.get("messageType"));
        if (messageType == null) {
            messageType = CsMessage.TYPE_TEXT;
        }
        String content = trimmed(body.get("content"), 1000);
        String imageUrl = trimmed(body.get("imageUrl"), 500);
        if (messageType == CsMessage.TYPE_IMAGE) {
            if (imageUrl == null) {
                throw new CustomException("图片内容不能为空");
            }
        } else if (content == null) {
            throw new CustomException("消息内容不能为空");
        }
        String senderName = session.getUserName() != null ? session.getUserName() : "用户" + userId;
        CsMessage message = customerService.sendMessage(sessionId, CsMessage.SENDER_USER, userId, senderName,
                messageType, content, imageUrl, tenantId);
        return R.success(message);
    }

    /** 会话消息列表（归属校验） */
    @GetMapping("/message/list/{sessionId}")
    @Operation(summary = "会话消息列表")
    public R<List<CsMessage>> listMessages(@PathVariable Long sessionId) {
        getOwnedSession(sessionId);
        return R.success(customerService.getSessionMessages(sessionId));
    }

    /** 当前用户在该会话的未读消息数（userType=1 用户视角；归属校验） */
    @GetMapping("/message/unread/{sessionId}")
    @Operation(summary = "未读消息数")
    public R<Integer> unread(@PathVariable Long sessionId) {
        getOwnedSession(sessionId);
        return R.success(customerService.getUnreadMessageCount(sessionId, 1));
    }

    /** 标记客服发来的消息为已读（userType=1；归属校验） */
    @PostMapping("/message/read/{sessionId}")
    @RateLimit
    @Operation(summary = "标记消息已读")
    public R<String> markRead(@PathVariable Long sessionId) {
        getOwnedSession(sessionId);
        boolean ok = customerService.markMessagesAsRead(sessionId, 1);
        return ok ? R.success("已标记已读") : R.error("操作失败");
    }

    // ==================== 投诉 ====================

    /** 提交投诉：白名单提取字段，服务端写入真实用户身份与租户，忽略客户端任何状态/处理人字段 */
    @PostMapping("/complaint/create")
    @RateLimit
    @Operation(summary = "提交投诉")
    public R<Complaint> createComplaint(@RequestBody Map<String, Object> body) {
        Long userId = requireUser();
        Long tenantId = BaseContext.getCurrentTenantId();
        User user = userService.getById(userId);

        Integer type = parseInteger(body.get("complaintType"));
        if (type == null) {
            type = Complaint.TYPE_OTHER;
        }
        String content = trimmed(body.get("content"), 1000);
        if (content == null) {
            throw new CustomException("请填写投诉内容");
        }
        String title = trimmed(body.get("title"), 200);
        if (title == null) {
            title = "投诉";
        }
        Complaint complaint = new Complaint();
        complaint.setComplaintType(type);
        complaint.setTitle(title);
        complaint.setContent(content);
        complaint.setImageUrls(trimmed(body.get("imageUrls"), 1000));
        complaint.setOrderId(parseLong(body.get("orderId")));
        complaint.setOrderNumber(trimmed(body.get("orderNumber"), 50));
        complaint.setUserId(userId);
        complaint.setUserName((user != null && user.getName() != null) ? user.getName() : "用户" + userId);
        complaint.setUserPhone(user != null ? user.getPhone() : null);
        complaint.setTenantId(tenantId);
        log.info("[投诉-顾客] 提交投诉: userId={}, type={}, orderId={}", userId, type, complaint.getOrderId());
        return R.success(customerService.createComplaint(complaint));
    }

    /** 我的投诉列表（按当前用户过滤） */
    @GetMapping("/complaint/list")
    @Operation(summary = "我的投诉列表")
    public R<List<Complaint>> listMyComplaints(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "type", required = false) Integer type) {
        Long userId = requireUser();
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Complaint> list = customerService.getComplaintList(status, type, tenantId).stream()
                .filter(c -> userId.equals(c.getUserId()))
                .collect(Collectors.toList());
        return R.success(list);
    }

    /** 投诉详情（归属校验） */
    @GetMapping("/complaint/{id}")
    @Operation(summary = "投诉详情")
    public R<Complaint> getComplaint(@PathVariable Long id) {
        return R.success(getOwnedComplaint(id));
    }

    /** 对投诉处理结果进行满意度评价（归属校验） */
    @PostMapping("/complaint/{id}/rate")
    @RateLimit
    @Operation(summary = "评价投诉处理")
    public R<String> rateComplaint(
            @PathVariable Long id,
            @RequestParam("satisfaction") Integer satisfaction,
            @RequestParam(value = "feedback", required = false) String feedback) {
        getOwnedComplaint(id);
        if (satisfaction == null || satisfaction < 1 || satisfaction > 5) {
            throw new CustomException("请给出 1-5 分的评价");
        }
        boolean ok = customerService.rateComplaint(id, satisfaction, feedback);
        return ok ? R.success("评价已提交") : R.error("评价失败");
    }

    // ==================== 归属校验与工具 ====================

    private Long requireUser() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new CustomException("请先登录");
        }
        return userId;
    }

    /** 取会话并校验归属当前用户，否则抛业务异常（不泄露会话是否存在，统一提示） */
    private CsSession getOwnedSession(Long sessionId) {
        Long userId = requireUser();
        CsSession session = customerService.getSessionById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            log.warn("[客服-顾客] 越权访问会话被拦截: userId={}, sessionId={}", userId, sessionId);
            throw new CustomException("会话不存在或无权访问");
        }
        return session;
    }

    /** 取投诉并校验归属当前用户 */
    private Complaint getOwnedComplaint(Long id) {
        Long userId = requireUser();
        Complaint complaint = customerService.getComplaintById(id);
        if (complaint == null || !userId.equals(complaint.getUserId())) {
            log.warn("[投诉-顾客] 越权访问投诉被拦截: userId={}, complaintId={}", userId, id);
            throw new CustomException("投诉不存在或无权访问");
        }
        return complaint;
    }

    private String trimmed(Object value, int maxLen) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) {
            return null;
        }
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    private Integer parseInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }
}
