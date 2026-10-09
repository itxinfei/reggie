package com.reggie.module.ai.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.reggie.common.BaseContext;
import com.reggie.module.ai.mapper.AIMessageRecordMapper;
import com.reggie.module.ai.service.AiPromptTemplateService;
import com.reggie.module.dish.mapper.DishMapper;
import com.reggie.module.dish.model.Dish;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AiInputSuggestionServiceImpl} 单元测试：空输入返回场景快捷问题起步候选、
 * 前缀命中多源合并（quick > dish > mine > hot）、去重与总量封顶、
 * LIKE 通配符转义、单源查询异常不拖垮整体。
 *
 * @author reggie
 * @since 2026-09-30
 */
class AiInputSuggestionServiceImplTest {

    private AiInputSuggestionServiceImpl service;
    private AiPromptTemplateService promptTemplateService;
    private AIMessageRecordMapper messageRecordMapper;
    private DishMapper dishMapper;

    @BeforeEach
    void setUp() {
        BaseContext.setCurrentTenantId(1L);
        service = new AiInputSuggestionServiceImpl();
        promptTemplateService = mock(AiPromptTemplateService.class);
        messageRecordMapper = mock(AIMessageRecordMapper.class);
        dishMapper = mock(DishMapper.class);
        ReflectionTestUtils.setField(service, "promptTemplateService", promptTemplateService);
        ReflectionTestUtils.setField(service, "messageRecordMapper", messageRecordMapper);
        ReflectionTestUtils.setField(service, "dishMapper", dishMapper);
        // 无 Spring 上下文时 LambdaQueryWrapper 解析 Dish::getStatus 需要 lambda 缓存，手动初始化
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Dish.class);
    }

    @AfterEach
    void tearDown() {
        BaseContext.remove();
        reset(promptTemplateService, messageRecordMapper, dishMapper);
    }

    private Dish dish(String name) {
        Dish d = new Dish();
        d.setName(name);
        return d;
    }

    @Test
    void emptyQueryReturnsQuickQuestions() {
        when(promptTemplateService.getQuickQuestions("order_assistant")).thenReturn(
                Arrays.asList("今天有什么好吃的推荐？", "有什么不辣的菜吗？"));
        List<Map<String, Object>> result = service.suggest(9L, "CUSTOMER", "order_assistant", "  ");
        assertEquals(2, result.size());
        assertEquals("quick", result.get(0).get("source"));
        assertEquals("今天有什么好吃的推荐？", result.get(0).get("text"));
    }

    @Test
    void customerForcedToOrderAssistantScene() {
        when(promptTemplateService.getQuickQuestions("order_assistant")).thenReturn(Arrays.asList("问题A"));
        // 顾客即使传 business_analysis 也收敛为 order_assistant
        service.suggest(9L, "CUSTOMER", "business_analysis", "");
        verify(promptTemplateService).getQuickQuestions("order_assistant");
    }

    @Test
    void prefixMergesSourcesInPriorityOrder() {
        when(promptTemplateService.getQuickQuestions("order_assistant")).thenReturn(
                Arrays.asList("宫保鸡丁怎么做的？"));
        when(dishMapper.selectList(any())).thenReturn(Arrays.asList(dish("宫保鸡丁"), dish("宫保虾球")));
        when(messageRecordMapper.selectPersonalSuggestions(anyLong(), anyLong(), anyString(), anyInt()))
                .thenReturn(Arrays.asList("宫保鸡丁外卖大概多久到？"));
        when(messageRecordMapper.selectHotSuggestions(anyLong(), anyString(), any(), anyInt()))
                .thenReturn(Arrays.asList("宫保鸡丁辣不辣？"));

        List<Map<String, Object>> result = service.suggest(9L, "CUSTOMER", "order_assistant", "宫保");

        assertEquals(5, result.size());
        assertEquals("quick", result.get(0).get("source"));
        assertEquals("dish", result.get(1).get("source"));
        assertEquals("我想吃宫保鸡丁，帮我推荐", result.get(1).get("text"));
        assertEquals("mine", result.get(3).get("source"));
        assertEquals("hot", result.get(4).get("source"));
    }

    @Test
    void duplicatesAcrossSourcesAreDropped() {
        when(promptTemplateService.getQuickQuestions("order_assistant")).thenReturn(
                Arrays.asList("宫保鸡丁辣不辣？"));
        when(dishMapper.selectList(any())).thenReturn(Collections.<Dish>emptyList());
        when(messageRecordMapper.selectPersonalSuggestions(anyLong(), anyLong(), anyString(), anyInt()))
                .thenReturn(Arrays.asList("宫保鸡丁辣不辣？"));

        List<Map<String, Object>> result = service.suggest(9L, "CUSTOMER", "order_assistant", "宫保");
        assertEquals(1, result.size());
    }

    @Test
    void likeWildcardsInPrefixAreEscaped() {
        when(promptTemplateService.getQuickQuestions("order_assistant")).thenReturn(
                Collections.<String>emptyList());
        when(dishMapper.selectList(any())).thenReturn(Collections.<Dish>emptyList());

        service.suggest(9L, "CUSTOMER", "order_assistant", "100%优惠_打折\\吗");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(messageRecordMapper).selectPersonalSuggestions(eq(9L), eq(1L), captor.capture(), anyInt());
        assertEquals("100\\%优惠\\_打折\\\\吗", captor.getValue());
    }

    @Test
    void singleSourceFailureDoesNotBreakOthers() {
        when(promptTemplateService.getQuickQuestions("order_assistant")).thenReturn(
                Arrays.asList("推荐招牌菜"));
        when(dishMapper.selectList(any())).thenThrow(new RuntimeException("db down"));
        when(messageRecordMapper.selectPersonalSuggestions(anyLong(), anyLong(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("db down"));
        when(messageRecordMapper.selectHotSuggestions(anyLong(), anyString(), any(), anyInt()))
                .thenThrow(new RuntimeException("db down"));

        List<Map<String, Object>> result = service.suggest(9L, "CUSTOMER", "order_assistant", "推");
        assertEquals(1, result.size());
        assertEquals("quick", result.get(0).get("source"));
    }

    @Test
    void emptyQueryQuickQuestionsAreCappedAtFour() {
        when(promptTemplateService.getQuickQuestions("order_assistant")).thenReturn(Arrays.asList(
                "推荐招牌菜1", "推荐招牌菜2", "推荐招牌菜3", "推荐招牌菜4", "推荐招牌菜5", "推荐招牌菜6"));
        List<Map<String, Object>> result = service.suggest(9L, "CUSTOMER", "order_assistant", "");
        // 空输入的起步候选取前 4 条快捷问题
        assertEquals(4, result.size());
        assertFalse(result.isEmpty());
    }

    @Test
    void nullUserIdSkipsPersonalSourceOnly() {
        when(promptTemplateService.getQuickQuestions("business_analysis")).thenReturn(
                Arrays.asList("最近7天的营业额趋势怎么样？"));
        when(messageRecordMapper.selectHotSuggestions(anyLong(), anyString(), any(), anyInt()))
                .thenReturn(Arrays.asList("最近7天的营业额趋势怎么样？"));

        List<Map<String, Object>> result = service.suggest(null, "EMPLOYEE", "business_analysis", "最近");
        assertEquals(1, result.size());
        assertEquals("quick", result.get(0).get("source"));
    }
}
