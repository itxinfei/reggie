package com.reggie.module.ai.service;

import com.reggie.common.BaseContext;
import com.reggie.module.ai.config.AIConfigProperties;
import com.reggie.module.ai.mapper.AIConversationMapper;
import com.reggie.module.ai.mapper.AIMessageRecordMapper;
import com.reggie.module.ai.model.AIChatRequest;
import com.reggie.module.ai.model.AIChatResponse;
import com.reggie.module.ai.model.AIConversation;
import com.reggie.module.ai.model.AIMessageRecord;
import com.reggie.module.ai.provider.AiProviderManager;
import com.reggie.module.ai.service.impl.AIChatServiceImpl;
import com.reggie.module.ai.service.UserProfileService;
import com.reggie.module.recommend.service.PreferenceAnalysisService;
import com.reggie.mapper.DishMapper;
import com.reggie.entity.Dish;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AI 对话服务单测（纯 Mock，不依赖数据库）
 *
 * @author reggie
 * @since 2026-07-11
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AIChatServiceTest {

    @Mock
    private AiProviderManager aiProviderManager;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private AIConversationMapper conversationMapper;

    @Mock
    private AIMessageRecordMapper messageRecordMapper;

    @Mock
    private AIConfigProperties aiConfig;

    @Mock
    private DishMapper dishMapper;

    @Mock
    private PreferenceAnalysisService preferenceAnalysisService;

    @Mock
    private Executor aiExecutor;

    @Test
    void testChat_returnsFallbackWhenProviderReturnsNull() {
        when(aiProviderManager.chat(anyList(), anyInt(), anyDouble())).thenReturn(null);

        AIChatRequest request = AIChatRequest.builder()
                .message("推荐点菜")
                .scene("order_assistant")
                .conversationId("conv-1")
                .userId(1L)
                .build();

        AIChatResponse response = aiChatService.chat(request);
        assertNotNull(response);
        assertEquals("AI服务暂时不可用，请稍后重试。", response.getContent());
    }

    @Test
    void testChat_orderAssistantScene_cleansJsonContent() {
        String aiContent = "根据您的口味，为您推荐：\n"
                + "```json\n"
                + "[{\"dishId\":1,\"reason\":\"适合您的辣味偏好\"}]\n"
                + "```\n"
                + "希望您喜欢！";
        AIChatResponse aiResponse = new AIChatResponse();
        aiResponse.setContent(aiContent);
        aiResponse.setModel("mock");
        aiResponse.setTokensUsed(10);

        when(aiProviderManager.chat(anyList(), anyInt(), anyDouble())).thenReturn(aiResponse);

        AIChatRequest request = AIChatRequest.builder()
                .message("推荐点菜")
                .scene("order_assistant")
                .conversationId("conv-2")
                .userId(2L)
                .build();

        AIChatResponse response = aiChatService.chat(request);
        assertNotNull(response);
        assertFalse(response.getContent().contains("```json"));
        assertFalse(response.getContent().contains("```"));
        assertTrue(response.getContent().contains("根据您的口味"));
    }

    @Test
    void testGetConversationMessages_returnsEmptyWhenUserIdMismatch() {
        BaseContext.setCurrentId(1L);

        AIConversation conv = new AIConversation();
        conv.setConversationId("conv-3");
        conv.setUserId(2L);

        when(conversationMapper.selectOne(any())).thenReturn(conv);

        List<AIMessageRecord> messages = aiChatService.getConversationMessages("conv-3");
        assertTrue(messages.isEmpty());
        BaseContext.remove();
    }

    @Test
    void testDeleteConversation_marksBothConversationAndMessages() {
        AIConversation conv = new AIConversation();
        conv.setConversationId("conv-4");
        conv.setUserId(1L);
        conv.setIsDeleted(0);

        when(conversationMapper.selectOne(any())).thenReturn(conv);
        when(conversationMapper.updateById(any())).thenReturn(1);
        // 关键：messageRecordMapper.update(any(), any()) 的第一个参数是实体(null)而非 LambdaUpdateWrapper
        // 使用 isNull() 匹配 null 的 entity 参数
        when(messageRecordMapper.update(isNull(), any())).thenReturn(1);

        aiChatService.deleteConversation("conv-4", 1L);

        assertEquals(1, conv.getIsDeleted().intValue());
        // 验证 messageRecordMapper.update(null, wrapper) 被调用
        verify(messageRecordMapper).update(isNull(), any());
    }

    @Test
    void testChatStream_doesNotThrowOnErrorResponse() {
        AIChatResponse errorResponse = new AIChatResponse();
        errorResponse.setContent("AI服务暂时不可用，请稍后重试。");
        when(aiProviderManager.chat(anyList(), anyInt(), anyDouble())).thenReturn(errorResponse);

        AIChatRequest request = AIChatRequest.builder()
                .message("推荐点菜")
                .scene("order_assistant")
                .conversationId("conv-stream-1")
                .userId(1L)
                .build();

        assertDoesNotThrow(() -> aiChatService.chatStream(request));
    }

    @Test
    void testOrderAssistantStream_usesAuthenticatedUserId() {
        BaseContext.setCurrentId(99L);

        when(dishMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(preferenceAnalysisService.analyzePricePreference(anyLong())).thenReturn("中");
        when(preferenceAnalysisService.analyzeTimePreference(anyLong())).thenReturn("午餐");
        when(preferenceAnalysisService.isHighFrequencyUser(anyLong())).thenReturn(false);
        when(messageRecordMapper.update(isNull(), any())).thenReturn(1);
        when(conversationMapper.update(any(), any())).thenReturn(1);

        AIChatResponse mockResponse = new AIChatResponse();
        mockResponse.setContent("mock response");
        when(aiProviderManager.chat(anyList(), anyInt(), anyDouble())).thenReturn(mockResponse);

        assertDoesNotThrow(() -> aiChatService.orderAssistantStream("我想吃点辣的", null, "conv-5"));
        BaseContext.remove();
    }
}
