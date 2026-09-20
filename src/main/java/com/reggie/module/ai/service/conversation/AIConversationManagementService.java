package com.reggie.module.ai.service.conversation;

import com.reggie.module.ai.model.AIConversation;
import com.reggie.module.ai.model.AIMessageRecord;

import java.util.List;
import java.util.Map;

/**
 * 对话管理服务
 * <p>
 * 负责对话的 CRUD、消息查询、用户反馈及上下文统计，
 * 从 {@code AIChatService} 中拆出以减小主服务类体积。
 * 不包含模型调用/消息持久化逻辑，仅处理对话元数据与权限校验。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-17
 */
public interface AIConversationManagementService {

    /**
     * 获取用户的对话列表
     */
    List<AIConversation> getUserConversations(Long userId, int page, int pageSize);

    /**
     * 获取对话详情（含消息）
     */
    List<AIMessageRecord> getConversationMessages(String conversationId);

    /**
     * 创建新对话
     */
    AIConversation createConversation(Long userId, String title, String scene);

    /**
     * 删除对话（软删除）
     */
    void deleteConversation(String conversationId, Long userId);

    /**
     * 记录用户反馈
     */
    void recordFeedback(Long messageId, String feedbackType, Long userId);

    /**
     * 获取对话上下文统计信息
     */
    Map<String, Object> getContextStats(String conversationId);

    /**
     * 重置对话上下文（清除缓存，保留历史记录）
     */
    void resetContext(String conversationId);

    /**
     * 按标题关键词搜索对话（含用户过滤和逻辑删除过滤）
     */
    List<AIConversation> searchConversations(Long userId, String keyword, int page, int pageSize);

    /**
     * 验证对话所有权（按对话ID查询 userId 和 isDeleted）
     *
     * @return 对话所属用户ID（未找到或已删除返回 null）
     */
    Long validateConversationOwnership(String conversationId);

    /**
     * 获取指定身份类型的对话列表（员工 EMPLOYEE 与 C 端用户 CUSTOMER ID 空间隔离）
     */
    List<AIConversation> getUserConversations(Long userId, String actorType, int page, int pageSize);

    /**
     * 按身份双校验获取对话消息历史（userId + actorType 均匹配才返回）
     */
    List<AIMessageRecord> getConversationMessages(String conversationId, Long userId, String actorType);

    /**
     * 创建带身份类型的新对话
     */
    AIConversation createConversation(Long userId, String actorType, String title, String scene);

    /**
     * 按身份双校验删除对话（软删除）
     */
    void deleteConversation(String conversationId, Long userId, String actorType);

    /**
     * 重命名对话（身份双校验，标题限长由调用方校验）
     *
     * @return true 表示命中并更新成功
     */
    boolean renameConversation(String conversationId, Long userId, String actorType, String title);

    /**
     * 删除单条消息（身份双校验 + 上下文缓存失效，消息必须属于该对话）
     *
     * @return true 表示删除成功
     */
    boolean deleteMessage(String conversationId, Long messageId, Long userId, String actorType);

    /**
     * 身份双校验对话所有权
     *
     * @return 对话所属用户ID（不存在/已删除/身份不匹配均返回 null）
     */
    Long validateConversationOwnership(String conversationId, Long userId, String actorType);

    /**
     * 按身份类型搜索对话（标题/场景关键词）
     */
    List<AIConversation> searchConversations(Long userId, String actorType, String keyword, int page, int pageSize);
}