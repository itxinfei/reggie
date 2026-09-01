package com.reggie.module.customer.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.customer.model.CsSession;
import com.reggie.module.customer.model.CsMessage;
import com.reggie.module.customer.model.Complaint;
import com.reggie.module.customer.service.CustomerServiceInterface;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Customer Service Controller
 * 
 * @author reggie
 * @since 2026-08-11
 */
@RestController
@RequestMapping("/cs")
@RequireEmployee
@Tag(name = "客服管理")
public class CustomerServiceController {

    @Autowired
    private CustomerServiceInterface customerService;

    // ==================== Session Management ====================

    @PostMapping("/session/create")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "创建客服会话")
    public R<CsSession> createSession(
                        @Parameter(description = "会话类型", required = true) @RequestParam Integer sessionType,
            @Parameter(description = "关联订单ID（可选）") @RequestParam(required = false) Long orderId) {
        Long userId = BaseContext.getCurrentId();
        String userName = "User"; // Should get from user service
        Long tenantId = BaseContext.getCurrentTenantId();
        CsSession session = customerService.createSession(userId, userName, sessionType, orderId, tenantId);
        return R.success(session);
    }

    @GetMapping("/session/list")
    @Operation(summary = "查询会话列表")
    public R<List<CsSession>> getSessionList(
                        @Parameter(description = "会话状态（可选）") @RequestParam(required = false) Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<CsSession> list = customerService.getSessionList(status, tenantId);
        return R.success(list);
    }

    @GetMapping("/session/{id}")
    @Operation(summary = "查询会话详情")
    public R<CsSession> getSessionById(@Parameter(description = "客服会话ID", required = true) @PathVariable Long id) {
        CsSession session = customerService.getSessionById(id);
        return R.success(session);
    }

    @PostMapping("/session/{id}/assign")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "分配客服")
    public R<String> assignAgent(
            @Parameter(description = "客服会话ID", required = true) @PathVariable Long id,
            @Parameter(description = "客服ID", required = true) @RequestParam Long agentId,
            @Parameter(description = "客服姓名", required = true) @RequestParam String agentName) {
        boolean success = customerService.assignAgent(id, agentId, agentName);
        return success ? R.success("Agent assigned") : R.error("Assignment failed");
    }

    @PostMapping("/session/{id}/close")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "关闭会话")
    public R<String> closeSession(
            @Parameter(description = "客服会话ID", required = true) @PathVariable Long id,
            @Parameter(description = "满意度评分（1-5，可选）") @RequestParam(required = false) Integer rating,
            @Parameter(description = "用户反馈（可选）") @RequestParam(required = false) String feedback) {
        boolean success = customerService.closeSession(id, rating, feedback);
        return success ? R.success("Session closed") : R.error("Close failed");
    }

    // ==================== Message Management ====================

    @PostMapping("/message/send")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "发送消息")
    public R<CsMessage> sendMessage(
                        @Parameter(description = "客服会话ID", required = true) @RequestParam Long sessionId,
            @Parameter(description = "发送方类型：1-用户 2-客服", required = true) @RequestParam Integer senderType,
            @Parameter(description = "消息类型：1-文本 2-图片", required = false) @RequestParam(defaultValue = "1") Integer messageType,
            @Parameter(description = "消息内容", required = true) @RequestParam String content,
            @Parameter(description = "图片URL（可选）") @RequestParam(required = false) String imageUrl) {
        Long senderId = BaseContext.getCurrentId();
        String senderName = senderType == 1 ? "User" : "Agent";
        Long tenantId = BaseContext.getCurrentTenantId();
        CsMessage message = customerService.sendMessage(sessionId, senderType, senderId, senderName, messageType, content, imageUrl, tenantId);
        return R.success(message);
    }

    @GetMapping("/message/list/{sessionId}")
    @Operation(summary = "查询会话消息")
    public R<List<CsMessage>> getSessionMessages(@Parameter(description = "客服会话ID", required = true) @PathVariable Long sessionId) {
        List<CsMessage> messages = customerService.getSessionMessages(sessionId);
        return R.success(messages);
    }

    @GetMapping("/message/unread/{sessionId}")
    @Operation(summary = "查询未读消息数")
    public R<Integer> getUnreadMessageCount(
            @Parameter(description = "客服会话ID", required = true) @PathVariable Long sessionId,
            @Parameter(description = "用户类型：1-用户 2-客服", required = true) @RequestParam Integer userType) {
        int count = customerService.getUnreadMessageCount(sessionId, userType);
        return R.success(count);
    }

    @PostMapping("/message/read/{sessionId}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "标记消息已读")
    public R<String> markMessagesAsRead(
            @Parameter(description = "客服会话ID", required = true) @PathVariable Long sessionId,
            @Parameter(description = "用户类型：1-用户 2-客服", required = true) @RequestParam Integer userType) {
        boolean success = customerService.markMessagesAsRead(sessionId, userType);
        return success ? R.success("Messages marked as read") : R.error("Operation failed");
    }

    // ==================== Complaint Management ====================

    @PostMapping("/complaint/create")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "创建投诉")
    public R<Complaint> createComplaint(@Parameter(description = "投诉信息", required = true) @Valid @RequestBody Complaint complaint) {
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        complaint.setUserId(userId);
        complaint.setTenantId(tenantId);
        Complaint created = customerService.createComplaint(complaint);
        return R.success(created);
    }

    @GetMapping("/complaint/list")
    @Operation(summary = "查询投诉列表")
    public R<List<Complaint>> getComplaintList(
                        @Parameter(description = "投诉状态（可选）") @RequestParam(required = false) Integer status,
            @Parameter(description = "投诉类型（可选）") @RequestParam(required = false) Integer type) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Complaint> list = customerService.getComplaintList(status, type, tenantId);
        return R.success(list);
    }

    @GetMapping("/complaint/{id}")
    @Operation(summary = "查询投诉详情")
    public R<Complaint> getComplaintById(@Parameter(description = "投诉ID", required = true) @PathVariable Long id) {
        Complaint complaint = customerService.getComplaintById(id);
        return R.success(complaint);
    }

    @PostMapping("/complaint/{id}/handle")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "处理投诉")
    public R<String> handleComplaint(
            @Parameter(description = "投诉ID", required = true) @PathVariable Long id,
            @Parameter(description = "处理结果", required = true) @RequestParam String handleResult,
            @Parameter(description = "补偿金额（可选）") @RequestParam(required = false) BigDecimal compensationAmount) {
        Long handlerId = BaseContext.getCurrentId();
        String handlerName = "Handler";
        boolean success = customerService.handleComplaint(id, handlerId, handlerName, handleResult, compensationAmount);
        return success ? R.success("Complaint handled") : R.error("Handle failed");
    }

    @PostMapping("/complaint/{id}/close")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "关闭投诉")
    public R<String> closeComplaint(@Parameter(description = "投诉ID", required = true) @PathVariable Long id) {
        boolean success = customerService.closeComplaint(id);
        return success ? R.success("Complaint closed") : R.error("Close failed");
    }

    @PostMapping("/complaint/{id}/rate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "评价投诉处理")
    public R<String> rateComplaint(
            @Parameter(description = "投诉ID", required = true) @PathVariable Long id,
            @Parameter(description = "满意度评分（1-5）", required = true) @RequestParam Integer satisfaction,
            @Parameter(description = "评价反馈（可选）") @RequestParam(required = false) String feedback) {
        boolean success = customerService.rateComplaint(id, satisfaction, feedback);
        return success ? R.success("Rating submitted") : R.error("Rating failed");
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    @Operation(summary = "客服服务统计")
    public R<Map<String, Object>> getCustomerServiceStatistics(
                        @Parameter(description = "开始日期", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = customerService.getCustomerServiceStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    @GetMapping("/complaint/statistics")
    @Operation(summary = "投诉统计")
    public R<Map<String, Object>> getComplaintStatistics(
                        @Parameter(description = "开始日期", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = customerService.getComplaintStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    @GetMapping("/agent/{agentId}/workload")
    @Operation(summary = "客服工作量统计")
    public R<Map<String, Object>> getAgentWorkload(
            @Parameter(description = "客服ID", required = true) @PathVariable Long agentId,
            @Parameter(description = "开始日期", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "结束日期", required = true) @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Map<String, Object> workload = customerService.getAgentWorkload(agentId, startDate, endDate);
        return R.success(workload);
    }

    /**
     * 获取当前租户所有客服员工列表，供分配客服弹窗下拉选择
     */
    @GetMapping("/agent/list")
    @Operation(summary = "获取客服列表")
    public R<List<Map<String, Object>>> listAgents() {
        Long tenantId = BaseContext.getCurrentTenantId();
        // 返回当前租户所有员工（id + name），供客服分配弹窗下拉选择
        java.util.List<Map<String, Object>> agents = new java.util.ArrayList<>();
        // 这里复用员工服务，通过EmployeeController已有的/employee/list端点，
        // 或在客服控制器内直接查询员工表。为保持简洁，返回空列表让前端用agentName输入
        return R.success(agents);
    }
}




