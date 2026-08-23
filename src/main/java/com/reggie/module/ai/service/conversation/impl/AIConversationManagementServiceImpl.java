package com.reggie.module.ai.service.conversation.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.ai.mapper.AIConversationMapper;
import com.reggie.module.ai.mapper.AIMessageRecordMapper;
import com.reggie.module.ai.model.AIConversation;
import com.reggie.module.ai.model.AIMessageRecord;
import com.reggie.module.ai.service.ConversationContextService;
import com.reggie.module.ai.service.conversation.AIConversationManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对话管理服务实现
 */
@Slf4j
@Service
public class AIConversationManagementServiceImpl
        extends ServiceImpl<AIConversationMapper, AIConversation>
        implements AIConversationManagementService {

    @Resource
    private AIConversationMapper conversationMapper;

    @Resource
    private AIMessageRecordMapper messageRecordMapper;

    @Resource
    private ConversationContextService conversationContextService;

    /** 单次对话携带的最大历史消息数 */
    private static final int MAX_HISTORY_MESSAGES = 20;

    @Override
    public List<AIConversation> getUserConversations(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIConversation::getUserId, userId)
                .eq(AIConversation::getIsDeleted, 0)
                .eq(AIConversation::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(AIConversation::getUpdateTime);
        Page<AIConversation> pageObj = PageUtils.of(page, pageSize);
        conversationMapper.selectPage(pageObj, wrapper);
        return pageObj.getRecords();
    }

    @Override
    public List<AIMessageRecord> getConversationMessages(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return Collections.emptyList();
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<AIConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.select(AIConversation::getUserId)
                .eq(AIConversation::getConversationId, conversationId)
                .eq(AIConversation::getIsDeleted, 0)
                .eq(AIConversation::getTenantId, BaseContext.getCurrentTenantId());
        AIConversation conv = conversationMapper.selectOne(convWrapper);
        if (conv == null || !currentUserId.equals(conv.getUserId())) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<AIMessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessageRecord::getConversationId, conversationId)
                .eq(AIMessageRecord::getIsDeleted, 0)
                .orderByAsc(AIMessageRecord::getCreateTime);
        return messageRecordMapper.selectList(wrapper);
    }

    @Override
    public AIConversation createConversation(Long userId, String title, String scene) {
        AIConversation conv = new AIConversation();
        conv.setConversationId(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        conv.setUserId(userId);
        conv.setTitle(title != null ? title : "新对话");
        conv.setScene(scene);
        conv.setMessageCount(0);
        conv.setIsDeleted(0);
        conv.setCreateTime(LocalDateTime.now());
        conv.setUpdateTime(LocalDateTime.now());
        conversationMapper.insert(conv);
        return conv;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String conversationId, Long userId) {
        LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIConversation::getConversationId, conversationId)
                .eq(AIConversation::getUserId, userId)
                .eq(AIConversation::getIsDeleted, 0)
                .eq(AIConversation::getTenantId, BaseContext.getCurrentTenantId());
        AIConversation conv = conversationMapper.selectOne(wrapper);
        if (conv != null) {
            conv.setIsDeleted(1);
            conversationMapper.updateById(conv);
            LambdaUpdateWrapper<AIMessageRecord> msgUpdateWrapper = new LambdaUpdateWrapper<>();
            msgUpdateWrapper.eq(AIMessageRecord::getConversationId, conversationId)
                    .eq(AIMessageRecord::getIsDeleted, 0)
                    .set(AIMessageRecord::getIsDeleted, 1);
            messageRecordMapper.update(null, msgUpdateWrapper);
            conversationContextService.clearContext(conversationId);
        }
    }

    @Override
    public void recordFeedback(Long messageId, String feedbackType, Long userId) {
        if (messageId == null) return;
        LambdaQueryWrapper<AIMessageRecord> fbWrapper = new LambdaQueryWrapper<>();
        fbWrapper.eq(AIMessageRecord::getId, messageId)
                .eq(AIMessageRecord::getTenantId, BaseContext.getCurrentTenantId());
        AIMessageRecord record = messageRecordMapper.selectOne(fbWrapper);
        if (record != null && (record.getUserId() == null || record.getUserId().equals(userId))) {
            record.setFeedback(feedbackType);
            messageRecordMapper.updateById(record);
        }
    }

    @Override
    public Map<String, Object> getContextStats(String conversationId) {
        return conversationContextService.getStats(conversationId);
    }

    @Override
    public void resetContext(String conversationId) {
        conversationContextService.clearContext(conversationId);
    }

    @Override
    public List<AIConversation> searchConversations(Long userId, String keyword, int page, int pageSize) {
        LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIConversation::getUserId, userId)
                .eq(AIConversation::getIsDeleted, 0)
                .eq(AIConversation::getTenantId, BaseContext.getCurrentTenantId());
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(AIConversation::getTitle, keyword)
                    .or().like(AIConversation::getScene, keyword));
        }
        wrapper.orderByDesc(AIConversation::getUpdateTime);
        Page<AIConversation> pageObj = PageUtils.of(page, pageSize);
        conversationMapper.selectPage(pageObj, wrapper);
        return pageObj.getRecords();
    }

    @Override
    public Long validateConversationOwnership(String conversationId) {
        LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(AIConversation::getUserId)
                .eq(AIConversation::getConversationId, conversationId)
                .eq(AIConversation::getIsDeleted, 0)
                .eq(AIConversation::getTenantId, BaseContext.getCurrentTenantId());
        AIConversation conversation = conversationMapper.selectOne(wrapper);
        return conversation != null ? conversation.getUserId() : null;
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从数据库加载会话最近的历史消息（用于多轮对话上下文）
     */
    private List<AIMessageRecord> getRecentMessages(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<AIMessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessageRecord::getConversationId, conversationId)
                .eq(AIMessageRecord::getIsDeleted, 0)
                .eq(AIMessageRecord::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(AIMessageRecord::getCreateTime);
        Page<AIMessageRecord> pageObj = PageUtils.of(1, MAX_HISTORY_MESSAGES);
        messageRecordMapper.selectPage(pageObj, wrapper);
        List<AIMessageRecord> records = pageObj.getRecords();
        Collections.reverse(records);
        return records;
    }

    /**
     * 更新对话的消息计数和最后更新时间（原子递增，避免竞态条件）
     */
    private void updateMessageCount(String conversationId) {
        try {
            LambdaUpdateWrapper<AIConversation> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(AIConversation::getConversationId, conversationId)
                    .eq(AIConversation::getIsDeleted, 0)
                    .setSql("message_count = IFNULL(message_count, 0) + 1")
                    .set(AIConversation::getUpdateTime, LocalDateTime.now());
            conversationMapper.update(null, wrapper);
        } catch (Exception e) {
            log.warn("更新消息计数失败: conversationId={}", conversationId, e);
        }
    }

    /**
     * 更新对话标题（取用户首条消息的前20字符作为标题）
     */
    private void updateConversationTitle(String conversationId, String firstMessage) {
        if (conversationId == null || firstMessage == null) {
            return;
        }
        try {
            LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AIConversation::getConversationId, conversationId)
                    .eq(AIConversation::getIsDeleted, 0);
            AIConversation conv = conversationMapper.selectOne(wrapper);
            if (conv != null && ("新对话".equals(conv.getTitle()) || conv.getTitle() == null)) {
                String title = firstMessage.length() > 20
                        ? firstMessage.substring(0, 20) + "..."
                        : firstMessage;
                conv.setTitle(title);
                conversationMapper.updateById(conv);
            }
        } catch (Exception e) {
            log.warn("更新对话标题失败: conversationId={}", conversationId, e);
        }
    }
}