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

    /**
     * 获取 user conversations。
     * @param userId 参数 userId
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @return 返回结果
     */
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

    /**
     * 获取 conversation messages。
     * @param conversationId 参数 conversationId
     * @return 返回结果
     */
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

    /**
     * 创建 conversation。
     * @param userId 参数 userId
     * @param title 参数 title
     * @param scene 参数 scene
     * @return 返回结果
     */
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

    /**
     * 删除 conversation。
     * @param conversationId 参数 conversationId
     * @param userId 参数 userId
     */
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

    /**
     * 处理 record feedback。
     * @param messageId 参数 messageId
     * @param feedbackType 参数 feedbackType
     * @param userId 参数 userId
     */
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

    /**
     * 获取 context stats。
     * @param conversationId 参数 conversationId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getContextStats(String conversationId) {
        return conversationContextService.getStats(conversationId);
    }

    /**
     * 重置 context。
     * @param conversationId 参数 conversationId
     */
    @Override
    public void resetContext(String conversationId) {
        conversationContextService.clearContext(conversationId);
    }

    /**
     * 搜索 conversations。
     * @param userId 参数 userId
     * @param keyword 参数 keyword
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @return 返回结果
     */
    @Override
    public List<AIConversation> searchConversations(Long userId, String keyword, int page, int pageSize) {
        LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIConversation::getUserId, userId)
                .eq(AIConversation::getIsDeleted, 0)
                .eq(AIConversation::getTenantId, BaseContext.getCurrentTenantId());
        if (keyword != null && !keyword.isEmpty()) {
            // 转义 LIKE 通配符（% _ \），用户输入按字面量匹配，防止通配符注入
            String escaped = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            wrapper.and(w -> w.like(AIConversation::getTitle, escaped)
                    .or().like(AIConversation::getScene, escaped));
        }
        wrapper.orderByDesc(AIConversation::getUpdateTime);
        Page<AIConversation> pageObj = PageUtils.of(page, pageSize);
        conversationMapper.selectPage(pageObj, wrapper);
        return pageObj.getRecords();
    }

    /**
     * 校验 conversation ownership。
     * @param conversationId 参数 conversationId
     * @return 返回结果
     */
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

}