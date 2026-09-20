package com.reggie.module.ai.adapter;

import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.model.ModelTurn;
import com.reggie.module.ai.tool.ToolDefinition;

import java.util.List;

/**
 * <p>
 * AI模型适配器接口（策略模式）
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-10
 */
public interface AiModelAdapter {

    /**
     * 适配器唯一标识，与 {@link AiProviderConfig#getApiFormat()} 对应
     * <p>示例：{@code "openai"}、{@code "anthropic"}、{@code "baidu"}、{@code "deepseek"} 等</p>
     */
    String getFormatId();

    /**
     * 适配器显示名称（用于日志和管理界面）
     */
    String getDisplayName();

    /**
     * 发送聊天请求并解析响应
     *
     * @param messages    对话消息列表
     * @param maxTokens   最大 token 数（0 表示使用配置默认值）
     * @param temperature 温度参数（负数表示使用配置默认值）
     * @param config      供应商配置（包含 baseUrl、apiKey、modelName 等）
     * @return AI 响应，永远不会返回 null
     */
    AIChatResponse chat(List<AIMessage> messages, int maxTokens, double temperature, AiProviderConfig config);

    /**
     * 是否支持 SSE 流式输出
     * <p>默认返回 false，适配器按需覆盖。</p>
     */
    default boolean supportsStreaming() {
        return false;
    }

    /**
     * SSE 流式聊天：逐 token 推送响应
     *
     * <p>调用方传入 {@link StreamCallback}，适配器在收到每个 SSE 事件时回调。
     * 返回完整响应内容（用于持久化）。</p>
     *
     * @param messages    对话消息列表
     * @param maxTokens   最大 token 数
     * @param temperature 温度参数
     * @param config      供应商配置
     * @param callback    流式回调
     * @return 完整响应内容（与 chat() 返回值相同的 content）
     */
    default String chatStream(List<AIMessage> messages, int maxTokens, double temperature,
                              AiProviderConfig config, StreamCallback callback) throws Exception {
        // 默认回退到非流式
        AIChatResponse response = chat(messages, maxTokens, temperature, config);
        if (response != null && response.getContent() != null) {
            callback.onToken(response.getContent(), true);
        }
        return response != null ? response.getContent() : null;
    }

    /**
     * 是否支持 function calling（工具调用）。
     * <p>除供应商配置 capabilities.tools 勾选外，还需适配器在协议层真正支持；
     * Baidu 压平协议不支持，默认 false。</p>
     */
    default boolean supportsToolCalling() {
        return false;
    }

    /**
     * 单轮工具感知对话：模型可能返回自然语言（经 textSink 流式推送），
     * 也可能返回 {@link ModelTurn#getToolCalls()} 请求调用工具，由编排层执行后续轮。
     *
     * @param messages    对话消息（可含 assistant.toolCalls 与 role=tool 结果消息）
     * @param maxTokens   最大 token 数
     * @param temperature 温度参数
     * @param config      供应商配置
     * @param tools       可用工具定义；空列表表示本轮不提供工具（强制模型总结作答）
     * @param abort       中止回调（用户点停止时断开上游）
     * @param textSink    文本增量回调（只推 token，不使用 isLast 语义）
     * @return 本轮结果；上游错误返回 {@link ModelTurn#error(String)}
     */
    default ModelTurn chatTurn(List<AIMessage> messages, int maxTokens, double temperature,
                               AiProviderConfig config, List<ToolDefinition> tools,
                               AbortableStreamCallback abort, StreamCallback textSink) throws Exception {
        throw new UnsupportedOperationException(getFormatId() + " 适配器不支持工具调用");
    }

    /**
     * 流式回调接口
     */
    interface StreamCallback {
        /**
         * 收到一个 token / 文本块
         *
         * @param token      当前文本块
         * @param isLast     是否为最后一个块
         */
        void onToken(String token, boolean isLast);
    }
}
