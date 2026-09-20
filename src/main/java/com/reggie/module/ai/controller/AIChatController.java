package com.reggie.module.ai.controller;
import com.reggie.common.utils.PageUtils;

import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.module.ai.model.AiChatConstants;
import com.reggie.module.ai.model.AIChatRequest;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIConversation;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AIMessageRecord;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.dto.BusinessAnalysisRequest;
import com.reggie.module.ai.dto.ChatStreamRequest;
import com.reggie.module.ai.dto.CreateConversationRequest;
import com.reggie.module.ai.dto.DishDescriptionRequest;
import com.reggie.module.ai.dto.OrderAssistantRequest;
import com.reggie.module.ai.dto.RecordFeedbackRequest;
import com.reggie.module.ai.dto.RenameConversationRequest;
import com.reggie.module.ai.service.AIChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>
 * AI聊天控制器
 * 提供智能点餐、菜品描述生成、经营分析等AI能力
 * 新增：流式响应(SSE)、对话管理、反馈记录
 * </p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@Validated
@RequestMapping("/api/ai")
@Tag(name = "AI智能助手", description = "AI智能点餐推荐、菜品描述生成、经营分析、流式对话")
public class AIChatController {

    @Resource
    private AIChatService aiChatService;

    @Resource
    private com.reggie.module.ai.service.UserProfileService userProfileService;

    @Resource
    private com.reggie.module.ai.provider.AiProviderManager aiProviderManager;

    /**
     * AI 健康探活线程池（见 {@code AsyncConfig#aiHealthProbeExecutor}）。
     * 用于将探活调用与 HTTP 请求线程隔离，配合 Future.get(timeout) 实现有界超时。
     */
    @Resource(name = "aiHealthProbeExecutor")
    private ThreadPoolTaskExecutor aiHealthProbeExecutor;

    // ==================== 核心对话接口 ====================

    /**
     * 通用AI对话接口（非流式）
     * @param request AI对话请求参数
     * @return AI回复结果
     */
    @PostMapping("/chat")
    @RateLimit(maxRequestsPerSecond = 2, type = RateLimitType.USER)
    @Operation(summary = "通用AI对话", description = "支持多场景：点餐推荐、菜品描述、经营分析、营销文案")
    public R<AIChatResponse> chat(@Parameter(description = "AI对话请求参数（消息内容、场景、对话ID）", required =
            true) @Valid @RequestBody AIChatRequest request, HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        if (userId != null) request.setUserId(userId);
        // 身份/租户强制以服务端会话为准，忽略请求体伪造值（附件归属校验依赖）
        request.setActorType(resolveActorType(httpRequest));
        request.setTenantId(BaseContext.getCurrentTenantId());
        log.info("AI对话请求: userId={}, scene={}, messageLength={}",
                userId, request.getScene(),
                request.getMessage() != null ? request.getMessage().length() : 0);

        if (request.getConversationId() == null || request.getConversationId().isEmpty()) {
            AIConversation conv = aiChatService.createConversation(userId, null,
                    request.getScene() != null ? request.getScene() : "business_analysis");
            request.setConversationId(conv.getConversationId());
            log.info("自动创建新对话: conversationId={}", conv.getConversationId());
        }

        AIChatResponse response = aiChatService.chat(request);

        if (response.getData() == null) {
            response.setData(new HashMap<>());
        }
        response.getData().put("conversationId", request.getConversationId());

        return R.success(response);
    }

    /**
     * 通用AI对话接口（SSE流式）
     * @param message 用户消息
     * @param scene 场景类型（可选）
     * @param conversationId 对话ID（可选）
     * @return SSE流式响应
     */
    @GetMapping("/chat/stream")
    @RateLimit(maxRequestsPerSecond = 1, type = RateLimitType.USER)
    @Operation(summary = "AI流式对话", description = "SSE流式输出，逐字显示AI回复")
    @Parameter(description = "用户消息")
    public SseEmitter chatStream(@RequestParam @Size(max = 2000, message = "消息长度不能超过2000字符") String message,
                                  @RequestParam(required = false) String scene,
                                  @Parameter(description = "对话ID")
                                  @RequestParam(required = false) String conversationId) {
        Long userId = BaseContext.getCurrentId();
        log.info("AI流式对话: userId={}, scene={}, messageLength={}", userId, scene, message.length());

        if (conversationId == null || conversationId.isEmpty()) {
            AIConversation conv = aiChatService.createConversation(userId, null,
                    scene != null ? scene : "order_assistant");
            conversationId = conv.getConversationId();
        }

        AIChatRequest request = AIChatRequest.builder()
                .message(message)
                .scene(scene != null ? scene : "order_assistant")
                .conversationId(conversationId)
                .userId(userId)
                .build();
        return aiChatService.chatStream(request);
    }

