package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.entity.Dish;
import com.reggie.mapper.DishMapper;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.mapper.AIConversationMapper;
import com.reggie.module.ai.mapper.AIMessageRecordMapper;
import com.reggie.module.ai.model.*;
import com.reggie.module.ai.provider.AiProviderManager;
import com.reggie.module.ai.adapter.AiModelAdapter.StreamCallback;
import com.reggie.module.ai.service.AIChatService;
import com.reggie.module.ai.service.ConversationContextService;
import com.reggie.module.ai.service.AICacheService;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import com.reggie.module.ai.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
/**
 * AIChat service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@SuppressWarnings("unchecked")
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

    /** 对话上下文记忆服务 */
    @Resource
    private ConversationContextService conversationContextService;

    /** 菜品数据缓存服务 */
    @Resource
    private AICacheService aiCacheService;

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
        final Long userId = request.getUserId();
        final String conversationId = request.getConversationId();
        final String scene = request.getScene();
        final String userMessage = request.getMessage();
        long sseTimeout = resolveSseTimeout();
        SseEmitter emitter = new SseEmitter(sseTimeout);
        emitter.onTimeout(() -> log.warn("SSE连接超时: conversationId={}", conversationId));
        emitter.onError((e) -> log.warn("SSE连接错误: conversationId={}", conversationId, e));
        emitter.onCompletion(() -> log.debug("SSE连接完成: conversationId={}", conversationId));

        saveUserMessage(request);
        final AiProviderConfig providerConfig = aiProviderManager.getActiveConfig();
        final Long tenantId = BaseContext.getCurrentTenantId();

        CompletableFuture.runAsync(() -> {
            try {
                List<AIMessage> messages = buildMessages(request);
                int maxTokens = (providerConfig != null && providerConfig.getMaxTokens() != null)
                        ? providerConfig.getMaxTokens() : aiConfig.getMaxTokens();
                double temperature = (providerConfig != null && providerConfig.getTemperature() != null)
                        ? providerConfig.getTemperature() : aiConfig.getTemperature();

                final long[] firstTokenTime = new long[1];
                long streamStart = System.currentTimeMillis();

                // 使用真流式输出（适配器直接支持 SSE 或降级分块）
                StringBuilder fullContent = new StringBuilder();
                final Long[] savedAiMsgId = new Long[1];
                final List<AIRecommendedDish>[] parsedDishes = new List[]{null};

                StreamCallback callback = new StreamCallback() {
                    @Override
                    public void onToken(String token, boolean isLast) {
                        try {
                            if (isLast) {
                                // 最后一块：持久化并发送完成信号
                                String content = fullContent.toString();
                                if (content.isEmpty()) return;

                                // 解析推荐菜品（点餐场景）
                                List<AIRecommendedDish> dishes = null;
                                if ("order_assistant".equals(scene)) {
                                    dishes = parseRecommendedDishes(content, tenantId);
                                    parsedDishes[0] = dishes;
                                    content = cleanJsonFromContent(content);
                                }

                                savedAiMsgId[0] = saveAiMessage(conversationId, userId,
                                        content, null, dishes);

                                Map<String, Object> doneData = new HashMap<>();
                                doneData.put("status", "complete");
                                if (savedAiMsgId[0] != null) {
                                    doneData.put("messageId", savedAiMsgId[0]);
                                }
                                emitter.send(SseEmitter.event().name("done").data(doneData));

                                if ("order_assistant".equals(scene) && dishes != null && !dishes.isEmpty()) {
                                    try {
                                        String dishJson = OBJECT_MAPPER.writeValueAsString(dishes);
                                        emitter.send(SseEmitter.event().name("dishes").data(dishJson));
                                    } catch (Exception e) {
                                        log.warn("序列化推荐菜品失败", e);
                                    }
                                }

                                updateConversationTitle(conversationId, userMessage);
                                emitter.complete();
                            } else {
                                // 中间 token：累积内容并推送
                                fullContent.append(token);
                                long now = System.currentTimeMillis();
                                if (firstTokenTime[0] == 0) {
                                    firstTokenTime[0] = now - streamStart;
                                    log.debug("首字延迟: {}ms, conversationId={}", firstTokenTime[0], conversationId);
                                }
                                Map<String, Object> chunkData = new HashMap<>();
                                chunkData.put("text", token);
                                emitter.send(SseEmitter.event().name("message").data(chunkData));
                            }
                        } catch (Exception e) {
                            log.warn("SSE token推送失败: conversationId={}", conversationId, e);
                        }
                    }
                };

                // 调用流式接口
                aiProviderManager.streamChat(messages, maxTokens, temperature, callback);

                // 超时兜底：如果流式完成时间过长，记录延迟
                if (firstTokenTime[0] > 0) {
                    long totalTime = System.currentTimeMillis() - streamStart;
                    if (firstTokenTime[0] > 3000) {
                        log.warn("首字延迟过高: {}ms, totalTime={}ms, provider={}",
                                firstTokenTime[0], totalTime,
                                providerConfig != null ? providerConfig.getProviderCode() : "default");
                    }
                }
            } catch (Exception e) {
                log.error("SSE流式对话异常: conversationId={}", conversationId, e);
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
            List<AIRecommendedDish> dishes = parseRecommendedDishes(response.getContent(), BaseContext.getCurrentTenantId());
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
    public AIChatResponse orderAssistant(String userMessage, Long userId, String conversationId) {
        // 修改点：异步刷新用户画像（不阻塞对话）
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

        // 修改点：仅在未提供conversationId时新建对话，避免Controller层与Service层双重创建导致孤立对话
        if (conversationId == null || conversationId.isEmpty()) {
            AIConversation conv = createConversation(userId, null, "order_assistant");
            conversationId = conv.getConversationId();
        }

        Map<String, Object> context = buildOrderContext(userId);

        AIChatRequest request = AIChatRequest.builder()
                .message(userMessage)
                .scene("order_assistant")
                .conversationId(conversationId)
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
                .eq(AIConversation::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(AIConversation::getUpdateTime);
        Page<AIConversation> pageObj = new Page<>(page, pageSize);
        conversationMapper.selectPage(pageObj, wrapper);
        return pageObj.getRecords();
    }

    @Override
    public List<AIMessageRecord> getConversationMessages(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            return Collections.emptyList();
        }
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<AIConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.select(AIConversation::getUserId)
                .eq(AIConversation::getConversationId, conversationId)
                .eq(AIConversation::getIsDeleted, 0)
                .eq(AIConversation::getTenantId, BaseContext.getCurrentTenantId());
        AIConversation conv = conversationMapper.selectOne(convWrapper);
        if (conv == null || !currentUserId.equals(conv.getUserId())) {
            return Collections.emptyList();
        }
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
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String conversationId, Long userId) {
        LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIConversation::getConversationId, conversationId)
                .eq(AIConversation::getUserId, userId)
                .eq(AIConversation::getIsDeleted, 0)
                .eq(AIConversation::getTenantId, BaseContext.getCurrentTenantId());
        AIConversation conv = conversationMapper.selectOne(wrapper);
        if (conv != null) {
            conv.setIsDeleted(1);
            conversationMapper.updateById(conv);
            LambdaUpdateWrapper<AIMessageRecord> msgUpdateWrapper = new LambdaUpdateWrapper<>();
            msgUpdateWrapper.eq(AIMessageRecord::getConversationId, conversationId)
                    .eq(AIMessageRecord::getIsDeleted, 0)
                    .set(AIMessageRecord::getIsDeleted, 1);
            messageRecordMapper.update(null, msgUpdateWrapper);
            // 清除上下文缓存
            conversationContextService.clearContext(conversationId);
        }
    }

    @Override
    public void saveMessage(AIMessageRecord record) {
        if (record.getConversationId() == null) {
            return;
        }
        if (record.getCreateTime() == null) {
            record.setCreateTime(LocalDateTime.now());
        }
        messageRecordMapper.insert(record);
        updateMessageCount(record.getConversationId());
    }

    @Override
    public void recordFeedback(Long messageId, String feedbackType, Long userId) {
        if (messageId == null) return;
        // 修改点：按 id + tenantId 查询校验归属，防止跨租户篡改反馈
        LambdaQueryWrapper<AIMessageRecord> fbWrapper = new LambdaQueryWrapper<>();
        fbWrapper.eq(AIMessageRecord::getId, messageId)
                .eq(AIMessageRecord::getTenantId, BaseContext.getCurrentTenantId());
        AIMessageRecord record = messageRecordMapper.selectOne(fbWrapper);
        if (record != null && (record.getUserId() == null || record.getUserId().equals(userId))) {
            record.setFeedback(feedbackType);
            messageRecordMapper.updateById(record);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构建对话消息列表
     * <p>集成上下文记忆：优先使用滑动窗口缓存，缺失时回退到 DB 历史。
     */
    private List<AIMessage> buildMessages(AIChatRequest request) {
        List<AIMessage> messages = new ArrayList<>();

        // 1) System Prompt
        String systemPrompt = getSystemPrompt(request.getScene());
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(AIMessage.builder().role("system").content(systemPrompt).build());
        }

        // 2) 对话上下文记忆（滑动窗口 + 摘要）
        if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
            List<AIMessage> ctxMessages = conversationContextService.getContext(request.getConversationId());
            if (ctxMessages.isEmpty()) {
                // 缓存未命中，从 DB 加载并重建上下文
                try {
                    List<AIMessageRecord> dbHistory = getRecentMessages(request.getConversationId());
                    List<AIMessage> historyMessages = new ArrayList<>();
                    for (AIMessageRecord record : dbHistory) {
                        if (record.getContent() != null) {
                            historyMessages.add(AIMessage.builder()
                                    .role(record.getRole())
                                    .content(record.getContent())
                                    .build());
                        }
                    }
                    conversationContextService.rebuild(request.getConversationId(), historyMessages);
                    ctxMessages = conversationContextService.getContext(request.getConversationId());
                } catch (Exception e) {
                    log.warn("加载对话历史失败: conversationId={}", request.getConversationId(), e);
                }
            }
            messages.addAll(ctxMessages);
        }

        // 3) 用户长期记忆画像
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

        // 4) 上下文数据（菜品列表等）
        if (request.getContext() != null && !request.getContext().isEmpty()) {
            try {
                String contextJson = OBJECT_MAPPER.writeValueAsString(request.getContext());
                String contextPrompt = "以下是当前可用数据（仅使用真实存在的数据，不要编造）：\n" + contextJson;
                messages.add(AIMessage.builder().role("system").content(contextPrompt).build());
            } catch (Exception e) {
                log.warn("序列化上下文数据失败", e);
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

        // 使用缓存获取菜品列表（避免每次请求都查 DB）
        String dishList = aiCacheService.getFormattedDishList();
        context.put("dishes", dishList);
        return context;
    }

    private List<AIRecommendedDish> parseRecommendedDishes(String aiContent, Long tenantId) {
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
                        LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
                        dishWrapper.in(Dish::getId, dishIds);
                        if (tenantId != null) {
                            dishWrapper.eq(Dish::getTenantId, tenantId);
                        }
                        Map<Long, Dish> dishMap = dishMapper.selectList(dishWrapper).stream()
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
     * 从AI回复中清理JSON部分，只保留人类可读的文本内容。
     * <p>仅移除真正的代码块 JSON，尽量保留自然语言中的括号文本。
     */
    private String cleanJsonFromContent(String content) {
        if (content == null) return null;
        String cleaned = content;
        // 优先清理 markdown 代码块
        cleaned = cleaned.replaceAll("(?s)```json\\s*[\\s\\S]*?```", "");
        cleaned = cleaned.replaceAll("(?s)```\\s*[\\s\\S]*?```", "");
        // 只清理代码块后残留的空行/多余空白
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n").trim();
        return cleaned;
    }

    /**
     * 从AI回复中提取可用于解析的JSON子串，避免自然语言括号干扰。
     * <p>优先提取代码块内的 JSON；否则从首个 '{' / '[' 开始，匹配同类型闭合符。 */
    private String extractJson(String content) {
        if (content == null || content.isEmpty()) return null;

        // 优先提取代码块中的 JSON
        int codeBlock = content.indexOf("```json");
        if (codeBlock >= 0) {
            int start = content.indexOf('\n', codeBlock + "```json".length());
            if (start >= 0) {
                int end = content.indexOf("```", start + 1);
                if (end > start) {
                    String block = content.substring(start + 1, end).trim();
                    if (!block.isEmpty()) return block;
                }
            }
        }

        // 仅当字符串整体就是 JSON 时，直接返回
        String trimmed = content.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return trimmed;
        }

        // 从首个 '{' / '[' 开始，匹配同类型闭合符，避免跨括号类型误匹配
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (c == '{' || c == '[') {
                int end = findMatchingBracket(trimmed, i, c);
                if (end > i) {
                    return trimmed.substring(i, end + 1);
                }
            }
        }
        return null;
    }

    private static int findMatchingBracket(String s, int start, char open) {
        char close = open == '{' ? '}' : ']';
        int depth = 1;
        boolean inString = false;
        for (int i = start + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == open) {
                    depth++;
                } else if (c == close) {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
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

    /**
     * 判断 AI 响应内容是否为错误信息。
     * <p>当前采用启发式：若内容以常见错误前缀开头，则视为错误响应。</p>
     */
    private boolean isErrorResponse(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        return lower.startsWith("ai服务")
                || lower.startsWith("无法连接")
                || lower.startsWith("连接失败")
                || lower.startsWith("请求失败")
                || lower.startsWith("网关响应解析失败")
                || lower.startsWith("模型返回了空响应");
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
                .eq(AIMessageRecord::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(AIMessageRecord::getCreateTime);
        Page<AIMessageRecord> pageObj = new Page<>(1, MAX_HISTORY_MESSAGES);
        messageRecordMapper.selectPage(pageObj, wrapper);
        List<AIMessageRecord> records = pageObj.getRecords();
        // 反转列表，使消息按时间正序排列（最早的在前）
        Collections.reverse(records);
        return records;
    }

    /**
     * 保存用户消息到数据库（含去重检查，防止SSE重连/重复请求导致的消息双写）
     * <p>同时将消息注入上下文记忆服务。
     *
     * @return 保存后的消息ID
     */
    private Long saveUserMessage(AIChatRequest request) {
        if (request.getConversationId() == null || request.getConversationId().isEmpty()) {
            return null;
        }
        try {
            // 去重检查
            LambdaQueryWrapper<AIMessageRecord> dedupWrapper = new LambdaQueryWrapper<>();
            dedupWrapper.eq(AIMessageRecord::getConversationId, request.getConversationId())
                    .eq(AIMessageRecord::getRole, "user")
                    .eq(AIMessageRecord::getContent, request.getMessage())
                    .eq(AIMessageRecord::getIsDeleted, 0)
                    .gt(AIMessageRecord::getCreateTime, LocalDateTime.now().minusSeconds(5))
                    .orderByDesc(AIMessageRecord::getCreateTime);
            AIMessageRecord existingMsg = messageRecordMapper.selectOne(dedupWrapper);
            if (existingMsg != null) {
                log.debug("检测到重复用户消息，跳过保存: conversationId={}, contentLength={}",
                        request.getConversationId(), request.getMessage().length());
                return existingMsg.getId();
            }

            AIMessageRecord record = new AIMessageRecord();
            record.setConversationId(request.getConversationId());
            record.setUserId(request.getUserId());
            record.setRole("user");
            record.setContent(request.getMessage());
            record.setMessageType("text");
            record.setIsDeleted(0);
            record.setCreateTime(LocalDateTime.now());
            messageRecordMapper.insert(record);

            // 注入上下文记忆
            conversationContextService.addMessage(
                    request.getConversationId(), "user", request.getMessage());

            // 更新会话消息计数
            updateMessageCount(request.getConversationId());
            return record.getId();
        } catch (Exception e) {
            log.warn("保存用户消息失败: conversationId={}", request.getConversationId(), e);
            return null;
        }
    }

    /**
     * 保存AI回复消息到数据库（含去重检查）
     * <p>同时将消息注入上下文记忆服务。
     *
     * @return 保存后的消息ID
     */
    private Long saveAiMessage(String conversationId, Long userId, String content,
                                Integer tokensUsed, List<AIRecommendedDish> dishes) {
        if (conversationId == null || conversationId.isEmpty() || content == null) {
            return null;
        }
        try {
            // 去重检查
            LambdaQueryWrapper<AIMessageRecord> dedupWrapper = new LambdaQueryWrapper<>();
            dedupWrapper.eq(AIMessageRecord::getConversationId, conversationId)
                    .eq(AIMessageRecord::getRole, "assistant")
                    .eq(AIMessageRecord::getContent, content)
                    .eq(AIMessageRecord::getIsDeleted, 0)
                    .gt(AIMessageRecord::getCreateTime, LocalDateTime.now().minusSeconds(10))
                    .orderByDesc(AIMessageRecord::getCreateTime);
            AIMessageRecord existingMsg = messageRecordMapper.selectOne(dedupWrapper);
            if (existingMsg != null) {
                log.debug("检测到重复AI回复消息，跳过保存: conversationId={}", conversationId);
                return existingMsg.getId();
            }

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
                } catch (Exception e) {
                    log.warn("序列化推荐菜品ID失败: conversationId={}", conversationId, e);
                }
            }

            messageRecordMapper.insert(record);

            // 注入上下文记忆
            conversationContextService.addMessage(conversationId, "assistant", content);

            // 更新会话消息计数
            updateMessageCount(conversationId);
            return record.getId();
        } catch (Exception e) {
            log.warn("保存AI回复消息失败: conversationId={}", conversationId, e);
            return null;
        }
    }

    /**
     * 更新对话的消息计数和最后更新时间（原子递增，避免竞态条件）
     */
    private void updateMessageCount(String conversationId) {
        try {
            LambdaUpdateWrapper<AIConversation> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(AIConversation::getConversationId, conversationId)
                    .eq(AIConversation::getIsDeleted, 0)
                    .setSql("message_count = IFNULL(message_count, 0) + 1")
                    .set(AIConversation::getUpdateTime, LocalDateTime.now());
            conversationMapper.update(null, wrapper);
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

    @Override
    public Map<String, Object> getContextStats(String conversationId) {
        return conversationContextService.getStats(conversationId);
    }

    @Override
    public void resetContext(String conversationId) {
        conversationContextService.clearContext(conversationId);
    }
}

