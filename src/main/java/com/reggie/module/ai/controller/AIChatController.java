package com.reggie.module.ai.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.ai.model.AIChatRequest;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIConversation;
import com.reggie.module.ai.model.AIMessageRecord;
import com.reggie.module.ai.service.AIChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI聊天控制器
 * 提供智能点餐、菜品描述生成、经营分析等AI能力
 * 新增：流式响应(SSE)、对话管理、反馈记录
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
     */
    @PostMapping("/chat")
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
     */
    @GetMapping("/chat/stream")
    @Operation(summary = "AI流式对话", description = "SSE流式输出，逐字显示AI回复")
    public SseEmitter chatStream(@RequestParam String message, @RequestParam(required = false) String scene,
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
     */
    @PostMapping("/order-assistant")
    @Operation(summary = "智能点餐助手", description = "用户用自然语言描述需求，AI推荐最合适的菜品")
    public R<AIChatResponse> orderAssistant(@RequestBody Map<String, Object> params) {
        String message = (String) params.getOrDefault("message", "");
        Long userId = params.containsKey("userId") ? Long.valueOf(params.get("userId").toString()) : null;
        String conversationId = params.containsKey("conversationId") ? (String) params.get("conversationId") : null;

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
     */
    @GetMapping("/order-assistant/stream")
    @Operation(summary = "智能点餐助手（流式）", description = "SSE流式输出推荐结果")
    public SseEmitter orderAssistantStream(@RequestParam String message,
                                            @RequestParam(required = false) String conversationId) {
        Long userId = BaseContext.getCurrentId();
        log.info("智能点餐流式: userId={}, messageLength={}", userId, message.length());
        return aiChatService.orderAssistantStream(message, userId, conversationId);
    }

    // ==================== 辅助功能 ====================

    /**
     * 生成菜品描述
     */
    @PostMapping("/dish-description")
    @Operation(summary = "AI菜品描述生成", description = "输入菜品名称，AI生成专业美食描述文案")
    public R<String> generateDishDescription(@RequestBody Map<String, String> params) {
        String dishName = params.get("dishName");
        String categoryName = params.getOrDefault("categoryName", "");
        String ingredients = params.getOrDefault("ingredients", "");
        log.info("生成菜品描述: dishName={}", dishName);
        String description = aiChatService.generateDishDescription(dishName, categoryName, ingredients);
        return R.success(description);
    }

    /**
     * 经营数据分析
     */
    @PostMapping("/business-analysis")
    @Operation(summary = "AI经营分析", description = "输入经营数据和问题，AI提供专业分析")
    public R<String> analyzeBusiness(@RequestBody Map<String, String> params) {
        String question = params.get("question");
        String dataJson = params.getOrDefault("data", "{}");
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
     */
    @GetMapping("/conversations")
    @Operation(summary = "获取对话列表", description = "获取当前用户的AI对话历史列表")
    public R<List<AIConversation>> getConversations(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = BaseContext.getCurrentId();
        List<AIConversation> conversations = aiChatService.getUserConversations(userId, page, pageSize);
        return R.success(conversations);
    }

    /**
     * 获取对话详情（含消息历史）
     */
    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "获取对话详情", description = "获取指定对话的消息历史")
    public R<List<AIMessageRecord>> getConversationDetail(@PathVariable String conversationId) {
        List<AIMessageRecord> messages = aiChatService.getConversationMessages(conversationId);
        return R.success(messages);
    }

    /**
     * 创建新对话
     */
    @PostMapping("/conversations")
    @Operation(summary = "创建对话", description = "创建新的AI对话")
    public R<AIConversation> createConversation(@RequestBody(required = false) Map<String, String> params) {
        Long userId = BaseContext.getCurrentId();
        String title = params != null ? params.get("title") : null;
        String scene = params != null ? params.get("scene") : "order_assistant";
        AIConversation conv = aiChatService.createConversation(userId, title, scene);
        return R.success(conv);
    }

    /**
     * 删除对话
     */
    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "删除对话", description = "软删除指定对话")
    public R<String> deleteConversation(@PathVariable String conversationId) {
        Long userId = BaseContext.getCurrentId();
        aiChatService.deleteConversation(conversationId, userId);
        return R.success("删除成功");
    }

    // ==================== 反馈记录 ====================

    /**
     * 记录用户反馈
     */
    @PostMapping("/feedback")
    @Operation(summary = "记录反馈", description = "用户对AI回复的反馈（有用/没用）")
    public R<String> recordFeedback(@RequestBody Map<String, Object> params) {
        Long userId = BaseContext.getCurrentId();
        Long messageId = params.containsKey("messageId") ? Long.valueOf(params.get("messageId").toString()) : null;
        String feedbackType = (String) params.get("feedbackType");
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
}