    /**
     * 通用AI对话接口（POST SSE流式，P1 新协议）。
     * <p>POST 化目的：携带 clientMsgId 幂等键、附件、context；前端可用 AbortController
     * 主动停止生成（断开连接触发后端中止上游请求）。</p>
     *
     * @param params 流式请求参数（message/regenerate 二选一）
     * @param httpRequest Servlet 请求（用于解析员工/用户身份）
     * @return SSE 流式响应：capabilities → message* → done/error/dishes
     */
    @PostMapping("/chat/stream")
    @RateLimit(maxRequestsPerSecond = 1, type = RateLimitType.USER)
    @Operation(summary = "AI流式对话（POST）", description = "POST+SSE流式输出，支持停止生成、幂等重试、重新生成")
    public SseEmitter chatStreamPost(@Valid @RequestBody ChatStreamRequest params, HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        String actorType = resolveActorType(httpRequest);
        boolean regenerate = Boolean.TRUE.equals(params.getRegenerate());
        String message = params.getMessage();
        // P2：允许纯图片消息（文本为空但携带附件ID）
        boolean hasAttachments = params.getAttachments() != null && !params.getAttachments().isEmpty();
        if (!regenerate && !hasAttachments && (message == null || message.trim().isEmpty())) {
            throw new CustomException("消息内容不能为空");
        }
        String scene = params.getScene() != null ? params.getScene() : "order_assistant";
        String conversationId = params.getConversationId();

        if (conversationId == null || conversationId.isEmpty()) {
            AIConversation conv = aiChatService.createConversation(userId, actorType, null, scene);
            conversationId = conv.getConversationId();
        } else if (userId == null
                || aiChatService.validateConversationOwnership(conversationId, userId, actorType) == null) {
            // 已有会话必须归属当前身份，拒绝员工/用户串用 ID 撞号会话
            throw new CustomException("对话不存在或无权访问");
        }

        log.info("AI流式对话(POST): userId={}, actorType={}, scene={}, regenerate={}, messageLength={}",
                userId, actorType, scene, regenerate, message != null ? message.length() : 0);

        AIChatRequest request = AIChatRequest.builder()
                .message(message)
                .scene(scene)
                .conversationId(conversationId)
                .userId(userId)
                .actorType(actorType)
                .tenantId(BaseContext.getCurrentTenantId())
                .clientMsgId(params.getClientMsgId())
                .attachments(params.getAttachments())
                .context(params.getContext())
                .regenerate(regenerate)
                .build();
        return aiChatService.chatStream(request);
    }

