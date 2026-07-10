package com.reggie.module.ai.adapter;

import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AiProviderConfig;

import java.util.List;

/**
 * AI模型适配器接口（策略模式）
 * <p>
 * 每种 API 格式（OpenAI / Anthropic / 百度 / DeepSeek / 通义千问 等）
 * 对应一个适配器实现，负责将统一的内部消息格式转换为该厂商的 API 请求格式，
 * 并解析该厂商特有的响应结构。
 * </p>
 *
 * <p><b>扩展新模型只需三步：</b></p>
 * <ol>
 *   <li>实现本接口（或继承 {@link BaseModelAdapter}）</li>
 *   <li>在 {@code AiProviderManager.initAdapters()} 中注册</li>
 *   <li>在后台管理页面配置 apiFormat 与适配器标识一致即可</li>
 * </ol>
 *
 * @author reggie
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
}
