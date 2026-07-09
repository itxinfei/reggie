package com.reggie.module.ai.controller;

import com.reggie.common.R;
import com.reggie.module.ai.model.AIChatRequest;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.service.AIChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * AI聊天控制器
 * 提供智能点餐、菜品描述生成、经营分析等AI能力
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@Tag(name = "AI智能助手", description = "AI智能点餐推荐、菜品描述生成、经营分析")
public class AIChatController {

    @Resource
    private AIChatService aiChatService;

    /**
     * 通用AI对话接口
     *
     * @param request 聊天请求
     * @return AI响应
     */
    @PostMapping("/chat")
    @Operation(summary = "通用AI对话", description = "支持多场景：点餐推荐、菜品描述、经营分析、营销文案")
    public R<AIChatResponse> chat(@RequestBody AIChatRequest request) {
        log.info("AI对话请求: scene={}, messageLength={}",
                request.getScene(),
                request.getMessage() != null ? request.getMessage().length() : 0);
        AIChatResponse response = aiChatService.chat(request);
        return R.success(response);
    }

    /**
     * 智能点餐推荐（简化接口）
     *
     * @param params 包含 message（用户输入）和 userId（可选，用于个性化推荐）
     * @return AI推荐响应
     */
    @PostMapping("/order-assistant")
    @Operation(summary = "智能点餐助手", description = "用户用自然语言描述需求，AI推荐最合适的菜品")
    public R<AIChatResponse> orderAssistant(@RequestBody Map<String, Object> params) {
        String message = (String) params.getOrDefault("message", "");
        Long userId = params.containsKey("userId") ? Long.valueOf(params.get("userId").toString()) : null;

        // 脱敏日志：只记录长度，不记录具体内容
        log.info("智能点餐请求: userId={}, messageLength={}", userId, message.length());
        AIChatResponse response = aiChatService.orderAssistant(message, userId);
        return R.success(response);
    }

    /**
     * 生成菜品描述
     *
     * @param params 包含 dishName、categoryName、ingredients
     * @return 生成的描述文本
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
     *
     * @param params 包含 question（用户问题）和 data（经营数据JSON）
     * @return AI分析结果
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
     *
     * @return 服务状态
     */
    @GetMapping("/health")
    @Operation(summary = "AI服务健康检查", description = "检查AI服务是否可用")
    public R<Map<String, Object>> health() {
        AIChatResponse testResponse = aiChatService.chat(
                AIChatRequest.builder()
                        .message("ping")
                        .scene("order_assistant")
                        .build()
        );
        boolean available = testResponse != null && testResponse.getContent() != null
                && !testResponse.getContent().contains("不可用");
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("available", available);
        result.put("model", testResponse != null ? testResponse.getModel() : "unknown");
        return R.success(result);
    }
}
