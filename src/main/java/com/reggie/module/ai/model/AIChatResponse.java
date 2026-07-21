package com.reggie.module.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * AI聊天响应DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
public class AIChatResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 回复内容 */
    private String content;

    /** 推荐菜品列表（点餐场景专用） */
    private List<AIRecommendedDish> dishes;

    /** 使用的模型 */
    private String model;

    /** Token使用量 */
    private Integer tokensUsed;

    /** 附加数据 */
    private Map<String, Object> data;

    public AIChatResponse() {}

    public AIChatResponse(String content, String model) {
        this.content = content;
        this.model = model;
    }

    public static AIChatResponseBuilder builder() {
        return new AIChatResponseBuilder();
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<AIRecommendedDish> getDishes() { return dishes; }
    public void setDishes(List<AIRecommendedDish> dishes) { this.dishes = dishes; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getTokensUsed() { return tokensUsed; }
    public void setTokensUsed(Integer tokensUsed) { this.tokensUsed = tokensUsed; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public static class AIChatResponseBuilder {
        private AIChatResponse r = new AIChatResponse();

        public AIChatResponseBuilder content(String v) { r.content = v; return this; }
        public AIChatResponseBuilder dishes(List<AIRecommendedDish> v) { r.dishes = v; return this; }
        public AIChatResponseBuilder model(String v) { r.model = v; return this; }
        public AIChatResponseBuilder tokensUsed(Integer v) { r.tokensUsed = v; return this; }
        public AIChatResponseBuilder data(Map<String, Object> v) { r.data = v; return this; }
        public AIChatResponse build() { return r; }
    }
}
