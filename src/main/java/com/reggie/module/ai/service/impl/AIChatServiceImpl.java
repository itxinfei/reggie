package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.entity.Dish;
import com.reggie.mapper.DishMapper;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.mapper.AIConversationMapper;
import com.reggie.module.ai.mapper.AIMessageRecordMapper;
import com.reggie.module.ai.model.*;
import com.reggie.module.ai.provider.AiProviderManager;
import com.reggie.module.ai.service.AIChatService;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import com.reggie.module.ai.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * AI聊天服务实现
 * 核心逻辑：构建Prompt → 调用AI Provider → 解析结果
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class AIChatServiceImpl extends ServiceImpl<AIConversationMapper, AIConversation> implements AIChatService {

    @Resource
    private AiProviderManager aiProviderManager;

    @Resource
    private AIConfigProperties aiConfig;

    @Resource
    private DishMapper dishMapper;

    @Resource
    private PreferenceAnalysisService preferenceAnalysisService;

    @Resource
    private UserProfileService userProfileService;

    @Resource
    private AIConversationMapper conversationMapper;

    @Resource
    private AIMessageRecordMapper messageRecordMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ==================== 流式对话 ====================

    @Override
    public SseEmitter chatStream(AIChatRequest request) {
        SseEmitter emitter = new SseEmitter(aiConfig.getTimeout() * 1000L);
        emitter.onTimeout(() -> log.warn("SSE连接超时"));
        emitter.onError((e) -> log.warn("SSE连接错误", e));
        emitter.onCompletion(() -> log.debug("SSE连接完成"));

        CompletableFuture.runAsync(() -> {
            try {
                List<AIMessage> messages = buildMessages(request);
                AIChatResponse response = aiProviderManager.chat(messages, aiConfig.getMaxTokens(), aiConfig.getTemperature());

                if (response != null && response.getContent() != null) {
                    // 流式发送内容（逐字符模拟，实际应由AI provider支持流式）
                    String content = response.getContent();
                    String[] chunks = splitIntoChunks(content, 20);
                    for (int i = 0; i < chunks.length; i++) {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(chunks[i]));
                        Thread.sleep(30);
                    }
                }

                // 发送完成事件
                emitter.send(SseEmitter.event().name("done").data("complete"));

                // 点餐场景：解析推荐菜品
                if ("order_assistant".equals(request.getScene()) && response != null && response.getContent() != null) {
                    List<AIRecommendedDish> dishes = parseRecommendedDishes(response.getContent());
                    if (dishes != null && !dishes.isEmpty()) {
                        StringBuilder dishJson = new StringBuilder();
                        dishJson.append(OBJECT_MAPPER.writeValueAsString(dishes));
                        emitter.send(SseEmitter.event().name("dishes").data(dishJson.toString()));
                    }
                }

                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("服务暂时不可用，请稍后重试"));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        });

        return emitter;
    }

    @Override
    public SseEmitter orderAssistantStream(String userMessage, Long userId, String conversationId) {
        Map<String, Object> context = new LinkedHashMap<>();
        try {
            if (userId != null) {
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

        List<Dish> availableDishes = dishMapper.selectList(
                new LambdaQueryWrapper<Dish>()
                        .eq(Dish::getStatus, 1)
                        .eq(Dish::getIsDeleted, 0)
                        .orderByDesc(Dish::getSort)
        );
        context.put("dishes", formatDishesForPrompt(availableDishes));

        // 创建或获取对话
        if (conversationId == null || conversationId.isEmpty()) {
            AIConversation conv = createConversation(userId, null, "order_assistant");
            conversationId = conv.getConversationId();
        }

        AIChatRequest request = AIChatRequest.builder()
                .message(userMessage)
                .scene("order_assistant")
                .conversationId(conversationId)
                .context(context)
                .build();

        return chatStream(request);
    }

    // ==================== 非流式对话 ====================

    @Override
    public AIChatResponse chat(AIChatRequest request) {
        // 异步刷新用户画像（不阻塞对话）
        if (request.getUserId() != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    userProfileService.refreshProfile(request.getUserId());
                } catch (Exception e) {
                    log.warn("异步刷新用户画像失败: userId={}", request.getUserId(), e);
                }
            });
        }

        List<AIMessage> messages = buildMessages(request);
        // 优先使用供应商配置中的参数，回退到 application.yml
        AiProviderConfig providerConfig = aiProviderManager.getActiveConfig();
        int maxTokens = providerConfig.getMaxTokens() != null ? providerConfig.getMaxTokens() : aiConfig.getMaxTokens();
        double temperature = providerConfig.getTemperature() != null ? providerConfig.getTemperature() : aiConfig.getTemperature();
        AIChatResponse response = aiProviderManager.chat(messages, maxTokens, temperature);

        if ("order_assistant".equals(request.getScene()) && response != null && response.getContent() != null) {
            List<AIRecommendedDish> dishes = parseRecommendedDishes(response.getContent());
            response.setDishes(dishes);
        }

        return response != null ? response : AIChatResponse.builder()
                .content("AI服务暂时不可用，请稍后重试。")
                .model(aiConfig.getModel())
                .build();
    }

    @Override
    public AIChatResponse orderAssistant(String userMessage, Long userId) {
        // 异步刷新用户画像（不阻塞对话）
        if (userId != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    userProfileService.refreshProfile(userId);
                } catch (Exception e) {
                    log.warn("异步刷新用户画像失败: userId={}", userId, e);
                }
            });
        }

        Map<String, Object> context = new LinkedHashMap<>();
        try {
            if (userId != null) {
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

        List<Dish> availableDishes = dishMapper.selectList(
                new LambdaQueryWrapper<Dish>()
                        .eq(Dish::getStatus, 1)
                        .eq(Dish::getIsDeleted, 0)
                        .orderByDesc(Dish::getSort)
        );
        context.put("dishes", formatDishesForPrompt(availableDishes));

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

        AIChatResponse response = aiProviderManager.chat(messages, 500, 0.8);
        return response != null ? response.getContent() : null;
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

        AIChatResponse response = aiProviderManager.chat(messages, 1500, 0.5);
        return response != null ? response.getContent() : null;
    }

    // ==================== 对话管理 ====================

    @Override
    public List<AIConversation> getUserConversations(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIConversation::getUserId, userId)
                .eq(AIConversation::getIsDeleted, 0)
                .orderByDesc(AIConversation::getUpdateTime)
                .last("LIMIT " + ((page - 1) * pageSize) + ", " + pageSize);
        return conversationMapper.selectList(wrapper);
    }

    @Override
    public List<AIMessageRecord> getConversationMessages(String conversationId) {
        LambdaQueryWrapper<AIMessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessageRecord::getConversationId, conversationId)
                .eq(AIMessageRecord::getIsDeleted, 0)
                .orderByAsc(AIMessageRecord::getCreateTime);
        return messageRecordMapper.selectList(wrapper);
    }

    @Override
    public AIConversation createConversation(Long userId, String title, String scene) {
        AIConversation conv = new AIConversation();
        conv.setConversationId(UUID.randomUUID().toString().replace("-", "").substring(0, 24));
        conv.setUserId(userId);
        conv.setTitle(title != null ? title : "新对话");
        conv.setScene(scene);
        conv.setMessageCount(0);
        conv.setIsDeleted(0);
        conversationMapper.insert(conv);
        return conv;
    }

    @Override
    public void deleteConversation(String conversationId, Long userId) {
        LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIConversation::getConversationId, conversationId)
                .eq(AIConversation::getUserId, userId)
                .eq(AIConversation::getIsDeleted, 0);
        AIConversation conv = conversationMapper.selectOne(wrapper);
        if (conv != null) {
            conv.setIsDeleted(1);
            conversationMapper.updateById(conv);
        }
    }

    @Override
    public void saveMessage(AIMessageRecord record) {
        if (record.getConversationId() != null) {
            messageRecordMapper.insert(record);
            // 更新会话消息计数
            LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AIConversation::getConversationId, record.getConversationId());
            AIConversation conv = conversationMapper.selectOne(wrapper);
            if (conv != null) {
                conv.setMessageCount((conv.getMessageCount() != null ? conv.getMessageCount() : 0) + 1);
                conversationMapper.updateById(conv);
            }
        }
    }

    @Override
    public void recordFeedback(Long messageId, String feedbackType, Long userId) {
        if (messageId == null) return;
        AIMessageRecord record = messageRecordMapper.selectById(messageId);
        if (record != null && record.getUserId().equals(userId)) {
            record.setFeedback(feedbackType);
            messageRecordMapper.updateById(record);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构建对话消息列表
     */
    private List<AIMessage> buildMessages(AIChatRequest request) {
        List<AIMessage> messages = new ArrayList<>();

        String systemPrompt = getSystemPrompt(request.getScene());
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(AIMessage.builder().role("system").content(systemPrompt).build());
        }

        // 注入用户长期记忆画像
        if (request.getUserId() != null) {
            try {
                String profileSummary = userProfileService.buildProfileSummary(request.getUserId());
                if (profileSummary != null && !profileSummary.isEmpty()) {
                    messages.add(AIMessage.builder().role("system")
                            .content("【用户长期记忆】\n" + profileSummary + "\n请严格遵守以上用户偏好进行推荐。")
                            .build());
                }
            } catch (Exception e) {
                log.warn("注入用户画像失败: userId={}", request.getUserId(), e);
            }
        }

        if (request.getContext() != null && !request.getContext().isEmpty()) {
            try {
                String contextJson = OBJECT_MAPPER.writeValueAsString(request.getContext());
                String contextPrompt = "以下是当前可用数据（仅使用真实存在的数据，不要编造）：\n" + contextJson;
                messages.add(AIMessage.builder().role("system").content(contextPrompt).build());
            } catch (Exception e) {
                log.warn("序列化上下文数据失败", e);
            }
        }

        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            messages.addAll(request.getHistory());
        }

        messages.add(AIMessage.builder().role("user").content(request.getMessage()).build());
        return messages;
    }

    private String getSystemPrompt(String scene) {
        if (scene == null) return aiConfig.getOrderAssistantPrompt();
        switch (scene) {
            case "order_assistant": return aiConfig.getOrderAssistantPrompt();
            case "dish_desc": return aiConfig.getDishDescPrompt();
            case "business_analysis": return aiConfig.getBusinessAnalysisPrompt();
            case "marketing":
                return "你是一个营销文案专家。请根据用户需求生成吸引人的营销文案。文案要有感染力，适合外卖平台推送。";
            default: return aiConfig.getOrderAssistantPrompt();
        }
    }

    private String formatDishesForPrompt(List<Dish> dishes) {
        StringBuilder sb = new StringBuilder();
        for (Dish dish : dishes) {
            sb.append(String.format("[%d] %s - ¥%.2f - %s",
                    dish.getId(), dish.getName(),
                    dish.getPrice() != null ? dish.getPrice().doubleValue() : 0,
                    dish.getDescription() != null ? dish.getDescription() : "暂无描述"));
            sb.append("\n");
        }
        return sb.toString();
    }

    private List<AIRecommendedDish> parseRecommendedDishes(String aiContent) {
        List<AIRecommendedDish> result = new ArrayList<>();
        try {
            String jsonStr = extractJson(aiContent);
            if (jsonStr != null) {
                JsonNode root = OBJECT_MAPPER.readTree(jsonStr);
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        Long dishId = node.has("dishId") ? node.get("dishId").asLong() : null;
                        String reason = node.has("reason") ? node.get("reason").asText() : "";
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

    private String extractJson(String content) {
        if (content == null) return null;
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start >= 0 && end > start) return content.substring(start, end + 1);
        start = content.indexOf('{');
        end = content.lastIndexOf('}');
        if (start >= 0 && end > start) return content.substring(start, end + 1);
        return null;
    }

    /** 将文本分割为流式块 */
    private String[] splitIntoChunks(String text, int chunkSize) {
        if (text == null || text.isEmpty()) return new String[0];
        int len = text.length();
        int chunks = (int) Math.ceil((double) len / chunkSize);
        String[] result = new String[chunks];
        for (int i = 0; i < chunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, len);
            result[i] = text.substring(start, end);
        }
        return result;
    }
}
