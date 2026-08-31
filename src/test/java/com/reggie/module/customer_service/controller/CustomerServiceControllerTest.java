package com.reggie.module.customer_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.CsrfTokenUtil;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.module.customer.mapper.CsSessionMapper;
import com.reggie.module.customer.mapper.CsMessageMapper;
import com.reggie.module.customer.mapper.ComplaintMapper;
import com.reggie.module.customer.model.CsSession;
import com.reggie.module.customer.model.CsMessage;
import com.reggie.module.customer.model.Complaint;
import com.reggie.module.customer.service.CustomerServiceInterface;
import com.reggie.test.TestDatabaseCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CustomerServiceController 测试 — 客服管理
 *
 * 测试策略：
 * - 使用真实 MySQL 数据库（application-test.yml + jdbc:mysql://localhost:3306/reggie）
 * - schema-customer-service.sql 通过 @Sql 在每个测试方法前执行建表
 * - @Transactional 每个测试方法回滚，天然隔离
 * - 仅用 sessionAttr 注入：employee=1L、tenantId=1L 触发 LoginCheckFilter 设置 BaseContext
 * - 写操作（POST/PUT/DELETE）通过 withCsrfToken 注入有效的 CSRF token
 *
 * @author reggie
 * @since 2026-08-28
 */
