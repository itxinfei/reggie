package com.reggie.module.customer.service.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.customer.service.model.CsSession;
import com.reggie.module.customer.service.model.CsMessage;
import com.reggie.module.customer.service.model.Complaint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Customer Service Interface
 * 
 * @author reggie
 * @since 2026-08-11
 */
public interface CustomerServiceInterface extends IService<CsSession> {

    // ==================== Session Management ====================

    /**
     * Create customer service session
     *
     * @param userId      User ID
     * @param userName    User name
     * @param sessionType Session type
     * @param orderId     Related order ID
     * @param tenantId    Tenant ID
     * @return Session
     */
    CsSession createSession(Long userId, String userName, Integer sessionType, Long orderId, Long tenantId);

    /**
     * Get session list
     *
     * @param status   Status filter
     * @param tenantId Tenant ID
     * @return Session list
     */
    List<CsSession> getSessionList(Integer status, Long tenantId);

    /**
     * Get session by ID
     *
     * @param id Session ID
     * @return Session
     */
    CsSession getSessionById(Long id);

    /**
     * Assign agent to session
     *
     * @param sessionId Session ID
     * @param agentId   Agent ID
     * @param agentName Agent name
     * @return Success or not
     */
    boolean assignAgent(Long sessionId, Long agentId, String agentName);

    /**
     * Close session
     *
     * @param sessionId  Session ID
     * @param rating     Satisfaction rating
     * @param feedback   User feedback
     * @return Success or not
     */
    boolean closeSession(Long sessionId, Integer rating, String feedback);

    // ==================== Message Management ====================

    /**
     * Send message
     *
     * @param sessionId   Session ID
     * @param senderType  Sender type
     * @param senderId    Sender ID
     * @param senderName  Sender name
     * @param messageType Message type
     * @param content     Message content
     * @param imageUrl    Image URL
     * @param tenantId    Tenant ID
     * @return Message
     */
    CsMessage sendMessage(Long sessionId, Integer senderType, Long senderId, String senderName,
                          Integer messageType, String content, String imageUrl, Long tenantId);

    /**
     * Get session messages
     *
     * @param sessionId Session ID
     * @return Message list
     */
    List<CsMessage> getSessionMessages(Long sessionId);

    /**
     * Get unread message count
     *
     * @param sessionId Session ID
     * @param userType  User type (1-User, 2-Agent)
     * @return Unread count
     */
    int getUnreadMessageCount(Long sessionId, Integer userType);

    /**
     * Mark messages as read
     *
     * @param sessionId Session ID
     * @param userType  User type
     * @return Success or not
     */
    boolean markMessagesAsRead(Long sessionId, Integer userType);

    // ==================== Complaint Management ====================

    /**
     * Create complaint
     *
     * @param complaint Complaint
     * @return Complaint
     */
    Complaint createComplaint(Complaint complaint);

    /**
     * Get complaint list
     *
     * @param status   Status filter
     * @param type     Type filter
     * @param tenantId Tenant ID
     * @return Complaint list
     */
    List<Complaint> getComplaintList(Integer status, Integer type, Long tenantId);

    /**
     * Get complaint by ID
     *
     * @param id Complaint ID
     * @return Complaint
     */
    Complaint getComplaintById(Long id);

    /**
     * Handle complaint
     *
     * @param id                Complaint ID
     * @param handlerId         Handler ID
     * @param handlerName       Handler name
     * @param handleResult      Handle result
     * @param compensationAmount Compensation amount
     * @return Success or not
     */
    boolean handleComplaint(Long id, Long handlerId, String handlerName, String handleResult, BigDecimal compensationAmount);

    /**
     * Close complaint
     *
     * @param id Complaint ID
     * @return Success or not
     */
    boolean closeComplaint(Long id);

    /**
     * Rate complaint handling
     *
     * @param id         Complaint ID
     * @param satisfaction Satisfaction rating
     * @param feedback   User feedback
     * @return Success or not
     */
    boolean rateComplaint(Long id, Integer satisfaction, String feedback);

    // ==================== Statistics ====================

    /**
     * Get customer service statistics
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Statistics
     */
    Map<String, Object> getCustomerServiceStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * Get complaint statistics
     *
     * @param startDate Start date
     * @param endDate   End date
     * @param tenantId  Tenant ID
     * @return Statistics
     */
    Map<String, Object> getComplaintStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * Get agent workload
     *
     * @param agentId  Agent ID
     * @param startDate Start date
     * @param endDate   End date
     * @return Workload
     */
    Map<String, Object> getAgentWorkload(Long agentId, LocalDateTime startDate, LocalDateTime endDate);
}
