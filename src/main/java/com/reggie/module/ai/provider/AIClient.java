package com.reggie.module.ai.provider;

import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AIChatResponse;

import java.util.List;

/**
 * <p>
 * AI Provider统一接口（策略模式），所有AI服务提供商需实现此接口。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
public interface AIClient {

    /**
     * 发送聊天请求
     *
     * @param messages   消息列表（system/user/assistant）
     * @param maxTokens  最大返回Token数
     * @param temperature 温度参数
     * @return AI响应
     */
    AIChatResponse chat(List<AIMessage> messages, int maxTokens, double temperature);

    /**
     * 获取提供商名称
     *
     * @return 提供商标识
     */
    String getProviderName();

    /**
     * 获取默认模型名称
     *
     * @return 模型名称
     */
    String getDefaultModel();
}
