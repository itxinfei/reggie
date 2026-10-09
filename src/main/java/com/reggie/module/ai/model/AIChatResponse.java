package com.reggie.module.ai.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * AI聊天响应DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Schema(description = "AI 聊天响应")
public class AIChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 回复内容 */
    @Schema(description = "AI 回复内容")
    private String content;

    /** 推荐菜品列表（点餐场景专用） */
    @Schema(description = "推荐菜品列表（点餐场景专用）")
    private List<AIRecommendedDish> dishes;

    /** 使用的模型 */
    @Schema(description = "使用的模型")
    private String model;

    /** Token使用量 */
    @Schema(description = "Token 使用量")
    private Integer tokensUsed;

    /** 附加数据 */
    @Schema(description = "附加数据")
    private Map<String, Object> data;

    public AIChatResponse() {}

    public AIChatResponse(String content, String model) {
        this.content = content;
        this.model = model;
    }

    /**
     * 构建 er。
     * @return 返回结果
     */
    public static AIChatResponseBuilder builder() {
        return new AIChatResponseBuilder();
    }

    /**
     * 获取 content。
     * @return 返回结果
     */
    public String getContent() { return content; }
    /**
     * 设置 content。
     * @param content 参数 content
     */
    public void setContent(String content) { this.content = content; }
    /**
     * 获取 dishes。
     * @return 返回结果
     */
    public List<AIRecommendedDish> getDishes() { return dishes; }
    /**
     * 设置 dishes。
     * @param dishes 参数 dishes
     */
    public void setDishes(List<AIRecommendedDish> dishes) { this.dishes = dishes; }
    /**
     * 获取 model。
     * @return 返回结果
     */
    public String getModel() { return model; }
    /**
     * 设置 model。
     * @param model 参数 model
     */
    public void setModel(String model) { this.model = model; }
    /**
     * 获取 tokens used。
     * @return 返回结果
     */
    public Integer getTokensUsed() { return tokensUsed; }
    /**
     * 设置 tokens used。
     * @param tokensUsed 参数 tokensUsed
     */
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
    /**
     * 获取 data。
     * @return 返回结果
     */
    public Map<String, Object> getData() { return data; }
    /**
     * 设置 data。
     * @param data 参数 data
     */
    public void setData(Map<String, Object> data) { this.data = data; }

    /**
     * AIChatResponseBuilder。
     */
    public static class AIChatResponseBuilder {
        private AIChatResponse r = new AIChatResponse();

        /**
         * 处理 content。
         * @param v 参数 v
         * @return 返回结果
         */
        public AIChatResponseBuilder content(String v) { r.content = v; return this; }
        /**
         * 处理 dishes。
         * @param v 参数 v
         * @return 返回结果
         */
        public AIChatResponseBuilder dishes(List<AIRecommendedDish> v) { r.dishes = v; return this; }
        /**
         * 处理 model。
         * @param v 参数 v
         * @return 返回结果
         */
        public AIChatResponseBuilder model(String v) { r.model = v; return this; }
        /**
         * 处理 tokens used。
         * @param v 参数 v
         * @return 返回结果
         */
        public AIChatResponseBuilder tokensUsed(Integer v) { r.tokensUsed = v; return this; }
        /**
         * 处理 data。
         * @param v 参数 v
         * @return 返回结果
         */
        public AIChatResponseBuilder data(Map<String, Object> v) { r.data = v; return this; }
        /**
         * 构建。
         * @return 返回结果
         */
        public AIChatResponse build() { return r; }
    }
}