@SpringBootTest(classes = com.reggie.ReggieApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "classpath:schema-customer-service.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
public class CustomerServiceControllerTest {

    private static final String CSRF_TOKEN_SESSION_KEY = "csrfToken";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerServiceInterface customerService;

    @Autowired
    private CsSessionMapper sessionMapper;

    @Autowired
    private CsMessageMapper messageMapper;

    @Autowired
    private ComplaintMapper complaintMapper;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.cleanTables("cs_session", "cs_message", "complaint");
        BaseContext.setCurrentId(1L);
        BaseContext.setCurrentTenantId(1L);
    }

    // ==================== 会话管理 ====================

    @Test
    @DisplayName("1. 创建客服会话 - 成功")
    void testCreateSession_success() throws Exception {
        mockMvc.perform(withCsrfToken(post("/cs/session/create")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("sessionType", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.sessionNo").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.status").value(CsSession.STATUS_WAITING));
    }

    @Test
    @DisplayName("2. 创建客服会话 - 关联订单")
    void testCreateSession_withOrderId() throws Exception {
        mockMvc.perform(withCsrfToken(post("/cs/session/create")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("sessionType", "2")
                        .param("orderId", "1001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.orderId").value(1001));
    }

    @Test
    @DisplayName("3. 获取会话列表 - 空列表")
    void testGetSessionList_empty() throws Exception {
        mockMvc.perform(get("/cs/session/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("4. 获取会话列表 - 按状态筛选")
    void testGetSessionList_byStatus() throws Exception {
        CsSession session = createSession(1001L, "UserA", CsSession.STATUS_WAITING, 1L);
        sessionMapper.insert(session);

        mockMvc.perform(get("/cs/session/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].status").value(CsSession.STATUS_WAITING));
    }

    @Test
    @DisplayName("5. 获取会话详情 - 按ID查询")
    void testGetSessionById() throws Exception {
        CsSession session = createSession(2001L, "UserB", CsSession.STATUS_WAITING, 1L);
        sessionMapper.insert(session);
        Long id = session.getId();

        mockMvc.perform(get("/cs/session/{id}", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.userName").value("UserB"));
    }

    @Test
    @DisplayName("6. 分配客服 - 成功")
    void testAssignAgent_success() throws Exception {
        CsSession session = createSession(3001L, "UserC", CsSession.STATUS_WAITING, 1L);
        sessionMapper.insert(session);
        Long id = session.getId();

        mockMvc.perform(withCsrfToken(post("/cs/session/{id}/assign", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("agentId", "100")
                        .param("agentName", "客服小张")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        CsSession updated = sessionMapper.selectById(id);
        assertThat(updated).isNotNull();
        assertThat(updated.getAgentId()).isEqualTo(100L);
        assertThat(updated.getAgentName()).isEqualTo("客服小张");
        assertThat(updated.getStatus()).isEqualTo(CsSession.STATUS_IN_PROGRESS);
        assertThat(updated.getFirstResponseTime()).isNotNull();
    }

    @Test
    @DisplayName("7. 分配客服 - 会话不存在返回错误")
    void testAssignAgent_sessionNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(post("/cs/session/{id}/assign", 999999L)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("agentId", "100")
                        .param("agentName", "客服小张")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("8. 关闭会话 - 成功")
    void testCloseSession_success() throws Exception {
        CsSession session = createSession(4001L, "UserD", CsSession.STATUS_IN_PROGRESS, 1L);
        session.setAgentId(100L);
        session.setAgentName("客服小张");
        sessionMapper.insert(session);
        Long id = session.getId();

        mockMvc.perform(withCsrfToken(post("/cs/session/{id}/close", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("rating", "5")
                        .param("feedback", "非常满意")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        CsSession updated = sessionMapper.selectById(id);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(CsSession.STATUS_CLOSED);
        assertThat(updated.getSatisfactionRating()).isEqualTo(5);
        assertThat(updated.getUserFeedback()).isEqualTo("非常满意");
        assertThat(updated.getCloseTime()).isNotNull();
    }

    @Test
    @DisplayName("9. 关闭会话 - 会话不存在返回错误")
    void testCloseSession_sessionNotFound() throws Exception {
        mockMvc.perform(withCsrfToken(post("/cs/session/{id}/close", 999999L)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // ==================== 消息管理 ====================

    @Test
    @DisplayName("10. 发送消息 - 成功")
    void testSendMessage_success() throws Exception {
        CsSession session = createSession(5001L, "UserE", CsSession.STATUS_IN_PROGRESS, 1L);
        sessionMapper.insert(session);
        Long sessionId = session.getId();

        mockMvc.perform(withCsrfToken(post("/cs/message/send")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("sessionId", sessionId.toString())
                        .param("senderType", "2")
                        .param("messageType", "1")
                        .param("content", "您好，请问有什么可以帮您？")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.senderType").value(CsMessage.SENDER_AGENT))
                .andExpect(jsonPath("$.data.content").value("您好，请问有什么可以帮您？"));
    }

    @Test
    @DisplayName("11. 获取会话消息列表")
    void testGetSessionMessages() throws Exception {
        CsSession session = createSession(6001L, "UserF", CsSession.STATUS_IN_PROGRESS, 1L);
        sessionMapper.insert(session);
        Long sessionId = session.getId();

        CsMessage message1 = createMessage(sessionId, CsMessage.SENDER_USER, 6001L, "UserF",
                CsMessage.TYPE_TEXT, "我订单有问题");
        messageMapper.insert(message1);

        CsMessage message2 = createMessage(sessionId, CsMessage.SENDER_AGENT, 1L, "Agent",
                CsMessage.TYPE_TEXT, "好的，我来帮您查一下");
        messageMapper.insert(message2);

        mockMvc.perform(get("/cs/message/list/{sessionId}", sessionId)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    @DisplayName("12. 获取未读消息数量")
    void testGetUnreadMessageCount() throws Exception {
        CsSession session = createSession(7001L, "UserG", CsSession.STATUS_IN_PROGRESS, 1L);
        sessionMapper.insert(session);
        Long sessionId = session.getId();

        // 创建2条未读消息（客服发给用户）
        CsMessage msg1 = createMessage(sessionId, CsMessage.SENDER_AGENT, 1L, "Agent",
                CsMessage.TYPE_TEXT, "消息1");
        msg1.setIsRead(0);
        messageMapper.insert(msg1);

        CsMessage msg2 = createMessage(sessionId, CsMessage.SENDER_AGENT, 1L, "Agent",
                CsMessage.TYPE_TEXT, "消息2");
        msg2.setIsRead(0);
        messageMapper.insert(msg2);

        mockMvc.perform(get("/cs/message/unread/{sessionId}", sessionId)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("userType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @DisplayName("13. 标记消息已读")
    void testMarkMessagesAsRead() throws Exception {
        CsSession session = createSession(8001L, "UserH", CsSession.STATUS_IN_PROGRESS, 1L);
        sessionMapper.insert(session);
        Long sessionId = session.getId();

        // 创建未读消息
        CsMessage msg = createMessage(sessionId, CsMessage.SENDER_AGENT, 1L, "Agent",
                CsMessage.TYPE_TEXT, "消息内容");
        msg.setIsRead(0);
        messageMapper.insert(msg);

        mockMvc.perform(withCsrfToken(post("/cs/message/read/{sessionId}", sessionId)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("userType", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }

    // ==================== 投诉管理 ====================

    @Test
    @DisplayName("14. 创建投诉 - 成功")
    void testCreateComplaint_success() throws Exception {
        Complaint complaint = createComplaint(9001L, "UserI", "订单餐品有问题",
                "餐品中有异物", Complaint.TYPE_FOOD_QUALITY);

        mockMvc.perform(withCsrfToken(post("/cs/complaint/create")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .contentType("application/json")
                        .content(toJson(complaint))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.complaintNo").isNotEmpty())
                .andExpect(jsonPath("$.data.status").value(Complaint.STATUS_PENDING));
    }

    @Test
    @DisplayName("15. 获取投诉列表 - 空列表")
    void testGetComplaintList_empty() throws Exception {
        mockMvc.perform(get("/cs/complaint/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("16. 获取投诉列表 - 按状态筛选")
    void testGetComplaintList_byStatus() throws Exception {
        Complaint complaint = createComplaint(1001L, "UserJ", "配送太慢",
                "等了1小时还没到", Complaint.TYPE_DELIVERY_SERVICE);
        complaintMapper.insert(complaint);

        mockMvc.perform(get("/cs/complaint/list")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    @DisplayName("17. 获取投诉详情 - 按ID查询")
    void testGetComplaintById() throws Exception {
        Complaint complaint = createComplaint(1101L, "UserK", "价格问题",
                "价格与宣传不符", Complaint.TYPE_PRICE_ISSUE);
        complaintMapper.insert(complaint);
        Long id = complaint.getId();

        mockMvc.perform(get("/cs/complaint/{id}", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.title").value("价格问题"));
    }

    @Test
    @DisplayName("18. 处理投诉 - 成功")
    void testHandleComplaint_success() throws Exception {
        Complaint complaint = createComplaint(1201L, "UserL", "服务态度差",
                "店员态度不好", Complaint.TYPE_SERVICE_ATTITUDE);
        complaintMapper.insert(complaint);
        Long id = complaint.getId();

        mockMvc.perform(withCsrfToken(post("/cs/complaint/{id}/handle", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("handleResult", "已联系用户道歉")
                        .param("compensationAmount", "20.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Complaint updated = complaintMapper.selectById(id);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(Complaint.STATUS_PROCESSING);
        assertThat(updated.getHandleResult()).isEqualTo("已联系用户道歉");
        assertThat(updated.getCompensationAmount()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(updated.getHandlerId()).isEqualTo(1L);
        assertThat(updated.getHandleTime()).isNotNull();
    }

    @Test
    @DisplayName("19. 关闭投诉 - 成功")
    void testCloseComplaint_success() throws Exception {
        Complaint complaint = createComplaint(1301L, "UserM", "其他问题",
                "一般反馈", Complaint.TYPE_OTHER);
        complaint.setStatus(Complaint.STATUS_PROCESSING);
        complaintMapper.insert(complaint);
        Long id = complaint.getId();

        mockMvc.perform(withCsrfToken(post("/cs/complaint/{id}/close", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Complaint updated = complaintMapper.selectById(id);
        assertThat(updated).isNotNull();
        assertThat(updated.getStatus()).isEqualTo(Complaint.STATUS_CLOSED);
    }

    @Test
    @DisplayName("20. 投诉评价 - 成功")
    void testRateComplaint_success() throws Exception {
        Complaint complaint = createComplaint(1401L, "UserN", "质量投诉",
                "食品不新鲜", Complaint.TYPE_FOOD_QUALITY);
        complaint.setStatus(Complaint.STATUS_PROCESSING);
        complaintMapper.insert(complaint);
        Long id = complaint.getId();

        mockMvc.perform(withCsrfToken(post("/cs/complaint/{id}/rate", id)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("satisfaction", "4")
                        .param("feedback", "处理速度还可以")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));

        Complaint updated = complaintMapper.selectById(id);
        assertThat(updated).isNotNull();
        assertThat(updated.getSatisfaction()).isEqualTo(4);
        assertThat(updated.getUserFeedback()).isEqualTo("处理速度还可以");
    }

    // ==================== 统计分析 ====================

    @Test
    @DisplayName("21. 获取客服统计数据")
    void testGetCustomerServiceStatistics() throws Exception {
        CsSession session = createSession(1501L, "UserO", CsSession.STATUS_IN_PROGRESS, 1L);
        session.setCreateTime(LocalDateTime.of(2026, 8, 28, 10, 0, 0));
        session.setFirstResponseTime(LocalDateTime.of(2026, 8, 28, 10, 5, 0));
        sessionMapper.insert(session);

        mockMvc.perform(get("/cs/statistics")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", formatDateTime(LocalDateTime.of(2026, 8, 28, 0, 0, 0)))
                        .param("endDate", formatDateTime(LocalDateTime.of(2026, 8, 28, 23, 59, 59))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.totalSessions").isNumber())
                .andExpect(jsonPath("$.data.closedSessions").isNumber())
                .andExpect(jsonPath("$.data.waitingSessions").isNumber());
    }

    @Test
    @DisplayName("22. 获取投诉统计数据")
    void testGetComplaintStatistics() throws Exception {
        Complaint complaint = createComplaint(1601L, "UserP", "投诉测试",
                "投诉内容", Complaint.TYPE_FOOD_QUALITY);
        complaint.setCreateTime(LocalDateTime.of(2026, 8, 28, 14, 0, 0));
        complaintMapper.insert(complaint);

        mockMvc.perform(get("/cs/complaint/statistics")
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", formatDateTime(LocalDateTime.of(2026, 8, 28, 0, 0, 0)))
                        .param("endDate", formatDateTime(LocalDateTime.of(2026, 8, 28, 23, 59, 59))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.totalComplaints").isNumber())
                .andExpect(jsonPath("$.data.typeDistribution").isMap());
    }

    @Test
    @DisplayName("23. 获取客服工作量统计")
    void testGetAgentWorkload() throws Exception {
        CsSession session = createSession(1701L, "UserQ", CsSession.STATUS_CLOSED, 1L);
        session.setAgentId(100L);
        session.setAgentName("客服小王");
        session.setCreateTime(LocalDateTime.of(2026, 8, 28, 9, 0, 0));
        session.setCloseTime(LocalDateTime.of(2026, 8, 28, 10, 0, 0));
        session.setSatisfactionRating(4);
        sessionMapper.insert(session);

        mockMvc.perform(get("/cs/agent/{agentId}/workload", 100L)
                        .sessionAttr("employee", 1L)
                        .sessionAttr("tenantId", 1L)
                        .param("startDate", formatDateTime(LocalDateTime.of(2026, 8, 28, 0, 0, 0)))
                        .param("endDate", formatDateTime(LocalDateTime.of(2026, 8, 28, 23, 59, 59))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data.agentId").value(100))
                .andExpect(jsonPath("$.data.totalSessions").isNumber())
                .andExpect(jsonPath("$.data.closedSessions").isNumber());
    }

    // ==================== 辅助方法 ====================

    private CsSession createSession(Long userId, String userName, Integer status, Long tenantId) {
        CsSession session = new CsSession();
        session.setSessionNo("CS" + System.currentTimeMillis() + userId);
        session.setUserId(userId);
        session.setUserName(userName);
        session.setStatus(status);
        session.setSessionType(1);
        session.setTenantId(tenantId);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        return session;
    }

    private CsMessage createMessage(Long sessionId, Integer senderType, Long senderId,
                                     String senderName, Integer messageType, String content) {
        CsMessage message = new CsMessage();
        message.setSessionId(sessionId);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setIsRead(0);
        message.setTenantId(1L);
        message.setCreateTime(LocalDateTime.now());
        return message;
    }

    private Complaint createComplaint(Long userId, String userName, String title,
                                       String content, Integer complaintType) {
        Complaint complaint = new Complaint();
        complaint.setComplaintNo("CP" + System.currentTimeMillis() + userId);
        complaint.setUserId(userId);
        complaint.setUserName(userName);
        complaint.setUserPhone("13800138000");
        complaint.setOrderId(10000L);
        complaint.setOrderNumber("ORD" + System.currentTimeMillis());
        complaint.setComplaintType(complaintType);
        complaint.setTitle(title);
        complaint.setContent(content);
        complaint.setStatus(Complaint.STATUS_PENDING);
        complaint.setTenantId(1L);
        complaint.setCreateTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());
        return complaint;
    }

    /**
     * 为 MockMvc 请求添加有效的 CSRF Token。
     */
    private MockHttpServletRequestBuilder withCsrfToken(MockHttpServletRequestBuilder request) {
        String token = CsrfTokenUtil.generateToken();
        return request
                .sessionAttr(CSRF_TOKEN_SESSION_KEY, token)
                .header(CSRF_HEADER_NAME, token);
    }

    private String toJson(Object obj) throws Exception {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}