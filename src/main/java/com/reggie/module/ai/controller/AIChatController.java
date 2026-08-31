package com.reggie.module.ai.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.RateLimitType;
import com.reggie.module.ai.mapper.AIConversationMapper;import com.reggie.module.ai.model.AIChatRequest;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIConversation;
import com.reggie.module.ai.model.AIMessage;
import com.reggie.module.ai.model.AIMessageRecord;
import com.reggie.module.ai.model.AiProviderConfig;
import com.reggie.module.ai.dto.BusinessAnalysisRequest;
import com.reggie.module.ai.dto.CreateConversationRequest;
import com.reggie.module.ai.dto.DishDescriptionRequest;
import com.reggie.module.ai.dto.OrderAssistantRequest;
import com.reggie.module.ai.dto.RecordFeedbackRequest;
import com.reggie.module.ai.service.AIChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
@RequestMapping("/api/ai")
@Tag(name = "AI智能助手", description = "AI智能点餐推荐、菜品描述生成、经营分析、流式对话")
public class AIChatController {

    @Resource
    private AIChatService aiChatService;

    @Resource
    private com.reggie.module.ai.service.UserProfileService userProfileService;

    @Resource
    private com.reggie.module.ai.provider.AiProviderManager aiProviderManager;

    // ==================== 核心对话接口 ====================

