package com.reggie.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI模块配置属性
 * 支持OpenAI兼容API（通义千问/DeepSeek/OpenAI等）
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "reggie.ai")
public class AIConfigProperties {

    /** AI服务提供商：openai / qwen / deepseek / ollama */
    private String provider = "deepseek";

    /** API密钥 */
    private String apiKey = "";

    /** API基础URL */
    private String baseUrl = "https://api.deepseek.com/v1";

    /** 默认模型名称 */
    private String model = "deepseek-chat";

    /** 请求超时时间（秒） */
    private int timeout = 60;

    /** 最大Token数 */
    private int maxTokens = 2048;

    /** 温度参数（0-2，越高越随机） */
    private double temperature = 0.7;

    /** 是否启用AI功能 */
    private boolean enabled = true;

    /** 点餐助手System Prompt */
    private String orderAssistantPrompt = "你是一个专业的餐饮推荐助手，名叫「小吉」。"
            + "你的任务是根据用户的需求和偏好，从当前门店的菜品中智能推荐最合适的菜品。\n"
            + "规则：\n"
            + "1. 只推荐门店真实存在的菜品，不要编造菜品\n"
            + "2. 考虑用户的口味偏好、预算、人数等因素\n"
            + "3. 推荐要多样化，荤素搭配\n"
            + "4. 回复简洁友好，用中文\n"
            + "5. 推荐理由要具体，说明为什么适合用户\n"
            + "6. 输出格式为JSON数组，每个菜品包含：dishId（菜品ID）、name（菜名）、reason（推荐理由）";

    /** 菜品描述生成Prompt */
    private String dishDescPrompt = "你是一个专业的美食文案写手。"
            + "请根据菜名和基本信息，生成一段吸引人的菜品描述。\n"
            + "要求：\n"
            + "1. 描述食材、口味、烹饪方式\n"
            + "2. 语言生动诱人，适合外卖平台展示\n"
            + "3. 长度控制在50-150字\n"
            + "4. 返回纯文本，不要加任何标记";

    /** 经营分析Prompt */
    private String businessAnalysisPrompt = "你是一个餐饮经营数据分析师。"
            + "请根据提供的经营数据，回答用户关于经营状况的问题。\n"
            + "要求：\n"
            + "1. 基于数据事实回答，不要编造数据\n"
            + "2. 给出具体数字和趋势分析\n"
            + "3. 提供可行的经营建议\n"
            + "4. 回复简洁专业";
}
