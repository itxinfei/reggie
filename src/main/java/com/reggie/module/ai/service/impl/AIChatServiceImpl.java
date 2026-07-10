package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * AI聊天服务实现
 * 核心逻辑：构建Prompt → 调用AI Provider → 解析结果 → 持久化消息
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class AIChatServiceImpl extends ServiceImpl<AIConversationMapper, AIConversation> implements AIChatService {

    /** AI供应商管理器 */
    @Resource
    private AiProviderManager aiProviderManager;

    /** AI配置属性 */
    @Resource
    private AIConfigProperties aiConfig;

    /** 菜品Mapper */
    @Resource
    private DishMapper dishMapper;

    /** 用户偏好分析服务 */
    @Resource
    private PreferenceAnalysisService preferenceAnalysisService;

    /** 用户画像服务 */
    @Resource
    private UserProfileService userProfileService;

    /** AI对话Mapper */
    @Resource
    private AIConversationMapper conversationMapper;

    /** AI消息记录Mapper */
    @Resource
    private AIMessageRecordMapper messageRecordMapper;

    /** JSON序列化工具 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 单次对话携带的最大历史消息数 */
    private static final int MAX_HISTORY_MESSAGES = 20;

    /** AI异步任务线程池 */
    @Resource
    private Executor aiExecutor;

    // ==================== 流式对话 ====================

    /**
     * 解析SSE超时时间（毫秒）
     * 优先级：供应商配置 > YAML配置 > 默认60秒，额外加30秒缓冲
     */
    private long resolveSseTimeout() {
        AiProviderConfig providerConfig = aiProviderManager.getActiveConfig();
        int timeout;
        if (providerConfig != null && providerConfig.getTimeout() != null) {
            timeout = providerConfig.getTimeout();
        } else {
            timeout = aiConfig.getTimeout();
        }
        return (long) timeout * 1000L + 30000L;
    }

    @Override
    public SseEmitter chatStream(AIChatRequest request) {
        long sseTimeout = resolveSseTimeout();
        SseEmitter emitter = new SseEmitter(sseTimeout);
        emitter.onTimeout(() -> log.warn("SSE连接超时: conversationId={}", request.getConversationId()));
        emitter.onError((e) -> log.warn("SSE连接错误: conversationId={}", request.getConversationId(), e));
        emitter.onCompletion(() -> log.debug("SSE连接完成: conversationId={}", request.getConversationId()));

        saveUserMessage(request);
        final Long userId = request.getUserId();
        final AiProviderConfig providerConfig = aiProviderManager.getActiveConfig();

        CompletableFuture.runAsync(() -> {
            try {
                List<AIMessage> messages = buildMessages(request);
                int maxTokens = (providerConfig != null && providerConfig.getMaxTokens() != null)
                        ? providerConfig.getMaxTokens() : aiConfig.getMaxTokens();
                double temperature = (providerConfig != null && providerConfig.getTemperature() != null)
                        ? providerConfig.getTemperature() : aiConfig.getTemperature();
                AIChatResponse response = aiProviderManager.chat(messages, maxTokens, temperature);

                Long savedAiMsgId = null;
                if (response != null && response.getContent() != null) {
                    // 点餐场景：先解析推荐菜品，再清理content中的JSON
                    if ("order_assistant".equals(request.getScene())) {
                        response.setDishes(parseRecommendedDishes(response.getContent()));
                        response.setContent(cleanJsonFromContent(response.getContent()));
                    }
                    savedAiMsgId = saveAiMessage(request.getConversationId(), userId,
                            response.getContent(), response.getTokensUsed(),
                            "order_assistant".equals(request.getScene()) ? response.getDishes() : null);

                    // 流式发送内容（按字符块模拟流式效果）
                    String content = response.getContent();
                    String[] chunks = splitIntoChunks(content, 20);
                    for (int i = 0; i < chunks.length; i++) {
                        Map<String, Object> chunkData = new HashMap<>();
                        chunkData.put("text", chunks[i]);
                        if (i == 0 && savedAiMsgId != null) {
                            chunkData.put("messageId", savedAiMsgId);
                        }
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(chunkData));
                        Thread.sleep(30);
                    }
                }

                // 发送完成事件（携带 messageId）
                Map<String, Object> doneData = new HashMap<>();
                doneData.put("status", "complete");
                if (savedAiMsgId != null) {
                    doneData.put("messageId", savedAiMsgId);
                }
                emitter.send(SseEmitter.event().name("done").data(doneData));

                // 点餐场景：发送推荐菜品（已在前面解析并设置到response中）
                if ("order_assistant".equals(request.getScene()) && response != null && response.getDishes() != null && !response.getDishes().isEmpty()) {
                    String dishJson = OBJECT_MAPPER.writeValueAsString(response.getDishes());
                    emitter.send(SseEmitter.event().name("dishes").data(dishJson));
                }

                updateConversationTitle(request.getConversationId(), request.getMessage());

                emitter.complete();
            } catch (Exception e) {
                log.error("SSE流式对话异常: conversationId={}", request.getConversationId(), e);
                try {
                    Map<String, Object> errorData = new HashMap<>();
                    errorData.put("message", "服务暂时不可用，请稍后重试");
                    emitter.send(SseEmitter.event().name("error").data(errorData));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }, aiExecutor);

        return emitter;
    }

    @Override
    public SseEmitter orderAssistantStream(String userMessage, Long userId, String conversationId) {
        Map<String, Object> context = buildOrderContext(userId);

        // 创建或获取对话
        if (conversationId == null || conversationId.isEmpty()) {
            AIConversation conv = createConversation(userId, null, "order_assistant");
            conversationId = conv.getConversationId();
        }

        AIChatRequest request = AIChatRequest.builder()
                .message(userMessage)
                .scene("order_assistant")
                .conversationId(conversationId)
                .userId(userId)
                .context(context)
                .build();

        return chatStream(request);
    }

    // ==================== 非流式对话 ====================

    @Override
    public AIChatResponse chat(AIChatRequest request) {
        saveUserMessage(request);

        // 异步刷新用户画像（不阻塞对话）
        if (request.getUserId() != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    // 修改点：使用节流刷新替代每次全量刷新
                    userProfileService.refreshIfNeeded(request.getUserId());
                } catch (Exception e) {
                    log.warn("异步刷新用户画像失败: userId={}", request.getUserId(), e);
                }
            }, aiExecutor);
        }

        List<AIMessage> messages = buildMessages(request);
        // 优先使用供应商配置中的参数，回退到 application.yml
        AiProviderConfig providerConfig = aiProviderManager.getActiveConfig();
        int maxTokens = (providerConfig != null && providerConfig.getMaxTokens() != null)
                ? providerConfig.getMaxTokens() : aiConfig.getMaxTokens();
        double temperature = (providerConfig != null && providerConfig.getTemperature() != null)
                ? providerConfig.getTemperature() : aiConfig.getTemperature();
        AIChatResponse response = aiProviderManager.chat(messages, maxTokens, temperature);

        if ("order_assistant".equals(request.getScene()) && response != null && response.getContent() != null) {
            List<AIRecommendedDish> dishes = parseRecommendedDishes(response.getContent());
            response.setDishes(dishes);
            // 清理content中的JSON部分，只保留人类可读的文本
            response.setContent(cleanJsonFromContent(response.getContent()));
        }

        AIChatResponse result = response != null ? response : AIChatResponse.builder()
                .content("AI服务暂时不可用，请稍后重试。")
                .model(aiConfig.getModel())
                .build();

        Long savedId = saveAiMessage(request.getConversationId(), request.getUserId(),
                result.getContent(), result.getTokensUsed(),
                "order_assistant".equals(request.getScene()) ? result.getDishes() : null);

        if (savedId != null && result.getData() == null) {
            result.setData(new HashMap<>());
        }
        if (savedId != null) {
            result.getData().put("messageId", savedId);
        }

        return result;
    }

    @Override
    public AIChatResponse orderAssistant(String userMessage, Long userId) {
        // 异步刷新用户画像（不阻塞对话）
        if (userId != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    // 修改点：使用节流刷新替代每次全量刷新
                    userProfileService.refreshIfNeeded(userId);
                } catch (Exception e) {
                    log.warn("异步刷新用户画像失败: userId={}", userId, e);
                }
            }, aiExecutor);
        }

        Map<String, Object> context = buildOrderContext(userId);

        AIConversation conv = createConversation(userId, null, "order_assistant");
        AIChatRequest request = AIChatRequest.builder()
                .message(userMessage)
                .scene("order_assistant")
                .conversationId(conv.getConversationId())
                .userId(userId)
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
                .orderByDesc(AIConversation::getUpdateTime);
        Page<AIConversation> pageObj = new Page<>(page, pageSize);
        conversationMapper.selectPage(pageObj, wrapper);
        return pageObj.getRecords();
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
        conv.setCreateTime(LocalDateTime.now());
        conv.setUpdateTime(LocalDateTime.now());
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
            // 修改点：批量更新消息记录，消除N+1问题
            LambdaUpdateWrapper<AIMessageRecord> msgUpdateWrapper = new LambdaUpdateWrapper<>();
            msgUpdateWrapper.eq(AIMessageRecord::getConversationId, conversationId)
                    .eq(AIMessageRecord::getIsDeleted, 0)
                    .set(AIMessageRecord::getIsDeleted, 1);
            messageRecordMapper.update(null, msgUpdateWrapper);
        }
    }

    @Override
    public void saveMessage(AIMessageRecord record) {
        if (record.getConversationId() != null) {
            if (record.getCreateTime() == null) {
                record.setCreateTime(LocalDateTime.now());
            }
            messageRecordMapper.insert(record);
            // 更新会话消息计数
            LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AIConversation::getConversationId, record.getConversationId());
            AIConversation conv = conversationMapper.selectOne(wrapper);
            if (conv != null) {
                conv.setMessageCount((conv.getMessageCount() != null ? conv.getMessageCount() : 0) + 1);
                conv.setUpdateTime(LocalDateTime.now());
                conversationMapper.updateById(conv);
            }
        }
    }

    @Override
    public void recordFeedback(Long messageId, String feedbackType, Long userId) {
        if (messageId == null) return;
        AIMessageRecord record = messageRecordMapper.selectById(messageId);
        if (record != null && (record.getUserId() == null || record.getUserId().equals(userId))) {
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

        // 1) System Prompt
        String systemPrompt = getSystemPrompt(request.getScene());
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(AIMessage.builder().role("system").content(systemPrompt).build());
        }

        // 2) 用户长期记忆画像
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

        // 3) 上下文数据（菜品列表等）
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            try {
                String contextJson = OBJECT_MAPPER.writeValueAsString(request.getContext());
                String contextPrompt = "以下是当前可用数据（仅使用真实存在的数据，不要编造）：\n" + contextJson;
                messages.add(AIMessage.builder().role("system").content(contextPrompt).build());
            } catch (Exception e) {
                log.warn("序列化上下文数据失败", e);
            }
        }

        // 4) 历史消息
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            // 请求带了历史消息（前端主动维护的）
            messages.addAll(request.getHistory());
        } else if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
            try {
                List<AIMessageRecord> dbHistory = getRecentMessages(request.getConversationId());
                for (AIMessageRecord record : dbHistory) {
                    if (record.getContent() != null) {
                        messages.add(AIMessage.builder()
                                .role(record.getRole())
                                .content(record.getContent())
                                .build());
                    }
                }
                if (!dbHistory.isEmpty()) {
                    log.debug("已从DB加载{}条历史消息: conversationId={}",
                            dbHistory.size(), request.getConversationId());
                }
            } catch (Exception e) {
                log.warn("加载对话历史失败: conversationId={}", request.getConversationId(), e);
            }
        }

        // 5) 当前用户消息
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

    /**
     * 构建智能点餐场景的上下文数据（用户偏好 + 可用菜品）
     * 修改点：提取公共方法，消除 orderAssistant() 和 orderAssistantStream() 的重复代码
     */
    private Map<String, Object> buildOrderContext(Long userId) {
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
        return context;
    }

    private List<AIRecommendedDish> parseRecommendedDishes(String aiContent) {
        List<AIRecommendedDish> result = new ArrayList<>();
        try {
            String jsonStr = extractJson(aiContent);
            if (jsonStr != null) {
                JsonNode root = OBJECT_MAPPER.readTree(jsonStr);
                if (root.isArray()) {
                    // 修改点：先收集所有dishId，批量查询，消除N+1问题
                    List<Long> dishIds = new ArrayList<>();
                    Map<Long, String> dishReasonMap = new HashMap<>();
                    for (JsonNode node : root) {
                        Long dishId = node.has("dishId") ? node.get("dishId").asLong() : null;
                        String reason = node.has("reason") ? node.get("reason").asText() : "";
                        if (dishId != null) {
                            dishIds.add(dishId);
                            dishReasonMap.put(dishId, reason);
                        }
                    }
                    if (!dishIds.isEmpty()) {
                        Map<Long, Dish> dishMap = dishMapper.selectBatchIds(dishIds).stream()
                                .collect(Collectors.toMap(Dish::getId, Function.identity()));
                        for (Long dishId : dishIds) {
                            Dish dish = dishMap.get(dishId);
                            if (dish != null) {
                                result.add(AIRecommendedDish.builder()
                                        .dishId(dish.getId())
                                        .name(dish.getName())
                                        .price(dish.getPrice())
                                        .image(dish.getImage())
                                        .reason(dishReasonMap.getOrDefault(dishId, ""))
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
     * 从AI回复中清理JSON部分，只保留人类可读的文本内容
     */
    private String cleanJsonFromContent(String content) {
        if (content == null) return null;
        String cleaned = content;
        // 清理JSON数组
        int start = cleaned.indexOf('[');
        int end = cleaned.lastIndexOf(']');
        if (start >= 0 && end > start) {
            String before = cleaned.substring(0, start).trim();
            String after = cleaned.substring(end + 1).trim();
            cleaned = (before + " " + after).trim();
        }
        // 清理JSON对象
        start = cleaned.indexOf('{');
        end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            String before = cleaned.substring(0, start).trim();
            String after = cleaned.substring(end + 1).trim();
            cleaned = (before + " " + after).trim();
        }
        // 清理可能残留的代码块标记
        cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
        return cleaned.trim();
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

    // ==================== 消息持久化方法 ====================

    /**
     * 从数据库加载会话最近的历史消息（用于多轮对话上下文）
     */
    private List<AIMessageRecord> getRecentMessages(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<AIMessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIMessageRecord::getConversationId, conversationId)
                .eq(AIMessageRecord::getIsDeleted, 0)
                .orderByDesc(AIMessageRecord::getCreateTime);
        Page<AIMessageRecord> pageObj = new Page<>(1, MAX_HISTORY_MESSAGES);
        messageRecordMapper.selectPage(pageObj, wrapper);
        List<AIMessageRecord> records = pageObj.getRecords();
        // 反转列表，使消息按时间正序排列（最早的在前）
        Collections.reverse(records);
        return records;
    }

    /**
     * 保存用户消息到数据库
     *
     * @return 保存后的消息ID
     */
    private Long saveUserMessage(AIChatRequest request) {
        if (request.getConversationId() == null || request.getConversationId().isEmpty()) {
            return null;
        }
        try {
            AIMessageRecord record = new AIMessageRecord();
            record.setConversationId(request.getConversationId());
            record.setUserId(request.getUserId());
            record.setRole("user");
            record.setContent(request.getMessage());
            record.setMessageType("text");
            record.setIsDeleted(0);
            record.setCreateTime(LocalDateTime.now());
            messageRecordMapper.insert(record);

            // 更新会话消息计数
            updateMessageCount(request.getConversationId());
            return record.getId();
        } catch (Exception e) {
            log.warn("保存用户消息失败: conversationId={}", request.getConversationId(), e);
            return null;
        }
    }

    /**
     * 保存AI回复消息到数据库
     *
     * @return 保存后的消息ID
     */
    private Long saveAiMessage(String conversationId, Long userId, String content,
                                Integer tokensUsed, List<AIRecommendedDish> dishes) {
        if (conversationId == null || conversationId.isEmpty()) {
            return null;
        }
        try {
            AIMessageRecord record = new AIMessageRecord();
            record.setConversationId(conversationId);
            record.setUserId(userId);
            record.setRole("assistant");
            record.setContent(content);
            record.setMessageType("text");
            record.setTokensUsed(tokensUsed != null ? tokensUsed : 0);
            record.setIsDeleted(0);
            record.setCreateTime(LocalDateTime.now());

            // 如果有推荐菜品，保存菜品ID列表
            if (dishes != null && !dishes.isEmpty()) {
                try {
                    List<Long> dishIdList = new ArrayList<>();
                    for (AIRecommendedDish dish : dishes) {
                        dishIdList.add(dish.getDishId());
                    }
                    record.setDishIds(OBJECT_MAPPER.writeValueAsString(dishIdList));
                } catch (Exception ignored) {
                }
            }

            messageRecordMapper.insert(record);

            // 更新会话消息计数
            updateMessageCount(conversationId);
            return record.getId();
        } catch (Exception e) {
            log.warn("保存AI回复消息失败: conversationId={}", conversationId, e);
            return null;
        }
    }

    /**
     * 更新对话的消息计数和最后更新时间
     */
    private void updateMessageCount(String conversationId) {
        try {
            LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AIConversation::getConversationId, conversationId)
                    .eq(AIConversation::getIsDeleted, 0);
            AIConversation conv = conversationMapper.selectOne(wrapper);
            if (conv != null) {
                conv.setMessageCount((conv.getMessageCount() != null ? conv.getMessageCount() : 0) + 1);
                conv.setUpdateTime(LocalDateTime.now());
                conversationMapper.updateById(conv);
            }
        } catch (Exception e) {
            log.warn("更新消息计数失败: conversationId={}", conversationId, e);
        }
    }

    /**
     * 更新对话标题（取用户首条消息的前20字符作为标题）
     */
    private void updateConversationTitle(String conversationId, String firstMessage) {
        if (conversationId == null || firstMessage == null) {
            return;
        }
        try {
            LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AIConversation::getConversationId, conversationId)
                    .eq(AIConversation::getIsDeleted, 0);
            AIConversation conv = conversationMapper.selectOne(wrapper);
            if (conv != null && ("新对话".equals(conv.getTitle()) || conv.getTitle() == null)) {
                String title = firstMessage.length() > 20
                        ? firstMessage.substring(0, 20) + "..."
                        : firstMessage;
                conv.setTitle(title);
                conversationMapper.updateById(conv);
            }
        } catch (Exception e) {
            log.warn("更新对话标题失败: conversationId={}", conversationId, e);
        }
    }
}
