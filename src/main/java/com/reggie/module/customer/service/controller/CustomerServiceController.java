package com.reggie.module.customer.service.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.customer.service.model.CsSession;
import com.reggie.module.customer.service.model.CsMessage;
import com.reggie.module.customer.service.model.Complaint;
import com.reggie.module.customer.service.service.CustomerServiceInterface;
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
@Tag(name = "Customer Service Management")
public class CustomerServiceController {

    @Autowired
    private CustomerServiceInterface customerService;

    // ==================== Session Management ====================

    @PostMapping("/session/create")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Create customer service session")
    public R<CsSession> createSession(
                        @Parameter(description = "Session type") @RequestParam Integer sessionType,
            @Parameter(description = "Order ID") @RequestParam(required = false) Long orderId) {
        Long userId = BaseContext.getCurrentId();
        String userName = "User"; // Should get from user service
        Long tenantId = BaseContext.getCurrentTenantId();
        CsSession session = customerService.createSession(userId, userName, sessionType, orderId, tenantId);
        return R.success(session);
    }

    @GetMapping("/session/list")
    @Operation(summary = "Get session list")
    public R<List<CsSession>> getSessionList(
                        @Parameter(description = "Status") @RequestParam(required = false) Integer status) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<CsSession> list = customerService.getSessionList(status, tenantId);
        return R.success(list);
    }

    @GetMapping("/session/{id}")
    @Operation(summary = "Get session by ID")
    public R<CsSession> getSessionById(@PathVariable Long id) {
        CsSession session = customerService.getSessionById(id);
        return R.success(session);
    }

    @PostMapping("/session/{id}/assign")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Assign agent to session")
    public R<String> assignAgent(
            @Parameter(description = "ID")
            @PathVariable Long id,
            @Parameter(description = "Agent ID") @RequestParam Long agentId,
            @Parameter(description = "Agent name") @RequestParam String agentName) {
        boolean success = customerService.assignAgent(id, agentId, agentName);
        return success ? R.success("Agent assigned") : R.error("Assignment failed");
    }

    @PostMapping("/session/{id}/close")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Close session")
    public R<String> closeSession(
            @Parameter(description = "ID")
            @PathVariable Long id,
            @Parameter(description = "Satisfaction rating (1-5)") @RequestParam(required = false) Integer rating,
            @Parameter(description = "User feedback") @RequestParam(required = false) String feedback) {
        boolean success = customerService.closeSession(id, rating, feedback);
        return success ? R.success("Session closed") : R.error("Close failed");
    }

    // ==================== Message Management ====================

    @PostMapping("/message/send")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Send message")
    public R<CsMessage> sendMessage(
                        @Parameter(description = "Session ID") @RequestParam Long sessionId,
            @Parameter(description = "Sender type: 1-User, 2-Agent") @RequestParam Integer senderType,
            @Parameter(description = "Message type: 1-Text, 2-Image") @RequestParam(defaultValue = "1") Integer messageType,
            @Parameter(description = "Content") @RequestParam String content,
            @Parameter(description = "Image URL") @RequestParam(required = false) String imageUrl) {
        Long senderId = BaseContext.getCurrentId();
        String senderName = senderType == 1 ? "User" : "Agent";
        Long tenantId = BaseContext.getCurrentTenantId();
        CsMessage message = customerService.sendMessage(sessionId, senderType, senderId, senderName, messageType, content, imageUrl, tenantId);
        return R.success(message);
    }

    @GetMapping("/message/list/{sessionId}")
    @Operation(summary = "Get session messages")
    @Parameter(description = "SessionId")
    public R<List<CsMessage>> getSessionMessages(@PathVariable Long sessionId) {
        List<CsMessage> messages = customerService.getSessionMessages(sessionId);
        return R.success(messages);
    }

    @GetMapping("/message/unread/{sessionId}")
    @Operation(summary = "Get unread message count")
    public R<Integer> getUnreadMessageCount(
            @Parameter(description = "sessionId")
            @PathVariable Long sessionId,
            @Parameter(description = "User type: 1-User, 2-Agent") @RequestParam Integer userType) {
        int count = customerService.getUnreadMessageCount(sessionId, userType);
        return R.success(count);
    }

    @PostMapping("/message/read/{sessionId}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Mark messages as read")
    public R<String> markMessagesAsRead(
            @Parameter(description = "sessionId")
            @PathVariable Long sessionId,
            @Parameter(description = "User type: 1-User, 2-Agent") @RequestParam Integer userType) {
        boolean success = customerService.markMessagesAsRead(sessionId, userType);
        return success ? R.success("Messages marked as read") : R.error("Operation failed");
    }

    // ==================== Complaint Management ====================

    @PostMapping("/complaint/create")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Create complaint")
    public R<Complaint> createComplaint(@Valid @RequestBody Complaint complaint) {
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        complaint.setUserId(userId);
        complaint.setTenantId(tenantId);
        Complaint created = customerService.createComplaint(complaint);
        return R.success(created);
    }

    @GetMapping("/complaint/list")
    @Operation(summary = "Get complaint list")
    public R<List<Complaint>> getComplaintList(
                        @Parameter(description = "Status") @RequestParam(required = false) Integer status,
            @Parameter(description = "Type") @RequestParam(required = false) Integer type) {
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Complaint> list = customerService.getComplaintList(status, type, tenantId);
        return R.success(list);
    }

    @GetMapping("/complaint/{id}")
    @Operation(summary = "Get complaint by ID")
    public R<Complaint> getComplaintById(@PathVariable Long id) {
        Complaint complaint = customerService.getComplaintById(id);
        return R.success(complaint);
    }

    @PostMapping("/complaint/{id}/handle")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Handle complaint")
    public R<String> handleComplaint(
            @Parameter(description = "ID")
            @PathVariable Long id,
            @Parameter(description = "Handle result") @RequestParam String handleResult,
            @Parameter(description = "Compensation amount") @RequestParam(required = false) BigDecimal compensationAmount) {
        Long handlerId = BaseContext.getCurrentId();
        String handlerName = "Handler";
        boolean success = customerService.handleComplaint(id, handlerId, handlerName, handleResult, compensationAmount);
        return success ? R.success("Complaint handled") : R.error("Handle failed");
    }

    @PostMapping("/complaint/{id}/close")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Close complaint")
    public R<String> closeComplaint(@PathVariable Long id) {
        boolean success = customerService.closeComplaint(id);
        return success ? R.success("Complaint closed") : R.error("Close failed");
    }

    @PostMapping("/complaint/{id}/rate")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "Rate complaint handling")
    public R<String> rateComplaint(
            @Parameter(description = "ID")
            @PathVariable Long id,
            @Parameter(description = "Satisfaction (1-5)") @RequestParam Integer satisfaction,
            @Parameter(description = "Feedback") @RequestParam(required = false) String feedback) {
        boolean success = customerService.rateComplaint(id, satisfaction, feedback);
        return success ? R.success("Rating submitted") : R.error("Rating failed");
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    @Operation(summary = "Get customer service statistics")
    public R<Map<String, Object>> getCustomerServiceStatistics(
                        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = customerService.getCustomerServiceStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    @GetMapping("/complaint/statistics")
    @Operation(summary = "Get complaint statistics")
    public R<Map<String, Object>> getComplaintStatistics(
                        @Parameter(description = "Start date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Long tenantId = BaseContext.getCurrentTenantId();
        Map<String, Object> statistics = customerService.getComplaintStatistics(startDate, endDate, tenantId);
        return R.success(statistics);
    }

    @GetMapping("/agent/{agentId}/workload")
    @Operation(summary = "Get agent workload")
    public R<Map<String, Object>> getAgentWorkload(
            @Parameter(description = "agentId")
            @PathVariable Long agentId,
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDate) {
        Map<String, Object> workload = customerService.getAgentWorkload(agentId, startDate, endDate);
        return R.success(workload);
    }
}



