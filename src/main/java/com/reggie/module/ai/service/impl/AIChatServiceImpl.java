package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.entity.Dish;
import com.reggie.mapper.DishMapper;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.model.*;
import com.reggie.module.ai.provider.AIClient;
import com.reggie.module.ai.service.AIChatService;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * AI聊天服务实现
 * 核心逻辑：构建Prompt → 调用AI Provider → 解析结果
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class AIChatServiceImpl implements AIChatService {

    @Resource
    private AIClient aiClient;

    @Resource
    private AIConfigProperties aiConfig;

    @Resource
    private DishMapper dishMapper;

    @Resource
    private PreferenceAnalysisService preferenceAnalysisService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public AIChatResponse chat(AIChatRequest request) {
        List<AIMessage> messages = buildMessages(request);
        AIChatResponse response = aiClient.chat(messages, aiConfig.getMaxTokens(), aiConfig.getTemperature());

        // 点餐场景：尝试解析推荐菜品
        if ("order_assistant".equals(request.getScene()) && response.getContent() != null) {
            List<AIRecommendedDish> dishes = parseRecommendedDishes(response.getContent());
            response.setDishes(dishes);
        }

        return response;
    }

    @Override
    public AIChatResponse orderAssistant(String userMessage, Long userId) {
        // 1. 获取用户偏好标签
        Map<String, Object> context = new LinkedHashMap<>();
        try {
            if (userId != null) {
                // 修改点：调用现有偏好分析方法，组装偏好信息
                String pricePref = preferenceAnalysisService.analyzePricePreference(userId);
                String timePref = preferenceAnalysisService.analyzeTimePreference(userId);
                boolean isHighFreq = preferenceAnalysisService.isHighFrequencyUser(userId);

                Map<String, Object> preferences = new LinkedHashMap<>();
                preferences.put("pricePreference", pricePref);
                preferences.put("timePreference", timePref);
                preferences.put("isHighFrequency", isHighFreq);
                context.put("preferences", preferences);
            }
        } catch (Exception e) {
            log.warn("获取用户偏好失败: userId={}", userId, e);
        }

        // 2. 获取门店在售菜品列表
        List<Dish> availableDishes = dishMapper.selectList(
                new LambdaQueryWrapper<Dish>()
                        .eq(Dish::getStatus, 1)
                        .eq(Dish::getIsDeleted, 0)
                        .orderByDesc(Dish::getSort)
        );
        context.put("dishes", formatDishesForPrompt(availableDishes));

        // 3. 构建请求
        AIChatRequest request = AIChatRequest.builder()
                .message(userMessage)
                .scene("order_assistant")
                .context(context)
                .build();

        return chat(request);
    }

    @Override
    public String generateDishDescription(String dishName, String categoryName, String ingredients) {
        String prompt = String.format(
                "请为以下菜品生成一段吸引人的描述：\n"
                        + "菜名：%s\n"
                        + "分类：%s\n"
                        + "主要食材：%s\n"
                        + "请直接返回描述文本，不要加任何前缀说明。",
                dishName, categoryName, ingredients != null ? ingredients : "暂无");

        List<AIMessage> messages = Arrays.asList(
                AIMessage.builder().role("system").content(aiConfig.getDishDescPrompt()).build(),
                AIMessage.builder().role("user").content(prompt).build()
        );

        AIChatResponse response = aiClient.chat(messages, 500, 0.8);
        return response.getContent();
    }

    @Override
    public String analyzeBusiness(String question, String dataJson) {
        String prompt = "以下是门店经营数据（JSON格式）：\n" + dataJson + "\n\n"
                + "用户问题：" + question + "\n"
                + "请基于数据进行分析回答。";

        List<AIMessage> messages = Arrays.asList(
                AIMessage.builder().role("system").content(aiConfig.getBusinessAnalysisPrompt()).build(),
                AIMessage.builder().role("user").content(prompt).build()
        );

        AIChatResponse response = aiClient.chat(messages, 1500, 0.5);
        return response.getContent();
    }

    /**
     * 构建对话消息列表
     */
    private List<AIMessage> buildMessages(AIChatRequest request) {
        List<AIMessage> messages = new ArrayList<>();

        // System Prompt
        String systemPrompt = getSystemPrompt(request.getScene());
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(AIMessage.builder().role("system").content(systemPrompt).build());
        }

        // 上下文数据注入
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            try {
                String contextJson = OBJECT_MAPPER.writeValueAsString(request.getContext());
                // 将上下文作为system消息追加
                String contextPrompt = "以下是当前可用数据（仅使用真实存在的数据，不要编造）：\n" + contextJson;
                messages.add(AIMessage.builder().role("system").content(contextPrompt).build());
            } catch (Exception e) {
                log.warn("序列化上下文数据失败", e);
            }
        }

        // 历史对话
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            messages.addAll(request.getHistory());
        }

        // 用户消息
        messages.add(AIMessage.builder().role("user").content(request.getMessage()).build());

        return messages;
    }

    /**
     * 根据场景获取System Prompt
     */
    private String getSystemPrompt(String scene) {
        if (scene == null) {
            return aiConfig.getOrderAssistantPrompt();
        }
        switch (scene) {
            case "order_assistant":
                return aiConfig.getOrderAssistantPrompt();
            case "dish_desc":
                return aiConfig.getDishDescPrompt();
            case "business_analysis":
                return aiConfig.getBusinessAnalysisPrompt();
            case "marketing":
                return "你是一个营销文案专家。请根据用户需求生成吸引人的营销文案。"
                        + "文案要有感染力，适合外卖平台推送。";
            default:
                return aiConfig.getOrderAssistantPrompt();
        }
    }

    /**
     * 格式化菜品数据供AI使用
     */
    private String formatDishesForPrompt(List<Dish> dishes) {
        StringBuilder sb = new StringBuilder();
        for (Dish dish : dishes) {
            sb.append(String.format("[%d] %s - ¥%.2f - %s",
                    dish.getId(),
                    dish.getName(),
                    dish.getPrice() != null ? dish.getPrice().doubleValue() : 0,
                    dish.getDescription() != null ? dish.getDescription() : "暂无描述"));
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 解析AI返回的推荐菜品JSON
     */
    private List<AIRecommendedDish> parseRecommendedDishes(String aiContent) {
        List<AIRecommendedDish> result = new ArrayList<>();
        try {
            // 尝试从AI回复中提取JSON部分
            String jsonStr = extractJson(aiContent);
            if (jsonStr != null) {
                JsonNode root = OBJECT_MAPPER.readTree(jsonStr);
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        Long dishId = node.has("dishId") ? node.get("dishId").asLong() : null;
                        String reason = node.has("reason") ? node.get("reason").asText() : "";

                        // 查询菜品真实信息
                        if (dishId != null) {
                            Dish dish = dishMapper.selectById(dishId);
                            if (dish != null) {
                                result.add(AIRecommendedDish.builder()
                                        .dishId(dish.getId())
                                        .name(dish.getName())
                                        .price(dish.getPrice())
                                        .image(dish.getImage())
                                        .reason(reason)
                                        .score(0.9)
                                        .build());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("解析AI推荐菜品失败（Mock模式下正常）: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 从AI回复中提取JSON部分
     */
    private String extractJson(String content) {
        if (content == null) {
            return null;
        }
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        start = content.indexOf('{');
        end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return null;
    }
}
