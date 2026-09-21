package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reggie.common.BaseContext;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.dish.mapper.DishMapper;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.mapper.AIConversationMapper;
import com.reggie.module.ai.mapper.AIMessageRecordMapper;
import com.reggie.module.ai.dto.AiAttachmentVO;
import com.reggie.module.ai.model.AiChatConstants;
import com.reggie.module.ai.model.AIChatRequest;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIConversation;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AIMessageRecord;
import com.reggie.module.ai.model.AIRecommendedDish;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.model.ModelTurn;
import com.reggie.module.ai.provider.AiProviderManager;
import com.reggie.module.ai.adapter.AbortableStreamCallback;
import com.reggie.module.ai.adapter.AiModelAdapter;
import com.reggie.module.ai.tool.AiToolOrchestrator;
import com.reggie.module.ai.tool.BusinessSnapshotService;
import com.reggie.module.ai.rag.service.KnowledgeRetrievalService;
import com.reggie.module.ai.tool.ToolEvent;
import com.reggie.module.ai.tool.ToolEventSink;
import com.reggie.module.ai.service.AIChatService;
import com.reggie.module.ai.service.AiAttachmentService;
import com.reggie.module.ai.service.AiPromptTemplateService;
import com.reggie.module.ai.service.ConversationContextService;
import com.reggie.module.ai.service.AICacheService;
import com.reggie.module.ai.service.conversation.AIConversationManagementService;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import com.reggie.module.ai.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
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

    /** 对话上下文记忆服务 */
    @Resource
    private ConversationContextService conversationContextService;

    /** 菜品数据缓存服务 */
    @Resource
    private AICacheService aiCacheService;

    /** P3：系统提示词改由提示词模板库下发（缺失/异常时降级 yml 默认常量） */
    @Resource
    private AiPromptTemplateService promptTemplateService;

    /** JSON序列化工具 */
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getDefault();

    /** AI异步任务线程池 */
    @Resource
    private Executor aiExecutor;

    /** 对话管理服务（对话 CRUD / 反馈 / 权限校验） */
    @Resource
    private AIConversationManagementService conversationManagementService;

    /** 图片附件服务（P2：归属校验、元数据落库、模型 base64 装配） */
    @Resource
    private AiAttachmentService aiAttachmentService;

    /** P4：function calling 多轮工具编排（经营分析场景查真实报表） */
    @Resource
    private AiToolOrchestrator aiToolOrchestrator;

    /** P4：不支持工具调用的供应商走经营快照注入降级 */
    @Resource
    private BusinessSnapshotService businessSnapshotService;

    /** P5：知识库 RAG 片段检索注入（失败静默降级） */
    @Resource
    private KnowledgeRetrievalService knowledgeRetrievalService;

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

    /**
     * 处理 chat stream。
     * <p>P1 改造：SSE 会话状态由 {@link ChatStreamSession} 持有，
     * 客户端断开（fetch abort）/超时经 emitter 回调触发 abort，断开上游连接并保留已生成片段。</p>
     * @param request 参数 request
     * @return 返回结果
     */
    @Override
    public SseEmitter chatStream(AIChatRequest request) {
        final String conversationId = request.getConversationId();
        long sseTimeout = resolveSseTimeout();
        SseEmitter emitter = new SseEmitter(sseTimeout);

        final AiProviderConfig providerConfig = aiProviderManager.getActiveConfig();
        final Long tenantId = BaseContext.getCurrentTenantId();
        final ChatStreamSession session = new ChatStreamSession(emitter, request, providerConfig, tenantId);

        emitter.onTimeout(() -> {
            log.warn("SSE连接超时，中止上游: conversationId={}", conversationId);
            session.abort();
        });
        emitter.onError((e) -> {
            // 客户端 AbortController/关闭页面会走这里：中止上游，避免继续消耗 token
            log.debug("SSE连接错误（客户端可能已断开），中止上游: conversationId={}", conversationId);
            session.abort();
        });
        emitter.onCompletion(() -> log.debug("SSE连接完成: conversationId={}", conversationId));

        CompletableFuture.runAsync(session, aiExecutor);

        return emitter;
    }

    /**
     * 流式无内容时推送错误事件并完成连接（等价抽取）。
     * <p>供应商返回的错误提示（如 Key 无效、请求失败）走 isLast token 但未累积内容，
     * 必须显式发送 error 事件并 complete，否则 SSE 挂起至超时。</p>
     */
    private void completeWithError(SseEmitter emitter, String errorMessage, String conversationId) {
        try {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("message", (errorMessage == null || errorMessage.isEmpty())
                    ? "AI服务返回了空响应" : errorMessage);
            emitter.send(SseEmitter.event().name("error").data(errorData));
            emitter.complete();
            log.warn("SSE流式无内容完成（错误提示已推送）: conversationId={}, message={}",
                    conversationId, errorMessage);
        } catch (Exception e) {
            // 宽异常兜底：连接可能已被客户端断开
            log.warn("SSE错误事件推送失败: conversationId={}", conversationId, e);
        }
    }

    /**
     * 发送推荐菜品事件（等价抽取，失败静默降级）。
     */
    private void sendDishesEvent(SseEmitter emitter, List<AIRecommendedDish> dishes) {
        try {
            String dishJson = OBJECT_MAPPER.writeValueAsString(dishes);
            emitter.send(SseEmitter.event().name("dishes").data(dishJson));
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.warn("序列化推荐菜品失败", e);
        }
    }

    /**
     * 处理流式异常并推送错误事件（等价抽取）。
     */
    private void handleStreamError(SseEmitter emitter, String conversationId, Exception e) {
        // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
        log.error("SSE流式对话异常: conversationId={}", conversationId, e);
        try {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("message", "服务暂时不可用，请稍后重试");
            emitter.send(SseEmitter.event().name("error").data(errorData));
            emitter.complete();
        } catch (Exception ex) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            emitter.completeWithError(ex);
        }
    }

    // ==================== 非流式对话 ====================

    /**
     * 处理 chat。
     * @param request 参数 request
     * @return 返回结果
     */
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
                    // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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
            List<AIRecommendedDish> dishes = parseRecommendedDishes(response.getContent(), BaseContext
                    .getCurrentTenantId());
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

    /**
     * 处理 order assistant。
     * @param userMessage 参数 userMessage
     * @param userId 参数 userId
     * @param conversationId 参数 conversationId
     * @return 返回结果
     */
    @Override
    public AIChatResponse orderAssistant(String userMessage, Long userId, String conversationId) {
        return orderAssistant(userMessage, userId, conversationId, null, null, null);
    }

    @Override
    public AIChatResponse orderAssistant(String userMessage, Long userId, String conversationId,
                                         List<String> attachmentIds, String actorType, Long tenantId) {
        // 修改点：异步刷新用户画像（不阻塞对话）
        if (userId != null) {
            CompletableFuture.runAsync(() -> {
                try {
                    // 修改点：使用节流刷新替代每次全量刷新
                    userProfileService.refreshIfNeeded(userId);
                } catch (Exception e) {
                    // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                    log.warn("异步刷新用户画像失败: userId={}", userId, e);
                }
            }, aiExecutor);
        }

        // 修改点：仅在未提供conversationId时新建对话，避免Controller层与Service层双重创建导致孤立对话
        if (conversationId == null || conversationId.isEmpty()) {
            AIConversation conv = createConversation(userId, actorType, "order_assistant");
            conversationId = conv.getConversationId();
        }

        Map<String, Object> context = buildOrderContext(userId);

        AIChatRequest request = AIChatRequest.builder()
                .message(userMessage)
                .scene("order_assistant")
                .conversationId(conversationId)
                .userId(userId)
                .actorType(actorType)
                .tenantId(tenantId)
                .attachments(attachmentIds)
                .context(context)
                .build();

        return chat(request);
    }

    /**
     * 生成 dish description。
     * @param dishName 参数 dishName
     * @param categoryName 参数 categoryName
     * @param ingredients 参数 ingredients
     * @return 返回结果
     */
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
                AIMessage.builder().role("system").content(getSystemPrompt("dish_desc")).build(),
                AIMessage.builder().role("user").content(prompt).build()
        );

        AIChatResponse response = aiProviderManager.chat(messages, 500, 0.8);
        return response != null ? response.getContent() : null;
    }

    /**
     * 处理 analyze business。
     * @param question 参数 question
     * @param dataJson 参数 dataJson
     * @return 返回结果
     */
    @Override
    public String analyzeBusiness(String question, String dataJson) {
        String prompt = "以下是门店经营数据（JSON格式）：\n" + dataJson + "\n\n"
                + "用户问题：" + question + "\n"
                + "请基于数据进行分析回答。";

        List<AIMessage> messages = Arrays.asList(
                AIMessage.builder().role("system").content(getSystemPrompt("business_analysis")).build(),
                AIMessage.builder().role("user").content(prompt).build()
        );

        AIChatResponse response = aiProviderManager.chat(messages, 1500, 0.5);
        return response != null ? response.getContent() : null;
    }

    // ==================== 对话管理（委托给 conversationManagementService） ====================

    /**
     * 获取 user conversations。
     * @param userId 参数 userId
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @return 返回结果
     */
    @Override
    public List<AIConversation> getUserConversations(Long userId, int page, int pageSize) {
        return conversationManagementService.getUserConversations(userId, page, pageSize);
    }

    /**
     * 获取 conversation messages。
     * @param conversationId 参数 conversationId
     * @return 返回结果
     */
    @Override
    public List<AIMessageRecord> getConversationMessages(String conversationId) {
        return conversationManagementService.getConversationMessages(conversationId);
    }

    /**
     * 创建 conversation。
     * @param userId 参数 userId
     * @param title 参数 title
     * @param scene 参数 scene
     * @return 返回结果
     */
    @Override
    public AIConversation createConversation(Long userId, String title, String scene) {
        return conversationManagementService.createConversation(userId, title, scene);
    }

    /**
     * 删除 conversation。
     * @param conversationId 参数 conversationId
     * @param userId 参数 userId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String conversationId, Long userId) {
        conversationManagementService.deleteConversation(conversationId, userId);
    }

    /**
     * 处理 record feedback。
     * @param messageId 参数 messageId
     * @param feedbackType 参数 feedbackType
     * @param userId 参数 userId
     */
    @Override
    public void recordFeedback(Long messageId, String feedbackType, Long userId) {
        conversationManagementService.recordFeedback(messageId, feedbackType, userId);
    }

    // ==================== P1：身份双校验版本委托 ====================

    /**
     * 处理 create conversation with actor。
     * @param userId 用户ID
     * @param actorType 身份类型 EMPLOYEE/CUSTOMER
     * @param title 标题
     * @param scene 场景
     * @return 新会话
     */
    @Override
    public AIConversation createConversation(Long userId, String actorType, String title, String scene) {
        return conversationManagementService.createConversation(userId, actorType, title, scene);
    }

    /**
     * 处理 get user conversations with actor。
     * @param userId 用户ID
     * @param actorType 身份类型
     * @param page 页码
     * @param pageSize 每页条数
     * @return 会话列表
     */
    @Override
    public List<AIConversation> getUserConversations(Long userId, String actorType, int page, int pageSize) {
        return conversationManagementService.getUserConversations(userId, actorType, page, pageSize);
    }

    /**
     * 处理 get conversation messages with actor。
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param actorType 身份类型
     * @return 消息列表
     */
    @Override
    public List<AIMessageRecord> getConversationMessages(String conversationId, Long userId, String actorType) {
        return conversationManagementService.getConversationMessages(conversationId, userId, actorType);
    }

    /**
     * 处理 delete conversation with actor。
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param actorType 身份类型
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConversation(String conversationId, Long userId, String actorType) {
        conversationManagementService.deleteConversation(conversationId, userId, actorType);
    }

    /**
     * 处理 rename conversation。
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param actorType 身份类型
     * @param title 新标题
     * @return 是否成功
     */
    @Override
    public boolean renameConversation(String conversationId, Long userId, String actorType, String title) {
        return conversationManagementService.renameConversation(conversationId, userId, actorType, title);
    }

    /**
     * 处理 delete message。
     * @param conversationId 会话ID
     * @param messageId 消息ID
     * @param userId 用户ID
     * @param actorType 身份类型
     * @return 是否成功
     */
    @Override
    public boolean deleteMessage(String conversationId, Long messageId, Long userId, String actorType) {
        return conversationManagementService.deleteMessage(conversationId, messageId, userId, actorType);
    }

    /**
     * 处理 validate ownership with actor。
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param actorType 身份类型
     * @return 归属用户ID，不匹配返回 null
     */
    @Override
    public Long validateConversationOwnership(String conversationId, Long userId, String actorType) {
        return conversationManagementService.validateConversationOwnership(conversationId, userId, actorType);
    }

    /**
     * 处理 search conversations with actor。
     * @param userId 用户ID
     * @param actorType 身份类型
     * @param keyword 关键词
     * @param page 页码
     * @param pageSize 每页条数
     * @return 会话列表
     */
    @Override
    public List<AIConversation> searchConversations(Long userId, String actorType, String keyword, int page,
                                                    int pageSize) {
        return conversationManagementService.searchConversations(userId, actorType, keyword, page, pageSize);
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
        List<AIMessage> ctxMessages = Collections.emptyList();
        if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
            ctxMessages = conversationContextService.getContext(request.getConversationId());
            if (ctxMessages.isEmpty()) {
                // 缓存未命中，从 DB 加载并重建上下文
                try {
                    List<AIMessageRecord> dbHistory = conversationManagementService.getConversationMessages(request
                            .getConversationId());
                    List<AIMessage> historyMessages = new ArrayList<>();
                    dbHistory.forEach(record -> {
                        // P2：历史 user 消息解析 attachments JSON 还原附件ID（运行时再校验归属读图）
                        List<Long> imgIds = "user".equals(record.getRole())
                                ? extractAttachmentIds(record.getAttachments()) : null;
                        boolean hasImages = imgIds != null && !imgIds.isEmpty();
                        if (record.getContent() != null || hasImages) {
                            historyMessages.add(AIMessage.builder()
                                    .role(record.getRole())
                                    .content(record.getContent())
                                    .attachmentIds(hasImages ? imgIds : null)
                                    .build());
                        }
                    });
                    conversationContextService.rebuild(request.getConversationId(), historyMessages);
                    ctxMessages = conversationContextService.getContext(request.getConversationId());
                } catch (Exception e) {
                    // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.warn("序列化上下文数据失败", e);
            }
        }

        // 4.5) P4 经营分析场景的数据供给（二选一）：
        //      支持 function calling → 注入工具使用准则（真实数字必须现查）；
        //      不支持（Baidu 压平协议/未勾选能力）→ 注入近 7 天经营快照兜底，二者都避免模型编造数字
        if ("business_analysis".equals(request.getScene())) {
            if (aiProviderManager.supportsToolCalling()) {
                messages.add(AIMessage.builder()
                        .role("system")
                        .content(buildToolGuidePrompt())
                        .build());
            } else {
                try {
                    String snapshotPrompt = businessSnapshotService
                            .buildSnapshotSystemPrompt(BaseContext.getCurrentTenantId());
                    if (snapshotPrompt != null && !snapshotPrompt.isEmpty()) {
                        messages.add(AIMessage.builder().role("system").content(snapshotPrompt).build());
                    }
                } catch (Exception e) {
                    // 宽异常兜底：快照失败退化为无快照对话，不阻断聊天
                    log.warn("注入经营快照失败，降级为无快照对话: {}", e.getMessage());
                }
            }
        }

        // 4.6) P5 知识库 RAG：按受众注入相关知识片段——
        //      后台（EMPLOYEE）全场景可见 MERCHANT/BOTH 文档；C 端仅点餐场景见 CUSTOMER/BOTH；
        //      检索/注入失败静默降级为普通对话，不阻断聊天
        boolean customerActor = "CUSTOMER".equals(request.getActorType());
        boolean ragSceneEnabled = !customerActor || "order_assistant".equals(request.getScene());
        if (ragSceneEnabled && request.getMessage() != null
                && !request.getMessage().trim().isEmpty()) {
            try {
                String ragAudience = customerActor ? "CUSTOMER" : "MERCHANT";
                String ragPrompt = knowledgeRetrievalService
                        .buildKnowledgePrompt(request.getMessage(), ragAudience);
                if (ragPrompt != null && !ragPrompt.isEmpty()) {
                    messages.add(AIMessage.builder().role("system").content(ragPrompt).build());
                }
            } catch (Exception e) {
                log.warn("注入知识库片段失败，降级为普通对话: {}", e.getMessage());
            }
        }

        // 5) 当前用户消息
        // 修改点(2026-09-20)：saveUserMessage 已把本轮消息写入上下文缓存（DB 重建路径同样包含），
        // 若末条已是相同的用户消息则不再追加，修复每轮问题被重复发送两次给模型的缺陷；
        // 重新生成场景（末轮 user 来自 DB 重建）也由此天然去重。
        // P2：去重同时比较附件ID集合（同文案带不同图算两条消息）；重生成不带附件、
        // 末条即被重放的原问题，仅比文本即可，避免把历史图片误判为差异而重复追加。
        List<Long> currentImageIds = parseAttachmentIds(request.getAttachments());
        boolean isRegenerate = Boolean.TRUE.equals(request.getRegenerate());
        AIMessage lastCtx = ctxMessages.isEmpty() ? null : ctxMessages.get(ctxMessages.size() - 1);
        boolean ctxHasCurrent = lastCtx != null && "user".equals(lastCtx.getRole())
                && Objects.equals(request.getMessage(), lastCtx.getContent())
                && (isRegenerate || sameImageIds(currentImageIds, lastCtx.getAttachmentIds()));
        if (!ctxHasCurrent) {
            messages.add(AIMessage.builder().role("user").content(request.getMessage())
                    .attachmentIds(currentImageIds.isEmpty() ? null : currentImageIds)
                    .build());
        }

        // 6) P2：历史 + 本轮图片统一做 owner 校验并读 base64 装配（最近 3 张），
        //    适配器按协议分流；失败静默降级为纯文本，不阻断对话
        attachImageDataUrls(messages, request);
        return messages;
    }

    /**
     * P4 工具链路 system 指引：明确数字必须来自工具、给出今天日期供相对时间换算。
     */
    private String buildToolGuidePrompt() {
        return "你可以通过工具查询本店的真实经营数据：经营日报、菜品销量排行、时段客流分析、"
                + "支付方式分析、分类销量占比、复购率、近期销售趋势。使用规则：\n"
                + "1. 凡涉及具体经营数字（营业额、销量、订单数、占比、排名、复购率等），"
                + "必须先调用对应工具获取真实数据，严禁凭常识或上下文猜测编造；\n"
                + "2. 今天是 " + LocalDate.now() + "（按此日期把“昨天/本周/近几天”换算成 yyyy-MM-dd 入参）；\n"
                + "3. 工具返回的是本店铺真实数据，金额单位均为元人民币；\n"
                + "4. 取数后用中文简洁回答，可使用表格或要点，并说明统计口径与时间范围；"
                + "工具未覆盖的问题再结合餐饮经营常识给建议。";
    }

    /**
     * 把消息列表中全部 user 图片（按时间正序）限量最近 {@link AiAttachmentService#MAX_IMAGES_PER_REQUEST}
     * 张，经附件服务归属校验后读为 data URL，回填到各消息的 imageDataUrls。
     * <p>身份缺失（内部场景）/无附件/读图失败一律静默跳过，按纯文本对话处理。</p>
     */
    private void attachImageDataUrls(List<AIMessage> messages, AIChatRequest request) {
        if (request.getUserId() == null || request.getActorType() == null) {
            return;
        }
        List<AIMessage> userImageMessages = new ArrayList<>();
        List<Long> allImageIds = new ArrayList<>();
        for (AIMessage msg : messages) {
            if ("user".equals(msg.getRole()) && msg.getAttachmentIds() != null
                    && !msg.getAttachmentIds().isEmpty()) {
                userImageMessages.add(msg);
                allImageIds.addAll(msg.getAttachmentIds());
            }
        }
        if (allImageIds.isEmpty()) {
            return;
        }
        // 只装配最近 N 张（保留消息内先后顺序），控制 token 与上游请求体体积
        int maxImages = AiAttachmentService.MAX_IMAGES_PER_REQUEST;
        List<Long> limitedIds = allImageIds.size() > maxImages
                ? new ArrayList<>(allImageIds.subList(allImageIds.size() - maxImages, allImageIds.size()))
                : allImageIds;

        Map<Long, String> dataUrls;
        try {
            dataUrls = aiAttachmentService.mapDataUrls(limitedIds, request.getUserId(),
                    request.getActorType(), request.getTenantId(), maxImages);
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，附件服务异常时降级纯文本
            log.warn("装配图片base64失败，本轮按纯文本继续: conversationId={}",
                    request.getConversationId(), e);
            return;
        }
        if (dataUrls.isEmpty()) {
            return;
        }
        for (AIMessage msg : userImageMessages) {
            List<String> urls = new ArrayList<>();
            for (Long id : msg.getAttachmentIds()) {
                String url = dataUrls.get(id);
                if (url != null) {
                    urls.add(url);
                }
            }
            if (!urls.isEmpty()) {
                msg.setImageDataUrls(urls);
            }
        }
    }

    /**
     * 解析前端传入的附件ID字符串列表：去空白、去重、过滤非法值，限量
     * {@link AiAttachmentService#MAX_IMAGES_PER_REQUEST} 张。ID 归属校验在附件服务完成。
     */
    private List<Long> parseAttachmentIds(List<String> rawIds) {
        List<Long> ids = new ArrayList<>();
        if (rawIds == null || rawIds.isEmpty()) {
            return ids;
        }
        Set<Long> seen = new HashSet<>();
        for (String raw : rawIds) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            // 雪花 ID 最多 19 位，超长直接丢弃，避免无意义 Long 解析异常
            if (trimmed.isEmpty() || trimmed.length() > 20) {
                continue;
            }
            try {
                Long id = Long.valueOf(trimmed);
                if (id > 0L && seen.add(id)) {
                    ids.add(id);
                }
            } catch (NumberFormatException ignore) {
                // 非数字附件ID忽略
            }
            if (ids.size() >= AiAttachmentService.MAX_IMAGES_PER_REQUEST) {
                break;
            }
        }
        return ids;
    }

    /**
     * 从附件元数据 JSON（ai_message.attachments）中还原附件ID（保序），坏 JSON 返回空列表。
     */
    private List<Long> extractAttachmentIds(String attachmentsJson) {
        if (attachmentsJson == null || attachmentsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<AiAttachmentVO> views = OBJECT_MAPPER.readValue(attachmentsJson,
                    new TypeReference<List<AiAttachmentVO>>() {
                    });
            List<Long> ids = new ArrayList<>();
            for (AiAttachmentVO view : views) {
                if (view != null && view.getAttachmentId() != null) {
                    ids.add(view.getAttachmentId());
                }
            }
            return ids;
        } catch (Exception e) {
            // 宽异常兜底：坏 JSON 不阻断历史加载，仅丢失该条图片
            log.warn("解析消息附件JSON失败: json={}", attachmentsJson, e);
            return Collections.emptyList();
        }
    }

    /** 从归属校验后的权威 VO 列表提取ID（保序） */
    private List<Long> extractViewIds(List<AiAttachmentVO> views) {
        List<Long> ids = new ArrayList<>();
        for (AiAttachmentVO view : views) {
            if (view != null && view.getAttachmentId() != null) {
                ids.add(view.getAttachmentId());
            }
        }
        return ids;
    }

    /** 比较两批附件ID是否等价（null/空 视为相同的「无图」） */
    private boolean sameImageIds(List<Long> a, List<Long> b) {
        boolean hasA = a != null && !a.isEmpty();
        boolean hasB = b != null && !b.isEmpty();
        if (!hasA && !hasB) {
            return true;
        }
        if (hasA != hasB) {
            return false;
        }
        return a.equals(b);
    }

    /**
     * P3：优先取 ai_prompt_template 启用中的 SYSTEM 模板（后台可运营编辑），
     * 库缺失/异常/空内容时降级 yml 配置常量（AIConfigProperties）。
     */
    private String getSystemPrompt(String scene) {
        String key = scene != null ? scene : "order_assistant";
        String templated = null;
        try {
            templated = promptTemplateService.getSystemPrompt(key);
        } catch (Exception e) {
            log.warn("读取提示词模板失败 scene={}, 降级默认配置: {}", key, e.getMessage());
        }
        if (templated != null && !templated.trim().isEmpty()) {
            return templated;
        }
        switch (key) {
            case "dish_desc": return aiConfig.getDishDescPrompt();
            case "business_analysis": return aiConfig.getBusinessAnalysisPrompt();
            case "marketing":
                return "你是一个营销文案专家。请根据用户需求生成吸引人的营销文案。文案要有感染力，适合外卖平台推送。";
            case "order_assistant":
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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
            if (jsonStr == null) {
                return result;
            }
            JsonNode root = OBJECT_MAPPER.readTree(jsonStr);
            if (!root.isArray()) {
                return result;
            }
            // 修改点：先收集所有dishId，批量查询，消除N+1问题
            List<Long> dishIds = new ArrayList<>();
            Map<Long, String> dishReasonMap = new HashMap<>();
            collectDishIds(root, dishIds, dishReasonMap);
            if (!dishIds.isEmpty()) {
                buildRecommendedDishes(dishIds, dishReasonMap, tenantId, result);
            }
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.debug("解析AI推荐菜品失败（Mock模式下正常）", e);
        }
        return result;
    }

    /**
     * 收集 AI 返回的菜品 ID 与推荐理由（等价抽取，降低嵌套）。
     *
     * @param root JSON 数组节点
     * @param dishIds 输出：菜品 ID 列表
     * @param dishReasonMap 输出：菜品 ID -> 推荐理由
     */
    private void collectDishIds(JsonNode root, List<Long> dishIds, Map<Long, String> dishReasonMap) {
        for (JsonNode node : root) {
            Long dishId = node.has("dishId") ? node.get("dishId").asLong() : null;
            String reason = node.has("reason") ? node.get("reason").asText() : "";
            if (dishId != null) {
                dishIds.add(dishId);
                dishReasonMap.put(dishId, reason);
            }
        }
    }

    /**
     * 按菜品 ID 批量查询并构建推荐结果（等价抽取，降低嵌套）。
     *
     * @param dishIds 菜品 ID 列表
     * @param dishReasonMap 菜品 ID -> 推荐理由
     * @param tenantId 租户 ID
     * @param result 输出：推荐结果列表
     */
    private void buildRecommendedDishes(List<Long> dishIds, Map<Long, String> dishReasonMap,
            Long tenantId, List<AIRecommendedDish> result) {
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

    // ==================== 私有辅助方法 ====================

    /**
     * 保存AI回复消息到数据库（含去重检查）
     * <p>
     * 设计说明：insert + addMessage(cache) + updateMessageCount 三步非事务绑定，
     * 异步调用上下文不生效。消息已去重（按conversationId+role+content+时间窗口），
     * 重复调用安全。缓存/计数失败通过各自catch降级，不影响主数据。
     * </p>
     *
     * @return 保存后的消息ID
     */
    private Long saveUserMessage(AIChatRequest request) {
        if (request.getConversationId() == null || request.getConversationId().isEmpty()) {
            return null;
        }
        try {
            String clientMsgId = request.getClientMsgId();
            AIMessageRecord existingMsg = null;
            if (clientMsgId != null && !clientMsgId.isEmpty()) {
                // 修改点(2026-09-20)：POST 流式按 (conversationId, clientMsgId) 幂等去重。
                // 旧的「时间窗 + content 全等」会吞掉同文案带图消息与重新生成后的正常消息；
                // MySQL 唯一索引对 NULL 不去重，不带 clientMsgId 的非流式 /chat 链路走下方时间窗兜底。
                LambdaQueryWrapper<AIMessageRecord> idempotentWrapper = new LambdaQueryWrapper<>();
                idempotentWrapper.eq(AIMessageRecord::getConversationId, request.getConversationId())
                        .eq(AIMessageRecord::getClientMsgId, clientMsgId)
                        .eq(AIMessageRecord::getIsDeleted, 0);
                existingMsg = messageRecordMapper.selectOne(idempotentWrapper);
            } else {
                // 非流式 /chat 链路（无幂等键）：5 秒时间窗 + 内容全等去重
                LambdaQueryWrapper<AIMessageRecord> dedupWrapper = new LambdaQueryWrapper<>();
                dedupWrapper.eq(AIMessageRecord::getConversationId, request.getConversationId())
                        .eq(AIMessageRecord::getRole, "user")
                        .eq(AIMessageRecord::getContent, request.getMessage())
                        .eq(AIMessageRecord::getIsDeleted, 0)
                        .gt(AIMessageRecord::getCreateTime, LocalDateTime.now().minusSeconds(5))
                        .orderByDesc(AIMessageRecord::getCreateTime);
                existingMsg = messageRecordMapper.selectOne(dedupWrapper);
            }
            if (existingMsg != null) {
                log.debug("检测到重复用户消息，跳过保存: conversationId={}, contentLength={}",
                        request.getConversationId(), request.getMessage() != null ? request.getMessage().length() : 0);
                return existingMsg.getId();
            }

            // P2：附件ID经归属校验后转权威 VO 列表落库（防前端伪造 mime/url/尺寸），
            // 同时作为上下文缓存中该条消息的 attachmentIds（只存ID，不缓存 base64）
            List<Long> attachmentIds = parseAttachmentIds(request.getAttachments());
            List<AiAttachmentVO> attachmentViews = null;
            if (!attachmentIds.isEmpty() && request.getUserId() != null
                    && request.getActorType() != null) {
                attachmentViews = aiAttachmentService.describeViews(attachmentIds,
                        request.getUserId(), request.getActorType(), request.getTenantId(),
                        AiAttachmentService.MAX_IMAGES_PER_REQUEST);
            }

            AIMessageRecord record = new AIMessageRecord();
            record.setConversationId(request.getConversationId());
            record.setUserId(request.getUserId());
            record.setRole("user");
            record.setContent(request.getMessage());
            record.setMessageType("text");
            record.setStatus(AiChatConstants.MSG_STATUS_COMPLETED);
            record.setClientMsgId(clientMsgId);
            record.setIsDeleted(0);
            record.setCreateTime(LocalDateTime.now());
            if (attachmentViews != null && !attachmentViews.isEmpty()) {
                record.setAttachments(OBJECT_MAPPER.writeValueAsString(attachmentViews));
            }
            messageRecordMapper.insert(record);

            // 注入上下文记忆（带附件ID，纯图消息 content 可空）
            conversationContextService.addMessage(
                    request.getConversationId(), "user", request.getMessage(),
                    attachmentViews != null && !attachmentViews.isEmpty()
                            ? extractViewIds(attachmentViews) : null);

            // 更新会话消息计数
            updateMessageCount(request.getConversationId());
            return record.getId();
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.warn("保存用户消息失败: conversationId={}", request.getConversationId(), e);
            return null;
        }
    }

    /**
     * 保存AI回复消息到数据库（含去重检查）
     * <p>
     * 设计说明：与 {@link #saveUserMessage} 一致，异步非事务设计，
     * 重复调用安全（去重保护），缓存/计数失败降级不影响主数据。
     * </p>
     *
     * @return 保存后的消息ID
     */
    private Long saveAiMessage(String conversationId, Long userId, String content,
                                Integer tokensUsed, List<AIRecommendedDish> dishes) {
        return saveAiMessage(conversationId, userId, content, tokensUsed, dishes,
                AiChatConstants.MSG_STATUS_COMPLETED);
    }

    /**
     * 保存AI回复消息（可指定状态：completed/stopped/failed）。
     *
     * @param status 消息状态，见 {@link AiChatConstants}
     */
    private Long saveAiMessage(String conversationId, Long userId, String content,
                                Integer tokensUsed, List<AIRecommendedDish> dishes, String status) {
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
            record.setStatus(status != null ? status : AiChatConstants.MSG_STATUS_COMPLETED);
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
                    // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
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
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.warn("更新对话标题失败: conversationId={}", conversationId, e);
        }
    }

    /**
     * 获取 context stats。
     * @param conversationId 参数 conversationId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getContextStats(String conversationId) {
        return conversationManagementService.getContextStats(conversationId);
    }

    /**
     * 重置 context。
     * @param conversationId 参数 conversationId
     */
    @Override
    public void resetContext(String conversationId) {
        conversationManagementService.resetContext(conversationId);
    }

    /**
     * 搜索 conversations。
     * @param userId 参数 userId
     * @param keyword 参数 keyword
     * @param page 参数 page
     * @param pageSize 参数 pageSize
     * @return 返回结果
     */
    @Override
    public List<AIConversation> searchConversations(Long userId, String keyword, int page, int pageSize) {
        return conversationManagementService.searchConversations(userId, keyword, page, pageSize);
    }

    /**
     * 校验 conversation ownership。
     * @param conversationId 参数 conversationId
     * @return 返回结果
     */
    @Override
    public Long validateConversationOwnership(String conversationId) {
        return conversationManagementService.validateConversationOwnership(conversationId);
    }

    /**
     * 单次 SSE 流式会话的状态载体：中止标志、上游中止动作、内容累积与一次性落库守卫。
     * <p>实现 Runnable 直接提交到 aiExecutor；作为 AbortableStreamCallback 传入适配器，
     * 用户停止时 abort() 置位并断开上游 HttpURLConnection，已生成片段以 stopped 状态保留。</p>
     */
    private final class ChatStreamSession implements AbortableStreamCallback, Runnable, ToolEventSink {

        private final SseEmitter emitter;
        private final AIChatRequest request;
        private final AiProviderConfig providerConfig;
        private final Long tenantId;
        private final String conversationId;
        private final String scene;
        private final Long userId;

        private final StringBuilder fullContent = new StringBuilder();
        private final AtomicBoolean aborted = new AtomicBoolean(false);
        private final AtomicBoolean finalized = new AtomicBoolean(false);
        private final AtomicReference<Runnable> abortActionRef = new AtomicReference<>();

        private List<AIRecommendedDish> parsedDishes;
        private Long savedAiMsgId;
        private long firstTokenTime;
        private final long streamStart = System.currentTimeMillis();

        ChatStreamSession(SseEmitter emitter, AIChatRequest request, AiProviderConfig providerConfig,
                          Long tenantId) {
            this.emitter = emitter;
            this.request = request;
            this.providerConfig = providerConfig;
            this.tenantId = tenantId;
            this.conversationId = request.getConversationId();
            this.scene = request.getScene();
            this.userId = request.getUserId();
        }

        @Override
        public boolean isAborted() {
            return aborted.get();
        }

        @Override
        public void registerAbortAction(Runnable action) {
            abortActionRef.set(action);
        }

        /**
         * 中止生成：置位并断开上游连接（可由 emitter 超时/断链回调触发）。
         */
        void abort() {
            if (aborted.compareAndSet(false, true)) {
                Runnable action = abortActionRef.get();
                if (action != null) {
                    try {
                        action.run();
                    } catch (Exception e) {
                        log.debug("中止上游连接失败（可忽略）: {}", e.getMessage());
                    }
                }
            }
        }

        @Override
        public void onToken(String token, boolean isLast) {
            // 停止后忽略残余回调（落库由 execute 兜底统一处理）
            if (aborted.get()) {
                return;
            }
            try {
                if (isLast) {
                    if (fullContent.length() == 0) {
                        // 错误提示（Key 无效/熔断等）以 isLast token 形式返回且无内容
                        completeWithError(emitter, token, conversationId);
                    } else {
                        finalizeStream(false);
                    }
                } else {
                    fullContent.append(token);
                    long now = System.currentTimeMillis();
                    if (firstTokenTime == 0) {
                        firstTokenTime = now - streamStart;
                        log.debug("首字延迟: {}ms, conversationId={}", firstTokenTime, conversationId);
                    }
                    Map<String, Object> chunkData = new HashMap<>();
                    chunkData.put("text", token);
                    try {
                        emitter.send(SseEmitter.event().name("message").data(chunkData));
                    } catch (Exception sendEx) {
                        // 客户端已断开：中止上游，避免继续消耗 token
                        log.debug("SSE推送失败，中止上游: conversationId={}", conversationId);
                        abort();
                    }
                }
            } catch (Exception e) {
                // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
                log.warn("SSE token处理失败: conversationId={}", conversationId, e);
            }
        }

        @Override
        public void run() {
            execute();
        }

        /**
         * P4：工具调用状态事件实时转发前端（running → done/error），历史消息不回放。
         */
        @Override
        public void onToolEvent(ToolEvent event) {
            if (aborted.get() || event == null) {
                return;
            }
            try {
                Map<String, Object> data = new HashMap<>();
                data.put("name", event.getName());
                data.put("label", event.getLabel());
                data.put("status", event.getStatus());
                if (event.getSummary() != null) {
                    data.put("summary", event.getSummary());
                }
                emitter.send(SseEmitter.event().name("tool").data(data));
            } catch (Exception e) {
                // 连接可能已断开；工具事件为增强展示，推送失败不影响主链路
                log.debug("tool事件推送失败（连接可能已断开）: conversationId={}", conversationId);
            }
        }

        /**
         * 异步执行流式对话。
         */
        private void execute() {
            try {
                // 1) 能力事件：按供应商配置 capabilities 下发（P2 vision；P4 tools 配置+协议双条件）
                Map<String, Boolean> providerCaps = aiProviderManager.getCapabilities();
                Map<String, Object> caps = new HashMap<>();
                caps.put("chat", Boolean.TRUE);
                caps.put("vision", Boolean.TRUE.equals(providerCaps.get("vision")));
                caps.put("tools", aiProviderManager.supportsToolCalling());
                try {
                    emitter.send(SseEmitter.event().name("capabilities").data(caps));
                } catch (Exception e) {
                    // 连接建立即断开，无需继续
                    abort();
                    return;
                }

                // 2) 重新生成：重放最后一条用户消息；普通发送：持久化用户消息
                boolean regenerate = Boolean.TRUE.equals(request.getRegenerate());
                if (regenerate) {
                    String lastUserMessage = prepareRegenerate();
                    if (lastUserMessage == null) {
                        completeWithError(emitter, "没有可重新生成的提问", conversationId);
                        return;
                    }
                    request.setMessage(lastUserMessage);
                } else {
                    // 修改点(2026-09-18)：用户消息持久化在异步线程执行，避免拖慢 SSE 建立
                    saveUserMessage(request);
                }

                List<AIMessage> messages = buildMessages(request);
                int maxTokens = (providerConfig != null && providerConfig.getMaxTokens() != null)
                        ? providerConfig.getMaxTokens() : aiConfig.getMaxTokens();
                double temperature = (providerConfig != null && providerConfig.getTemperature() != null)
                        ? providerConfig.getTemperature() : aiConfig.getTemperature();

                // 3) 经营分析场景且供应商支持 function calling：走工具多轮编排（真实查报表）；
                //    其余场景（含不支持 tools 的供应商，已在 buildMessages 注入经营快照）保持原流式链路
                boolean useTools = "business_analysis".equals(scene)
                        && aiProviderManager.supportsToolCalling();
                ModelTurn toolTurn = null;
                if (useTools) {
                    // 工具轮文本出口：只转发增量，绝不发 isLast——最终收尾由 execute 统一做，
                    // 否则会提前触发 finalizeStream（session 自身 onToken 的 isLast 语义不能复用）
                    AiModelAdapter.StreamCallback toolTextSink = new AiModelAdapter.StreamCallback() {
                        @Override
                        public void onToken(String token, boolean isLast) {
                            if (aborted.get() || token == null || token.isEmpty()) {
                                return;
                            }
                            fullContent.append(token);
                            long now = System.currentTimeMillis();
                            if (firstTokenTime == 0) {
                                firstTokenTime = now - streamStart;
                                log.debug("首字延迟(工具轮): {}ms, conversationId={}",
                                        firstTokenTime, conversationId);
                            }
                            Map<String, Object> chunkData = new HashMap<>();
                            chunkData.put("text", token);
                            try {
                                emitter.send(SseEmitter.event().name("message").data(chunkData));
                            } catch (Exception sendEx) {
                                // 客户端已断开：中止上游，避免继续消耗 token
                                log.debug("SSE推送失败，中止上游: conversationId={}", conversationId);
                                abort();
                            }
                        }
                    };
                    toolTurn = aiToolOrchestrator.run(messages, maxTokens, temperature, tenantId,
                            this, toolTextSink, this);
                } else {
                    // 适配器真流式或 manager 分块降级
                    aiProviderManager.streamChat(messages, maxTokens, temperature, this);
                }

                // 4) 收尾：中止时保留 stopped 片段；工具轮整体报错且无文本则下发错误；
                //    适配器未回调 isLast 时保证连接不挂起
                if (aborted.get()) {
                    finalizeStream(true);
                } else if (toolTurn != null && toolTurn.isError() && fullContent.length() == 0) {
                    String errMsg = toolTurn.getErrorMessage();
                    completeWithError(emitter,
                            errMsg != null ? errMsg : "AI服务暂时不可用，请稍后重试", conversationId);
                } else if (!finalized.get()) {
                    if (fullContent.length() > 0) {
                        finalizeStream(false);
                    } else {
                        completeWithError(emitter, "AI服务返回了空响应", conversationId);
                    }
                }

                // 5) 首字延迟观测
                if (firstTokenTime > 3000) {
                    log.warn("首字延迟过高: {}ms, totalTime={}ms, provider={}",
                            firstTokenTime, System.currentTimeMillis() - streamStart,
                            providerConfig != null ? providerConfig.getProviderCode() : "default");
                }
            } catch (Exception e) {
                if (aborted.get()) {
                    finalizeStream(true);
                } else {
                    handleStreamError(emitter, conversationId, e);
                }
            }
        }

        /**
         * 结束并落库：stopped=true 时片段以 stopped 状态保留，且不再触发菜品推荐。
         */
        private void finalizeStream(boolean stopped) {
            if (!finalized.compareAndSet(false, true)) {
                return;
            }
            try {
                String content = fullContent.toString();
                if (content.isEmpty()) {
                    if (stopped) {
                        emitter.complete();
                    } else {
                        completeWithError(emitter, "AI服务返回了空响应", conversationId);
                    }
                    return;
                }

                List<AIRecommendedDish> dishes = null;
                if ("order_assistant".equals(scene) && !stopped) {
                    dishes = parseRecommendedDishes(content, tenantId);
                    parsedDishes = dishes;
                    content = cleanJsonFromContent(content);
                }

                String status = stopped
                        ? AiChatConstants.MSG_STATUS_STOPPED : AiChatConstants.MSG_STATUS_COMPLETED;
                savedAiMsgId = saveAiMessage(conversationId, userId, content, null, dishes, status);

                Map<String, Object> doneData = new HashMap<>();
                doneData.put("status", stopped ? "stopped" : "complete");
                doneData.put("stopped", stopped);
                doneData.put("conversationId", conversationId);
                if (savedAiMsgId != null) {
                    doneData.put("messageId", savedAiMsgId);
                }
                try {
                    emitter.send(SseEmitter.event().name("done").data(doneData));
                } catch (Exception sendEx) {
                    // 客户端主动断开时 done 推不出去，落库已成功，忽略
                    log.debug("done事件推送失败（连接可能已断开）: conversationId={}", conversationId);
                }

                if (!stopped && "order_assistant".equals(scene) && dishes != null && !dishes.isEmpty()) {
                    sendDishesEvent(emitter, dishes);
                }

                updateConversationTitle(conversationId, request.getMessage());
                emitter.complete();
            } catch (Exception e) {
                log.warn("流式收尾失败: conversationId={}", conversationId, e);
                try {
                    emitter.complete();
                } catch (Exception ignore) {
                    // 宽异常兜底：连接状态已终结
                }
            }
        }

        /**
         * 重新生成前置处理：定位最后一条用户消息，逻辑删除其后的 assistant 消息，
         * 并清除内存上下文（buildMessages 缓存 miss 后从 DB 重建）。
         *
         * @return 最后一条用户消息内容；无用户消息或归属校验失败返回 null
         */
        private String prepareRegenerate() {
            if (userId == null || request.getActorType() == null) {
                return null;
            }
            if (conversationManagementService.validateConversationOwnership(
                    conversationId, userId, request.getActorType()) == null) {
                log.warn("重新生成被拒绝（会话归属不匹配）: conversationId={}, userId={}", conversationId, userId);
                return null;
            }
            List<AIMessageRecord> history = conversationManagementService.getConversationMessages(
                    conversationId, userId, request.getActorType());
            String lastUserContent = null;
            int lastUserIdx = -1;
            for (int i = 0; i < history.size(); i++) {
                if ("user".equals(history.get(i).getRole())) {
                    lastUserIdx = i;
                    lastUserContent = history.get(i).getContent();
                }
            }
            if (lastUserContent == null) {
                return null;
            }
            for (int i = lastUserIdx + 1; i < history.size(); i++) {
                AIMessageRecord record = history.get(i);
                if ("assistant".equals(record.getRole())) {
                    LambdaUpdateWrapper<AIMessageRecord> uw = new LambdaUpdateWrapper<>();
                    uw.eq(AIMessageRecord::getId, record.getId())
                            .eq(AIMessageRecord::getIsDeleted, 0)
                            .set(AIMessageRecord::getIsDeleted, 1);
                    messageRecordMapper.update(null, uw);
                }
            }
            conversationContextService.clearContext(conversationId);
            return lastUserContent;
        }
    }
}