    /**
     * 通用AI对话接口（非流式）
     * @param request AI对话请求参数
     * @return AI回复结果
     */
    @PostMapping("/chat")
    @RateLimit(maxRequestsPerSecond = 2, type = RateLimitType.USER)
    @Operation(summary = "通用AI对话", description = "支持多场景：点餐推荐、菜品描述、经营分析、营销文案")
    public R<AIChatResponse> chat(@Valid @RequestBody AIChatRequest request) {
        Long userId = BaseContext.getCurrentId();
        if (userId != null) request.setUserId(userId);
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
    @Parameter(description = "Message")
    public SseEmitter chatStream(@RequestParam String message, @RequestParam(required = false) String scene,
                                  @Parameter(description = "conversationId")
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
     * 智能点餐推荐（简化接口，非流式）
     * @param params 请求参数（message、conversationId）
     * @return AI推荐结果
     */
    @PostMapping("/order-assistant")
    @RateLimit(maxRequestsPerSecond = 2, type = RateLimitType.USER)
    @Operation(summary = "智能点餐助手", description = "用户用自然语言描述需求，AI推荐最合适的菜品")
    public R<AIChatResponse> orderAssistant(@Valid @RequestBody OrderAssistantRequest params) {
        String message = params.getMessage();
        // #10 安全修复：删除客户端 userId 入参，统一从登录上下文获取，防止越权 IDOR
        Long userId = BaseContext.getCurrentId();
        String conversationId = params.getConversationId();

        log.info("智能点餐请求: userId={}, messageLength={}", userId, message.length());

        if (conversationId == null || conversationId.isEmpty()) {
            AIConversation conv = aiChatService.createConversation(userId, null, "order_assistant");
            conversationId = conv.getConversationId();
        }

        // 修改点：已在Controller层统一创建对话，Service层复用此conversationId避免重复创建
        AIChatResponse response = aiChatService.orderAssistant(message, userId, conversationId);
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
    @Parameter(description = "Message")
    public SseEmitter orderAssistantStream(@RequestParam String message,
                                            @Parameter(description = "conversationId")
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
    public R<String> generateDishDescription(@Valid @RequestBody DishDescriptionRequest params) {
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
    public R<String> analyzeBusiness(@Valid @RequestBody BusinessAnalysisRequest params) {
        String question = params.getQuestion();
        String dataJson = params.getData();
        if (dataJson == null) dataJson = "{}";
        log.info("经营分析请求: question={}", question);
        String analysis = aiChatService.analyzeBusiness(question, dataJson);
        return R.success(analysis);
    }

    /**
     * AI服务健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "AI服务健康检查", description = "检查AI服务是否可用")
    public R<Map<String, Object>> health() {
        AIChatResponse testResponse = aiProviderManager.chat(
                java.util.Arrays.asList(
                        com.reggie.module.ai.model.AIMessage.builder().role("user").content("ping").build()
                ), 50, 0.1);
        boolean available = testResponse != null && testResponse.getContent() != null
                && !testResponse.getContent().contains("未配置")
                && !testResponse.getContent().contains("未就绪")
                && !testResponse.getContent().contains("不可用")
                && !testResponse.getContent().contains("失败");
        Map<String, Object> result = new HashMap<>();
        result.put("available", available);
        result.put("model", testResponse != null ? testResponse.getModel() : "unknown");
        result.put("features", Arrays.asList("streaming", "conversation", "feedback", "order_assistant", "business_analysis"));
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
    @Parameter(description = "Page")
    public R<List<AIConversation>> getConversations(@RequestParam(defaultValue = "1") @Min(1) int page,
                                                     @Parameter(description = "Page size")
                                                     @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        Long userId = BaseContext.getCurrentId();
        List<AIConversation> conversations = aiChatService.getUserConversations(userId, page, PageUtils.cap(pageSize));
        return R.success(conversations);
    }

    /**
     * 获取对话详情（含消息历史）
     * @param conversationId 对话ID
     * @return 消息历史列表
     */
    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "获取对话详情", description = "获取指定对话的消息历史")
    @Parameter(description = "ConversationId")
    public R<List<AIMessageRecord>> getConversationDetail(@PathVariable String conversationId) {
        // 修复 P2-9：校验 conversationId 属于当前用户，防止 IDOR 越权
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<AIConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.select(AIConversation::getUserId)
                .eq(AIConversation::getConversationId, conversationId)
                .eq(AIConversation::getIsDeleted, 0);
        AIConversation conv = conversationMapper.selectOne(convWrapper);
        if (conv == null || !userId.equals(conv.getUserId())) {
            return R.error("对话不存在或无权访问");
        }
        List<AIMessageRecord> messages = aiChatService.getConversationMessages(conversationId);
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
    public R<AIConversation> createConversation(@RequestBody(required = false) CreateConversationRequest params) {
        Long userId = BaseContext.getCurrentId();
        String title = params != null ? params.getTitle() : null;
        String scene = params != null && params.getScene() != null ? params.getScene() : "order_assistant";
        AIConversation conv = aiChatService.createConversation(userId, title, scene);
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
    @Parameter(description = "ConversationId")
    public R<String> deleteConversation(@PathVariable String conversationId) {
        Long userId = BaseContext.getCurrentId();
        aiChatService.deleteConversation(conversationId, userId);
        return R.success("删除成功");
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
    public R<String> recordFeedback(@Valid @RequestBody RecordFeedbackRequest params) {
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
    @Parameter(description = "Keyword")
    public R<List<AIConversation>> searchConversations(@RequestParam String keyword,
                                                       @Parameter(description = "Page number")
                                                       @RequestParam(defaultValue = "1") @Min(1) int page,
                                                       @Parameter(description = "Page size")
                                                       @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<AIConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AIConversation::getUserId, userId)
                .eq(AIConversation::getIsDeleted, 0)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(AIConversation::getTitle, keyword)
                        .or()
                        .like(AIConversation::getScene, keyword))
                .orderByDesc(AIConversation::getUpdateTime);
        Page<AIConversation> pageObj = PageUtils.of(page, pageSize);
        conversationMapper.selectPage(pageObj, wrapper);
        return R.success(pageObj.getRecords());
    }

    /**
     * 重置对话上下文（清除缓存，保留历史记录）
     * @param conversationId 对话ID
     * @return 操作结果
     */
    @PostMapping("/conversations/{conversationId}/reset")
    @Operation(summary = "重置对话上下文", description = "清除对话的上下文缓存，保留历史消息记录")
    @Parameter(description = "ConversationId")
    public R<String> resetConversationContext(@PathVariable String conversationId) {
        Long userId = BaseContext.getCurrentId();
        // 验证所有权
        LambdaQueryWrapper<AIConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.select(AIConversation::getUserId)
                .eq(AIConversation::getConversationId, conversationId)
                .eq(AIConversation::getIsDeleted, 0);
        AIConversation conv = conversationMapper.selectOne(convWrapper);
        if (conv == null || !userId.equals(conv.getUserId())) {
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
    @Parameter(description = "ConversationId")
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

    // ==================== 依赖注入 ====================

    @Resource
    private AIConversationMapper conversationMapper;
}





