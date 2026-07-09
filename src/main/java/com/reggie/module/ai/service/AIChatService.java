package com.reggie.module.ai.service;

import com.reggie.module.ai.model.AIChatRequest;
import com.reggie.module.ai.model.AIChatResponse;

/**
 * AI聊天服务接口
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface AIChatService {

    /**
     * 通用AI对话
     *
     * @param request 聊天请求
     * @return AI响应
     */
    AIChatResponse chat(AIChatRequest request);

    /**
     * 智能点餐推荐
     *
     * @param userMessage 用户自然语言输入
     * @param userId      用户ID（用于获取偏好）
     * @return AI推荐响应（含菜品列表）
     */
    AIChatResponse orderAssistant(String userMessage, Long userId);

    /**
     * 生成菜品描述
     *
     * @param dishName     菜品名称
     * @param categoryName 分类名称
     * @param ingredients  主要食材
     * @return 生成的描述文本
     */
    String generateDishDescription(String dishName, String categoryName, String ingredients);

    /**
     * 经营数据分析
     *
     * @param question 用户问题
     * @param dataJson 经营数据（JSON格式）
     * @return AI分析结果
     */
    String analyzeBusiness(String question, String dataJson);
}
