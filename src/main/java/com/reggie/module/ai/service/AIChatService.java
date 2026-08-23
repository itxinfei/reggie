package com.reggie.module.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.ai.model.AIChatRequest;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIConversation;
import com.reggie.module.ai.model.AIMessageRecord;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * AI聊天服务接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface AIChatService extends IService<AIConversation> {

    /**
     * 通用AI对话（流式）
     *
     * @param request 聊天请求
     * @return SSE流式响应
     */
    SseEmitter chatStream(AIChatRequest request);

    /**
     * 通用AI对话（非流式）
     *
     * @param request 聊天请求
     * @return AI响应
     */
    AIChatResponse chat(AIChatRequest request);

    /**
     * 智能点餐推荐（流式）
     */
    SseEmitter orderAssistantStream(String userMessage, Long userId, String conversationId);

    /**
     * 智能点餐推荐（非流式）
     * 修改点：新增conversationId参数，避免Controller和Service各自创建对话导致孤立数据
     */
    AIChatResponse orderAssistant(String userMessage, Long userId, String conversationId);

    /**
     * 生成菜品描述
     */
    String generateDishDescription(String dishName, String categoryName, String ingredients);

    /**
     * 经营数据分析
     */
    String analyzeBusiness(String question, String dataJson);

    // ==================== 对话管理 ====================

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

    // ==================== 上下文记忆 ====================

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
     * <p>域4 改造：从 AIChatController 下沉，Controller 不再直接操作 Mapper</p>
     *
     * @param userId   当前用户ID
     * @param keyword  搜索关键词（同时匹配 title 和 scene）
     * @param page     页码
     * @param pageSize 每页条数
     * @return 匹配的对话列表
     */
    List<AIConversation> searchConversations(Long userId, String keyword, int page, int pageSize);

    /**
     * 验证对话所有权（按对话ID查询 userId 和 isDeleted）
     * <p>域4 改造：从 AIChatController 下沉</p>
     *
     * @param conversationId 对话ID
     * @return 对话所属用户ID（未找到或已删除返回 null）
     */
    Long validateConversationOwnership(String conversationId);
}