    /**
     * 从登录会话解析身份类型：session 含 employee=后台员工，否则按 C 端用户处理。
     * <p>不动公共 BaseContext/过滤器，仅 AI 模块内部消歧员工与用户 ID 撞号。</p>
     */
    private String resolveActorType(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute("employee") != null) {
            return AiChatConstants.ACTOR_EMPLOYEE;
        }
        return AiChatConstants.ACTOR_CUSTOMER;
    }

    /**
     * 智能点餐推荐（简化接口，非流式）
     * @param params 请求参数（message、conversationId）
     * @return AI推荐结果
     */
    @PostMapping("/order-assistant")
    @RateLimit(maxRequestsPerSecond = 2, type = RateLimitType.USER)
    @Operation(summary = "智能点餐助手", description = "用户用自然语言描述需求，AI推荐最合适的菜品")
    public R<AIChatResponse> orderAssistant(@Parameter(description = "点餐推荐请求参数（消息内容、对话ID、附件）", required =
            true) @Valid @RequestBody OrderAssistantRequest params, HttpServletRequest httpRequest) {
        String message = params.getMessage();
        // P2：允许纯图片消息（文本为空但携带附件ID）
        boolean hasAttachments = params.getAttachments() != null && !params.getAttachments().isEmpty();
        if (!hasAttachments && (message == null || message.trim().isEmpty())) {
            throw new CustomException("消息内容不能为空");
        }
        // #10 安全修复：删除客户端 userId 入参，统一从登录上下文获取，防止越权 IDOR
        Long userId = BaseContext.getCurrentId();
        // 身份/租户强制以服务端会话为准（附件 owner 归属校验依赖）
        String actorType = resolveActorType(httpRequest);
        Long tenantId = BaseContext.getCurrentTenantId();
        String conversationId = params.getConversationId();

        log.info("智能点餐请求: userId={}, actorType={}, messageLength={}, attachments={}",
                userId, actorType, message != null ? message.length() : 0,
                hasAttachments ? params.getAttachments().size() : 0);

        if (conversationId == null || conversationId.isEmpty()) {
            AIConversation conv = aiChatService.createConversation(userId, actorType, "order_assistant");
            conversationId = conv.getConversationId();
        }

        // 修改点：已在Controller层统一创建对话，Service层复用此conversationId避免重复创建
        AIChatResponse response = aiChatService.orderAssistant(message, userId, conversationId,
                params.getAttachments(), actorType, tenantId);
        // 附加 conversationId 到响应中，方便前端后续使用
        if (response != null && response.getData() == null) {
            response.setData(new HashMap<>());
        }
        if (response != null && response.getData() != null && conversationId != null) {
            response.getData().put("conversationId", conversationId);
        }
        return R.success(response);
    }

    /**
     * 智能点餐推荐（SSE流式）
     * @param message 用户消息
     * @param conversationId 对话ID（可选）
     * @return SSE流式响应
     */
    @GetMapping("/order-assistant/stream")
    @RateLimit(maxRequestsPerSecond = 1, type = RateLimitType.USER)
    @Operation(summary = "智能点餐助手（流式）", description = "SSE流式输出推荐结果")
    @Parameter(description = "用户消息")
    public SseEmitter orderAssistantStream(@RequestParam @Size(max = 2000, message = "消息长度不能超过2000字符") String message,
                                            @Parameter(description = "对话ID")
                                            @RequestParam(required = false) String conversationId) {
        Long userId = BaseContext.getCurrentId();
        log.info("智能点餐流式: userId={}, messageLength={}", userId, message.length());
        return aiChatService.orderAssistantStream(message, userId, conversationId);
    }

    // ==================== 辅助功能 ====================

    /**
     * 生成菜品描述
     * @param params 请求参数（dishName、categoryName、ingredients）
     * @return 菜品描述文案
     */
    @PostMapping("/dish-description")
    @RateLimit(maxRequestsPerSecond = 1, type = RateLimitType.USER)
    @Operation(summary = "AI菜品描述生成", description = "输入菜品名称，AI生成专业美食描述文案")
    public R<String> generateDishDescription(@Parameter(description = "菜品描述请求参数（菜品名、分类、食材）", required =
            true) @Valid @RequestBody DishDescriptionRequest params) {
        String dishName = params.getDishName();
        String categoryName = params.getCategoryName();
        String ingredients = params.getIngredients();
        if (categoryName == null) categoryName = "";
        if (ingredients == null) ingredients = "";
        log.info("生成菜品描述: dishName={}", dishName);
        String description = aiChatService.generateDishDescription(dishName, categoryName, ingredients);
        return R.success(description);
    }

    /**
     * 经营数据分析
     * @param params 请求参数（question、data）
     * @return AI分析结果
     */
    @PostMapping("/business-analysis")
    @RateLimit(maxRequestsPerSecond = 1, type = RateLimitType.USER)
    @Operation(summary = "AI经营分析", description = "输入经营数据和问题，AI提供专业分析")
    public R<String> analyzeBusiness(@Parameter(description = "经营分析请求参数（问题、数据）", required =
            true) @Valid @RequestBody BusinessAnalysisRequest params) {
        String question = params.getQuestion();
        String dataJson = params.getData();
        if (dataJson == null) dataJson = "{}";
        log.info("经营分析请求: question={}", question);
        String analysis = aiChatService.analyzeBusiness(question, dataJson);
        return R.success(analysis);
    }

    /**
     * AI 健康探活超时时间（秒）。
     * <p>
     * 探活属于「体验型」接口，仅用于前端展示连接状态，不应让请求长时间挂起，
     * 因此取值远小于供应商配置的业务超时（30~60 秒）。
     */
    private static final int AI_HEALTH_PROBE_TIMEOUT_SECONDS = 3;

    /**
     * AI服务健康检查
     * <p>
     * 修改点：探活改为「有界超时」执行。原实现在请求线程中同步调用外部 AI 接口，
     * 供应商不可达时需等待完整连接超时（配置 30s）才返回，前端请求长时间挂起（无响应）。
     * 现改为提交到专用线程池并以 3 秒为上限等待结果，超时即判定不可用并快速返回，
     * 避免管理端页面出现悬挂请求。
     */
    @GetMapping("/health")
    @RateLimit(maxRequestsPerSecond = 1, type = RateLimitType.IP)
    @Operation(summary = "AI服务健康检查", description = "检查AI服务是否可用（探活上限3秒，超时判定不可用）")
    public R<Map<String, Object>> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("features", Arrays.asList("streaming", "conversation", "feedback", "order_assistant",
                "business_analysis"));

        Future<AIChatResponse> probe = null;
        try {
            probe = aiHealthProbeExecutor.submit(() -> aiProviderManager.chat(
                    Arrays.asList(AIMessage.builder().role("user").content("ping").build()), 50, 0.1));
            AIChatResponse testResponse = probe.get(AI_HEALTH_PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            boolean available = testResponse != null && testResponse.getContent() != null
                    && !testResponse.getContent().contains("未配置")
                    && !testResponse.getContent().contains("未就绪")
                    && !testResponse.getContent().contains("不可用")
                    && !testResponse.getContent().contains("失败");
            result.put("available", available);
            result.put("model", testResponse != null ? testResponse.getModel() : "unknown");
        } catch (TimeoutException e) {
            if (probe != null) {
                probe.cancel(true);
            }
            log.warn("AI健康检查探活超时（{}秒），判定为不可用", AI_HEALTH_PROBE_TIMEOUT_SECONDS);
            result.put("available", false);
            result.put("model", "unknown");
        } catch (RejectedExecutionException e) {
            log.warn("AI健康检查探活任务被线程池拒绝，判定为不可用", e);
            result.put("available", false);
            result.put("model", "unknown");
        } catch (ExecutionException e) {
            log.warn("AI健康检查探活执行失败，判定为不可用", e);
            result.put("available", false);
            result.put("model", "unknown");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("AI健康检查探活被中断，判定为不可用");
            result.put("available", false);
            result.put("model", "unknown");
        }
        return R.success(result);
    }

    // ==================== 对话管理 ====================

    /**
     * 获取用户对话列表
     * @param page 页码
     * @param pageSize 每页条数
     * @return 对话列表
     */
    @GetMapping("/conversations")
    @Operation(summary = "获取对话列表", description = "获取当前用户的AI对话历史列表")
    @Parameter(description = "页码")
    public R<List<AIConversation>> getConversations(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                     @Parameter(description = "每页条数")
                                                     @RequestParam(defaultValue =
                                                             "20") @Min(1) @Max(100) int pageSize,
                                                     HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        String actorType = resolveActorType(httpRequest);
        // 修改点(2026-09-20)：员工与 C 端用户按 actorType 分流，消除 ID 撞号串会话
        List<AIConversation> conversations = aiChatService.getUserConversations(
                userId, actorType, page, PageUtils.cap(pageSize));
        return R.success(conversations);
    }

    /**
     * 获取对话详情（含消息历史）
     * @param conversationId 对话ID
     * @return 消息历史列表
     */
    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "获取对话详情", description = "获取指定对话的消息历史")
    @Parameter(description = "对话ID")
    public R<List<AIMessageRecord>> getConversationDetail(@PathVariable String conversationId,
                                                          HttpServletRequest httpRequest) {
        // 修复 P2-9：校验 conversationId 属于当前身份，防止 IDOR 越权（统一走 Service 层）
        Long userId = BaseContext.getCurrentId();
        String actorType = resolveActorType(httpRequest);
        if (userId == null
                || aiChatService.validateConversationOwnership(conversationId, userId, actorType) == null) {
            return R.error("对话不存在或无权访问");
        }
        List<AIMessageRecord> messages = aiChatService.getConversationMessages(conversationId, userId, actorType);
        return R.success(messages);
    }

    /**
     * 创建新对话
     * @param params 请求参数（title、scene）
     * @return 创建的对话信息
     */
    @PostMapping("/conversations")
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
    @Operation(summary = "创建对话", description = "创建新的AI对话")
    public R<AIConversation> createConversation(@Parameter(description = "创建对话请求参数（标题、场景，可选）", required =
            false) @RequestBody(required = false) CreateConversationRequest params,
                                                HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        String actorType = resolveActorType(httpRequest);
        String title = params != null ? params.getTitle() : null;
        String scene = params != null && params.getScene() != null ? params.getScene() : "order_assistant";
        AIConversation conv = aiChatService.createConversation(userId, actorType, title, scene);
        return R.success(conv);
    }

    /**
     * 删除对话
     * @param conversationId 对话ID
     * @return 操作结果
     */
    @DeleteMapping("/conversations/{conversationId}")
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
    @Operation(summary = "删除对话", description = "软删除指定对话")
    @Parameter(description = "对话ID")
    public R<String> deleteConversation(@PathVariable String conversationId, HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        String actorType = resolveActorType(httpRequest);
        aiChatService.deleteConversation(conversationId, userId, actorType);
        return R.success("删除成功");
    }

    /**
     * 重命名对话
     * @param conversationId 对话ID
     * @param params 新标题
     * @return 操作结果
     */
    @PatchMapping("/conversations/{conversationId}")
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
    @Operation(summary = "重命名对话", description = "修改指定AI对话的标题")
    public R<String> renameConversation(@PathVariable String conversationId,
                                        @Valid @RequestBody RenameConversationRequest params,
                                        HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        String actorType = resolveActorType(httpRequest);
        String title = params.getTitle().trim();
        boolean ok = aiChatService.renameConversation(conversationId, userId, actorType, title);
        return ok ? R.success("重命名成功") : R.error("对话不存在或无权访问");
    }

    /**
     * 删除单条消息（重新生成/手动删除气泡场景；同时失效上下文缓存）
     * @param conversationId 对话ID
     * @param messageId 消息ID
     * @return 操作结果
     */
    @DeleteMapping("/conversations/{conversationId}/messages/{messageId}")
    @RateLimit(maxRequestsPerSecond = 5, type = RateLimitType.USER)
    @Operation(summary = "删除单条消息", description = "逻辑删除对话内的一条消息并重建上下文")
    public R<String> deleteMessage(@PathVariable String conversationId,
                                   @PathVariable Long messageId,
                                   HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        String actorType = resolveActorType(httpRequest);
        boolean ok = aiChatService.deleteMessage(conversationId, messageId, userId, actorType);
        return ok ? R.success("删除成功") : R.error("消息不存在或无权操作");
    }

    // ==================== 反馈记录 ====================

    /**
     * 记录用户反馈
     * @param params 请求参数（messageId、feedbackType）
     * @return 操作结果
     */
    @PostMapping("/feedback")
    @RateLimit(maxRequestsPerSecond = 10, type = RateLimitType.USER)
    @Operation(summary = "记录反馈", description = "用户对AI回复的反馈（有用/没用）")
    public R<String> recordFeedback(@Parameter(description = "反馈请求参数（消息ID、反馈类型）", required =
            true) @Valid @RequestBody RecordFeedbackRequest params) {
        Long userId = BaseContext.getCurrentId();
        Long messageId = params.getMessageId();
        String feedbackType = params.getFeedbackType();
        aiChatService.recordFeedback(messageId, feedbackType, userId);
        return R.success("反馈记录成功");
    }

    // ==================== 用户画像 ====================

    /**
     * 获取用户画像摘要
     */
    @GetMapping("/profile/summary")
    @Operation(summary = "获取用户画像", description = "返回用户口味偏好、常点菜品等标签")
    public R<Map<String, Object>> getProfileSummary() {
        Long userId = BaseContext.getCurrentId();
        Map<String, Object> result = new HashMap<>();

        if (userId == null) {
            result.put("tags", Collections.emptyList());
            result.put("summary", "");
            return R.success(result);
        }

        try {
            // 获取画像摘要文本
            String summary = userProfileService.buildProfileSummary(userId);

            // 提取标签（从摘要中解析）
            List<String> tags = new ArrayList<>();
            if (summary != null) {
                String[] lines = summary.split("\n");
                for (String line : lines) {
                    if (line.startsWith("口味偏好：")) {
                        tags.addAll(Arrays.asList(line.substring(5).split("[,，]")));
                    } else if (line.startsWith("喜欢品类：")) {
                        tags.addAll(Arrays.asList(line.substring(5).split("[,，]")));
                    } else if (line.startsWith("常点菜品：")) {
                        tags.addAll(Arrays.asList(line.substring(5).split("[,，]")));
                    }
                }
            }

            result.put("tags", tags.stream().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
            result.put("summary", summary != null ? summary : "");
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，避免单个失败影响主流程
            log.warn("获取用户画像失败: userId={}", userId, e);
            result.put("tags", Collections.emptyList());
            result.put("summary", "");
        }

        return R.success(result);
    }

    // ==================== 对话管理增强 ====================

    /**
     * 搜索对话（按标题关键词）
     * @param keyword    搜索关键词
     * @param page       页码
     * @param pageSize   每页数量
     * @return 匹配的对话列表
     */
    @GetMapping("/conversations/search")
    @Operation(summary = "搜索对话", description = "按标题关键词搜索对话")
    @Parameter(description = "搜索关键词")
    public R<List<AIConversation>> searchConversations(@RequestParam String keyword,
                                                       @Parameter(description = "页码")
                                                       @RequestParam(defaultValue = "1") @Min(1) int page,
                                                       @Parameter(description = "每页条数")
                                                       @RequestParam(defaultValue =
                                                               "20") @Min(1) @Max(100) int pageSize,
                                                       HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        String actorType = resolveActorType(httpRequest);
        // 修改点(2026-09-20)：身份双校验，员工与 C 端用户搜索结果隔离
        return R.success(aiChatService.searchConversations(userId, actorType, keyword, page,
                PageUtils.cap(pageSize)));
    }

    /**
     * 重置对话上下文（清除缓存，保留历史记录）
     * @param conversationId 对话ID
     * @return 操作结果
     */
    @PostMapping("/conversations/{conversationId}/reset")
    @Operation(summary = "重置对话上下文", description = "清除对话的上下文缓存，保留历史消息记录")
    @Parameter(description = "对话ID")
    public R<String> resetConversationContext(@PathVariable String conversationId,
                                              HttpServletRequest httpRequest) {
        Long userId = BaseContext.getCurrentId();
        String actorType = resolveActorType(httpRequest);
        // 验证所有权（身份双校验，统一走 Service 层）
        if (userId == null
                || aiChatService.validateConversationOwnership(conversationId, userId, actorType) == null) {
            return R.error("对话不存在或无权访问");
        }
        aiChatService.resetContext(conversationId);
        return R.success("上下文已重置，历史消息已保留");
    }

    /**
     * 获取对话上下文统计信息
     * @param conversationId 对话ID
     * @return 统计信息
     */
    @GetMapping("/conversations/{conversationId}/context-stats")
    @Operation(summary = "上下文统计", description = "获取对话的上下文使用情况统计")
    @Parameter(description = "对话ID")
    public R<Map<String, Object>> getContextStats(@PathVariable String conversationId) {
        Map<String, Object> stats = aiChatService.getContextStats(conversationId);
        return R.success(stats);
    }

    // ==================== AI 服务状态 ====================

    /**
     * 获取 AI 服务运行状态（含熔断器信息）
     */
    @GetMapping("/status")
    @Operation(summary = "AI服务状态", description = "返回当前供应商、熔断器状态等")
    public R<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        AiProviderConfig activeConfig = aiProviderManager.getActiveConfig();
        if (activeConfig != null) {
            status.put("provider", activeConfig.getProviderName());
            status.put("model", activeConfig.getModelName());
            status.put("format", activeConfig.getApiFormat());
        } else {
            status.put("provider", "未配置");
            status.put("model", "N/A");
        }
        status.put("circuitBreaker", aiProviderManager.getCircuitBreakerStats());
        return R.success(status);
    }

    // ==================== 场景前端配置（P1 静态，P3 起由提示词模板服务下发） ====================

    /** 后台员工可选场景 */
    private static final List<String> EMPLOYEE_SCENES = Arrays.asList(
            "business_analysis", "dish_desc", "marketing");

    /** C 端用户唯一场景 */
    private static final String CUSTOMER_SCENE = "order_assistant";

    /** 各场景面向用户的开场白（注意：不是 system prompt，system prompt 永不下发） */
    private static final Map<String, String> SCENE_WELCOME = new HashMap<>();

    /** 各场景快捷问题 */
    private static final Map<String, List<String>> SCENE_QUICK_QUESTIONS = new HashMap<>();

    static {
        SCENE_WELCOME.put("business_analysis",
                "你好，我是你的经营分析助手，可以帮你分析营业额、热销菜品、客流时段等经营问题。");
        SCENE_WELCOME.put("dish_desc",
                "你好，告诉我菜品名称和主要食材，我可以帮你生成诱人的菜品描述。");
        SCENE_WELCOME.put("marketing",
                "你好，描述你的活动内容和目标客群，我来帮你生成适合外卖平台推送的营销文案。");
        SCENE_WELCOME.put("order_assistant",
                "你好呀！我是点餐小助手，可以帮你推荐菜品、介绍口味和份量，有什么想吃的尽管问我～");

        SCENE_QUICK_QUESTIONS.put("business_analysis", Arrays.asList(
                "最近7天的营业额趋势怎么样？",
                "热销菜品 Top10 是哪些？",
                "午市和晚市的销售占比如何？",
                "顾客的复购情况怎么样？"));
        SCENE_QUICK_QUESTIONS.put("dish_desc", Arrays.asList(
                "帮我写一份宫保鸡丁的菜品描述",
                "生成一段麻辣香锅的外卖介绍",
                "鱼香肉丝怎么描述更吸引人？"));
        SCENE_QUICK_QUESTIONS.put("marketing", Arrays.asList(
                "写一条周末满减活动的推送文案",
                "新客首单立减活动怎么宣传？",
                "帮我写会员日充值活动文案"));
        SCENE_QUICK_QUESTIONS.put("order_assistant", Arrays.asList(
                "今天有什么好吃的推荐？",
                "3个人吃饭点什么比较合适？",
                "有什么不辣的菜吗？",
                "店里的人气招牌菜有哪些？"));
    }

    /**
     * 获取场景前端配置（欢迎语、快捷问题、能力开关）。
     * <p>P1 为静态配置，P3 提示词模板入库后改由模板服务下发。</p>
     * <p><b>安全约束：只下发面向用户的开场白与快捷问题，永不下发 system prompt。</b></p>
     */
    @GetMapping("/scene-config")
    @Operation(summary = "场景配置", description = "返回欢迎语、快捷问题与能力开关（不含系统提示词）")
    public R<Map<String, Object>> getSceneConfig(
            @Parameter(description = "场景标识") @RequestParam(required = false) String scene,
            HttpServletRequest httpRequest) {
        String actorType = resolveActorType(httpRequest);
        boolean employee = AiChatConstants.ACTOR_EMPLOYEE.equals(actorType);
        // 非员工只允许点餐场景；员工只允许三个后台场景；非法/缺省回落到身份默认场景
        String resolvedScene;
        if (employee) {
            resolvedScene = EMPLOYEE_SCENES.contains(scene) ? scene : "business_analysis";
        } else {
            resolvedScene = CUSTOMER_SCENE.equals(scene) ? scene : CUSTOMER_SCENE;
        }

        // P2：vision 按供应商配置 capabilities 下发；tools 链路 P4 才完成，暂恒 false
        Map<String, Boolean> providerCaps = aiProviderManager.getCapabilities();
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("chat", true);
        capabilities.put("vision", Boolean.TRUE.equals(providerCaps.get("vision")));
        capabilities.put("tools", false);

        Map<String, Object> data = new HashMap<>();
        data.put("scene", resolvedScene);
        data.put("welcome", SCENE_WELCOME.get(resolvedScene));
        data.put("quickQuestions", SCENE_QUICK_QUESTIONS.get(resolvedScene));
        data.put("capabilities", capabilities);
        if (employee) {
            // 后台页场景切换条需要全量场景清单
            List<Map<String, String>> scenes = new ArrayList<>();
            scenes.add(buildSceneItem("business_analysis", "经营分析"));
            scenes.add(buildSceneItem("dish_desc", "菜品描述"));
            scenes.add(buildSceneItem("marketing", "营销文案"));
            data.put("scenes", scenes);
        }
        return R.success(data);
    }

    private Map<String, String> buildSceneItem(String value, String label) {
        Map<String, String> item = new HashMap<>();
        item.put("value", value);
        item.put("label", label);
        return item;
    }

}





