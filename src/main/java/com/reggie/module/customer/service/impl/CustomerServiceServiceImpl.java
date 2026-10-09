package com.reggie.module.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.customer.mapper.CsSessionMapper;
import com.reggie.module.customer.mapper.CsMessageMapper;
import com.reggie.module.customer.mapper.ComplaintMapper;
import com.reggie.module.customer.model.CsSession;
import com.reggie.module.customer.model.CsMessage;
import com.reggie.module.customer.model.Complaint;
import com.reggie.module.customer.service.CustomerServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer Service Implementation
 * 
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class CustomerServiceServiceImpl extends ServiceImpl<CsSessionMapper, CsSession> 
        implements CustomerServiceInterface {

    @Autowired
    private CsSessionMapper sessionMapper;

    @Autowired
    private CsMessageMapper messageMapper;

    @Autowired
    private ComplaintMapper complaintMapper;

    // ==================== Session Management ====================

    /**
     * 创建 session。
     * @param userId 参数 userId
     * @param userName 参数 userName
     * @param sessionType 参数 sessionType
     * @param orderId 参数 orderId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsSession createSession(Long userId, String userName, Integer sessionType, Long orderId, Long tenantId) {
        CsSession session = new CsSession();
        session.setSessionNo("CS" + System.currentTimeMillis());
        session.setUserId(userId);
        session.setUserName(userName);
        session.setSessionType(sessionType);
        session.setOrderId(orderId);
        session.setStatus(CsSession.STATUS_WAITING);
        session.setTenantId(tenantId);
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        sessionMapper.insert(session);
        return session;
    }

    /**
     * 获取 session list。
     * @param status 参数 status
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<CsSession> getSessionList(Integer status, Long tenantId) {
        LambdaQueryWrapper<CsSession> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(CsSession::getStatus, status);
        }
        if (tenantId != null) {
            qw.eq(CsSession::getTenantId, tenantId);
        }
        qw.orderByDesc(CsSession::getCreateTime);
        return sessionMapper.selectList(qw);
    }

    /**
     * 获取 session by id。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    public CsSession getSessionById(Long id) {
        return sessionMapper.selectById(id);
    }

    /**
     * 分配 agent。
     * @param sessionId 参数 sessionId
     * @param agentId 参数 agentId
     * @param agentName 参数 agentName
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignAgent(Long sessionId, Long agentId, String agentName) {
        CsSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }

        session.setAgentId(agentId);
        session.setAgentName(agentName);
        session.setStatus(CsSession.STATUS_IN_PROGRESS);
        session.setFirstResponseTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());

        return sessionMapper.updateById(session) > 0;
    }

    /**
     * 关闭 session。
     * @param sessionId 参数 sessionId
     * @param rating 参数 rating
     * @param feedback 参数 feedback
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeSession(Long sessionId, Integer rating, String feedback) {
        CsSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }

        session.setStatus(CsSession.STATUS_CLOSED);
        session.setCloseTime(LocalDateTime.now());
        session.setSatisfactionRating(rating);
        session.setUserFeedback(feedback);
        session.setUpdateTime(LocalDateTime.now());

        return sessionMapper.updateById(session) > 0;
    }

    // ==================== Message Management ====================

    /**
     * 发送 message。
     * @param sessionId 参数 sessionId
     * @param senderType 参数 senderType
     * @param senderId 参数 senderId
     * @param senderName 参数 senderName
     * @param messageType 参数 messageType
     * @param content 参数 content
     * @param imageUrl 参数 imageUrl
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsMessage sendMessage(Long sessionId, Integer senderType, Long senderId, String senderName,
                                  Integer messageType, String content, String imageUrl, Long tenantId) {
        CsMessage message = new CsMessage();
        message.setSessionId(sessionId);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setSenderName(senderName);
        message.setMessageType(messageType);
        message.setContent(content);
        message.setImageUrl(imageUrl);
        message.setIsRead(0);
        message.setTenantId(tenantId);
        message.setCreateTime(LocalDateTime.now());

        messageMapper.insert(message);
        return message;
    }

    /**
     * 获取 session messages。
     * @param sessionId 参数 sessionId
     * @return 返回结果
     */
    @Override
    public List<CsMessage> getSessionMessages(Long sessionId) {
        LambdaQueryWrapper<CsMessage> qw = new LambdaQueryWrapper<>();
        qw.eq(CsMessage::getSessionId, sessionId);
        qw.orderByAsc(CsMessage::getCreateTime);
        return messageMapper.selectList(qw);
    }

    /**
     * 获取 unread message count。
     * @param sessionId 参数 sessionId
     * @param userType 参数 userType
     * @return 返回结果
     */
    @Override
    public int getUnreadMessageCount(Long sessionId, Integer userType) {
        LambdaQueryWrapper<CsMessage> qw = new LambdaQueryWrapper<>();
        qw.eq(CsMessage::getSessionId, sessionId);
        qw.eq(CsMessage::getIsRead, 0);
        // Count messages from the other party
        if (userType == 1) { // User - count agent messages
            qw.eq(CsMessage::getSenderType, CsMessage.SENDER_AGENT);
        } else { // Agent - count user messages
            qw.eq(CsMessage::getSenderType, CsMessage.SENDER_USER);
        }
        return messageMapper.selectCount(qw).intValue();
    }

    /**
     * 处理 mark messages as read。
     * @param sessionId 参数 sessionId
     * @param userType 参数 userType
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markMessagesAsRead(Long sessionId, Integer userType) {
        List<CsMessage> messages = getSessionMessages(sessionId);
        for (CsMessage message : messages) {
            if (message.getIsRead() == 0) {
                // Mark messages from the other party as read
                if ((userType == 1 && message.getSenderType() == CsMessage.SENDER_AGENT) ||
                    (userType == 2 && message.getSenderType() == CsMessage.SENDER_USER)) {
                    message.setIsRead(1);
                    messageMapper.updateById(message);
                }
            }
        }
        return true;
    }

    // ==================== Complaint Management ====================

    /**
     * 创建 complaint。
     * @param complaint 参数 complaint
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Complaint createComplaint(Complaint complaint) {
        complaint.setComplaintNo("CP" + System.currentTimeMillis());
        complaint.setStatus(Complaint.STATUS_PENDING);
        complaint.setCreateTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());

        complaintMapper.insert(complaint);
        return complaint;
    }

    /**
     * 获取 complaint list。
     * @param status 参数 status
     * @param type 参数 type
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<Complaint> getComplaintList(Integer status, Integer type, Long tenantId) {
        LambdaQueryWrapper<Complaint> qw = new LambdaQueryWrapper<>();
        if (status != null) {
            qw.eq(Complaint::getStatus, status);
        }
        if (type != null) {
            qw.eq(Complaint::getComplaintType, type);
        }
        if (tenantId != null) {
            qw.eq(Complaint::getTenantId, tenantId);
        }
        qw.orderByDesc(Complaint::getCreateTime);
        return complaintMapper.selectList(qw);
    }

    /**
     * 获取 complaint by id。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    public Complaint getComplaintById(Long id) {
        return complaintMapper.selectById(id);
    }

    /**
     * 处理 complaint。
     * @param id 参数 id
     * @param handlerId 参数 handlerId
     * @param handlerName 参数 handlerName
     * @param handleResult 参数 handleResult
     * @param compensationAmount 参数 compensationAmount
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleComplaint(Long id, Long handlerId, String handlerName, String handleResult,
            BigDecimal compensationAmount) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            return false;
        }

        complaint.setStatus(Complaint.STATUS_PROCESSING);
        complaint.setHandlerId(handlerId);
        complaint.setHandlerName(handlerName);
        complaint.setHandleResult(handleResult);
        complaint.setCompensationAmount(compensationAmount);
        complaint.setHandleTime(LocalDateTime.now());
        complaint.setUpdateTime(LocalDateTime.now());

        return complaintMapper.updateById(complaint) > 0;
    }

    /**
     * 关闭 complaint。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closeComplaint(Long id) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            return false;
        }

        complaint.setStatus(Complaint.STATUS_CLOSED);
        complaint.setUpdateTime(LocalDateTime.now());

        return complaintMapper.updateById(complaint) > 0;
    }

    /**
     * 处理 rate complaint。
     * @param id 参数 id
     * @param satisfaction 参数 satisfaction
     * @param feedback 参数 feedback
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rateComplaint(Long id, Integer satisfaction, String feedback) {
        Complaint complaint = complaintMapper.selectById(id);
        if (complaint == null) {
            return false;
        }

        complaint.setSatisfaction(satisfaction);
        complaint.setUserFeedback(feedback);
        complaint.setUpdateTime(LocalDateTime.now());

        return complaintMapper.updateById(complaint) > 0;
    }

    // ==================== Statistics ====================

    /**
     * 获取 customer service statistics。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getCustomerServiceStatistics(LocalDateTime startDate, LocalDateTime endDate,
            Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<CsSession> sessionQw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            sessionQw.ge(CsSession::getCreateTime, startDate);
        }
        if (endDate != null) {
            sessionQw.le(CsSession::getCreateTime, endDate);
        }
        if (tenantId != null) {
            sessionQw.eq(CsSession::getTenantId, tenantId);
        }
        List<CsSession> sessions = sessionMapper.selectList(sessionQw);

        int totalSessions = sessions.size();
        int closedSessions = 0;
        int waitingSessions = 0;
        long totalResponseTime = 0;
        int responseCount = 0;
        long totalSatisfaction = 0;
        int ratingCount = 0;

        for (CsSession session : sessions) {
            if (session.getStatus() == CsSession.STATUS_CLOSED) {
                closedSessions++;
                if (session.getSatisfactionRating() != null) {
                    totalSatisfaction += session.getSatisfactionRating();
                    ratingCount++;
                }
            } else if (session.getStatus() == CsSession.STATUS_WAITING) {
                waitingSessions++;
            }

            if (session.getFirstResponseTime() != null && session.getCreateTime() != null) {
                long responseTime = java.time.Duration.between(session.getCreateTime(), session.getFirstResponseTime())
                        .toMinutes();
                totalResponseTime += responseTime;
                responseCount++;
            }
        }

        BigDecimal avgResponseTime = responseCount > 0 ?
                new BigDecimal(totalResponseTime).divide(new BigDecimal(responseCount), 1, java.math.RoundingMode
                        .HALF_UP) :
                BigDecimal.ZERO;

        BigDecimal avgSatisfaction = ratingCount > 0 ?
                new BigDecimal(totalSatisfaction).divide(new BigDecimal(ratingCount), 1, java.math.RoundingMode
                        .HALF_UP) :
                BigDecimal.ZERO;

        result.put("totalSessions", totalSessions);
        result.put("closedSessions", closedSessions);
        result.put("waitingSessions", waitingSessions);
        result.put("avgResponseTime", avgResponseTime);
        result.put("avgSatisfaction", avgSatisfaction);

        return result;
    }

    /**
     * 获取 complaint statistics。
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getComplaintStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<Complaint> complaintQw = new LambdaQueryWrapper<>();
        if (startDate != null) {
            complaintQw.ge(Complaint::getCreateTime, startDate);
        }
        if (endDate != null) {
            complaintQw.le(Complaint::getCreateTime, endDate);
        }
        if (tenantId != null) {
            complaintQw.eq(Complaint::getTenantId, tenantId);
        }
        List<Complaint> complaints = complaintMapper.selectList(complaintQw);

        int totalComplaints = complaints.size();
        int pendingComplaints = 0;
        int processingComplaints = 0;
        int resolvedComplaints = 0;
        int closedComplaints = 0;
        BigDecimal totalCompensation = BigDecimal.ZERO;
        Map<Integer, Integer> typeCountMap = new HashMap<>();

        for (Complaint complaint : complaints) {
            switch (complaint.getStatus()) {
                case Complaint.STATUS_PENDING:
                    pendingComplaints++;
                    break;
                case Complaint.STATUS_PROCESSING:
                    processingComplaints++;
                    break;
                case Complaint.STATUS_RESOLVED:
                    resolvedComplaints++;
                    break;
                case Complaint.STATUS_CLOSED:
                    closedComplaints++;
                    break;
            }

            if (complaint.getCompensationAmount() != null) {
                totalCompensation = totalCompensation.add(complaint.getCompensationAmount());
            }

            typeCountMap.merge(complaint.getComplaintType(), 1, Integer::sum);
        }

        result.put("totalComplaints", totalComplaints);
        result.put("pendingComplaints", pendingComplaints);
        result.put("processingComplaints", processingComplaints);
        result.put("resolvedComplaints", resolvedComplaints);
        result.put("closedComplaints", closedComplaints);
        result.put("totalCompensation", totalCompensation);
        result.put("typeDistribution", typeCountMap);

        return result;
    }

    /**
     * 获取 agent workload。
     * @param agentId 参数 agentId
     * @param startDate 参数 startDate
     * @param endDate 参数 endDate
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getAgentWorkload(Long agentId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<CsSession> sessionQw = new LambdaQueryWrapper<>();
        sessionQw.eq(CsSession::getAgentId, agentId);
        if (startDate != null) {
            sessionQw.ge(CsSession::getCreateTime, startDate);
        }
        if (endDate != null) {
            sessionQw.le(CsSession::getCreateTime, endDate);
        }
        List<CsSession> sessions = sessionMapper.selectList(sessionQw);

        int totalSessions = sessions.size();
        int closedSessions = 0;
        long totalSatisfaction = 0;
        int ratingCount = 0;

        for (CsSession session : sessions) {
            if (session.getStatus() == CsSession.STATUS_CLOSED) {
                closedSessions++;
                if (session.getSatisfactionRating() != null) {
                    totalSatisfaction += session.getSatisfactionRating();
                    ratingCount++;
                }
            }
        }

        BigDecimal avgSatisfaction = ratingCount > 0 ?
                new BigDecimal(totalSatisfaction).divide(new BigDecimal(ratingCount), 1, java.math.RoundingMode
                        .HALF_UP) :
                BigDecimal.ZERO;

        result.put("agentId", agentId);
        result.put("totalSessions", totalSessions);
        result.put("closedSessions", closedSessions);
        result.put("avgSatisfaction", avgSatisfaction);

        return result;
    }
}


