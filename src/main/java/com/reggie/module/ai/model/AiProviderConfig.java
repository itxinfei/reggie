package com.reggie.module.ai.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * AI供应商配置
 * 管理员可在后台配置和切换不同的大模型供应商
 *
 * @author reggie
 * @since 2026-07-10
 */
@Data
@TableName("ai_provider_config")
public class AiProviderConfig {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 供应商编码：deepseek / qwen / zhipu / ernie / baichuan / moonshot / minimax / custom */
    private String providerCode;

    /** 供应商名称（展示用）：DeepSeek / 通义千问 / 智谱AI / 文心一言 / 百川智能 / 月之暗面 / MiniMax / 自定义 */
    private String providerName;

    /** API基础URL */
    private String baseUrl;

    /** 模型名称：deepseek-chat / qwen-turbo / glm-4 / ernie-4.0 / baichuan2-turbo / moonshot-v1-8k / abab6 / 等 */
    private String modelName;

    /** API密钥（加密存储） */
    private String apiKey;

    /** 请求超时时间（秒） */
    private Integer timeout;

    /** 最大Token数 */
    private Integer maxTokens;

    /** 温度参数（0-2） */
    private Double temperature;

    /** API格式类型：openai_compatible / baidu / 360 / custom */
    private String apiFormat;

    /** 额外请求头（JSON），如 {"api-key": "xxx"} 百度专用 */
    private String extraHeaders;

    /** 请求体映射模板（JSON），用于非OpenAI格式的适配 */
    private String requestTemplate;

    /** 响应解析路径（JSONPath），用于非OpenAI格式的响应提取 */
    private String responsePath;

    /** 供应商图标/Logo URL */
    private String iconUrl;

    /** 是否启用 */
    private Boolean enabled;

    /** 是否是当前激活的供应商 */
    private Boolean isActive;

    /** 最后测试时间 */
    private LocalDateTime lastTestTime;

    /** 最后测试结果：success / fail */
    private String lastTestResult;

    /** 排序号（越小越靠前） */
    private Integer sort;

    /** 备注 */
    private String remark;

    // ========== 审计字段 ==========

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;
}
