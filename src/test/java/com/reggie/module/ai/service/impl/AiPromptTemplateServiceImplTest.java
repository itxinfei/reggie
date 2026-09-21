package com.reggie.module.ai.service.impl;

import com.reggie.common.CustomException;
import com.reggie.module.ai.constant.AiPromptDefaults;
import com.reggie.module.ai.mapper.AiPromptTemplateMapper;
import com.reggie.module.ai.model.AiPromptTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AiPromptTemplateServiceImpl} 单元测试：自定义模板创建校验（场景/类型/长度/重复）、
 * QUICK 多格式解析与条数上限、内置更新锁字段、内置禁删与重置默认、Seeder 补插去重。
 *
 * 说明：service 使用 Mockito {@link spy} 包装，{@code saveBatch}/{@code removeById}
 * 在 MP 3.5.x 中依赖 TableInfoHelper 的 TableInfo 初始化（无 Spring/MyBatis 上下文时
 * getTableInfo 返回 null 会抛 NPE），故对这两个批量方法打桩并捕获入参校验；
 * 其余方法均走真实实现 + mock baseMapper。
 *
 * @author reggie
 * @since 2026-09-21
 */
class AiPromptTemplateServiceImplTest {

    private AiPromptTemplateServiceImpl service;
    private AiPromptTemplateMapper mapper;

    @BeforeEach
    void setUp() {
        service = spy(new AiPromptTemplateServiceImpl());
        mapper = mock(AiPromptTemplateMapper.class);
        when(mapper.insert(any(AiPromptTemplate.class))).thenAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock inv) {
                inv.<AiPromptTemplate>getArgument(0).setId(500L);
                return 1;
            }
        });
        when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.updateById(any(AiPromptTemplate.class))).thenReturn(1);
        when(mapper.deleteById(any(java.io.Serializable.class))).thenReturn(1);
        when(mapper.selectList(any())).thenReturn(new ArrayList<AiPromptTemplate>());
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
    }

    // ==================== 创建 ====================

    @Test
    void createSystemTemplateFillsDefaults() {
        AiPromptTemplate t = template("order_assistant", "SYSTEM", "标题", "系统内容", null);
        Long id = service.createTemplate(t);

        assertEquals(Long.valueOf(500L), id);
        AiPromptTemplate saved = captureInserted();
        assertEquals("system_order_assistant", saved.getCode());
        assertEquals(false, saved.getBuiltin());
        assertEquals(Integer.valueOf(100), saved.getSort());
        assertEquals(true, saved.getEnabled());
        assertEquals(Integer.valueOf(1), saved.getVersion());
        assertEquals("系统内容", saved.getContent());
        assertNull(saved.getQuickQuestions());
    }

    @Test
    void createDuplicateSceneTypeRejected() {
        when(mapper.selectCount(any())).thenReturn(1L);
        CustomException ex = assertThrows(CustomException.class,
                () -> service.createTemplate(template("marketing", "SYSTEM", "t", "c", null)));
        assertTrue(ex.getMessage().contains("已存在"));
        verify(mapper, never()).insert(any(AiPromptTemplate.class));
    }

    @Test
    void createInvalidSceneTypeOrTitleRejected() {
        AiPromptTemplate badScene = template("nope", "SYSTEM", "t", "c", null);
        assertThrows(CustomException.class, () -> service.createTemplate(badScene));

        AiPromptTemplate badType = template("marketing", "WAT", "t", "c", null);
        assertThrows(CustomException.class, () -> service.createTemplate(badType));

        AiPromptTemplate noTitle = template("marketing", "SYSTEM", "  ", "c", null);
        assertThrows(CustomException.class, () -> service.createTemplate(noTitle));
    }

    @Test
    void createSystemEmptyOrOversizedContentRejected() {
        assertThrows(CustomException.class,
                () -> service.createTemplate(template("marketing", "SYSTEM", "t", "  ", null)));
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 4001; i++) {
            huge.append('a');
        }
        assertThrows(CustomException.class,
                () -> service.createTemplate(template("marketing", "SYSTEM", "t", huge.toString(), null)));
    }

    @Test
    void createQuickFromMultiLineDedupesAndTrims() {
        AiPromptTemplate t = template("order_assistant", "QUICK", "标题", null,
                "问题1\n\n问题2\n问题1\n 问题3 ");
        service.createTemplate(t);

        AiPromptTemplate saved = captureInserted();
        // 去空条/去重/trim 后回写标准 JSON
        assertEquals("[\"问题1\",\"问题2\",\"问题3\"]", saved.getQuickQuestions());
        assertNull(saved.getContent());
    }

    @Test
    void createQuickFromJsonArrayAccepted() {
        AiPromptTemplate t = template("order_assistant", "QUICK", "标题", null,
                "[\"问题A\",\"问题B\"]");
        service.createTemplate(t);
        assertEquals("[\"问题A\",\"问题B\"]", captureInserted().getQuickQuestions());
    }

    @Test
    void createQuickEmptyRejected() {
        assertThrows(CustomException.class,
                () -> service.createTemplate(template("order_assistant", "QUICK", "t", null, "  ")));
    }

    @Test
    void createQuickTooManyRejected() {
        StringBuilder q = new StringBuilder();
        for (int i = 1; i <= 9; i++) {
            q.append("问题").append(i).append('\n');
        }
        assertThrows(CustomException.class,
                () -> service.createTemplate(template("order_assistant", "QUICK", "t", null, q.toString())));
    }

    @Test
    void createQuickSingleTooLongRejected() {
        StringBuilder q = new StringBuilder();
        for (int i = 0; i < 51; i++) {
            q.append('长');
        }
        assertThrows(CustomException.class,
                () -> service.createTemplate(template("order_assistant", "QUICK", "t", null, q.toString())));
    }

    @Test
    void createQuickBadJsonRejected() {
        assertThrows(CustomException.class,
                () -> service.createTemplate(template("order_assistant", "QUICK", "t", null, "[bad")));
    }

    // ==================== 更新 ====================

    @Test
    void updateBuiltinLocksSceneTypeAndCode() {
        AiPromptTemplate existing = builtin(10L, "system_order_assistant",
                "order_assistant", "SYSTEM");
        when(mapper.selectById(10L)).thenReturn(existing);

        AiPromptTemplate input = template("marketing", "WELCOME", "新标题", "新内容", null);
        input.setId(10L);
        input.setCode("hacker_code");
        service.updateTemplate(input);

        AiPromptTemplate updated = captureUpdated();
        assertEquals("system_order_assistant", updated.getCode());
        assertEquals("order_assistant", updated.getScene());
        assertEquals("SYSTEM", updated.getType());
        assertEquals("新标题", updated.getTitle());
        assertEquals("新内容", updated.getContent());
        assertEquals(true, updated.getBuiltin());
    }

    @Test
    void updateMissingOrWithoutIdRejected() {
        AiPromptTemplate noId = template("marketing", "SYSTEM", "t", "c", null);
        assertThrows(CustomException.class, () -> service.updateTemplate(noId));

        AiPromptTemplate ghost = template("marketing", "SYSTEM", "t", "c", null);
        ghost.setId(404L);
        assertThrows(CustomException.class, () -> service.updateTemplate(ghost));
    }

    @Test
    void updateCustomDuplicateRejected() {
        AiPromptTemplate existing = template("marketing", "SYSTEM", "旧", "旧内容", null);
        existing.setId(10L);
        existing.setBuiltin(false);
        when(mapper.selectById(10L)).thenReturn(existing);
        when(mapper.selectCount(any())).thenReturn(1L);

        AiPromptTemplate input = template("marketing", "SYSTEM", "新", "新内容", null);
        input.setId(10L);
        assertThrows(CustomException.class, () -> service.updateTemplate(input));
    }

    // ==================== 删除 / 重置 ====================

    @Test
    void deleteBuiltinRejectedCustomAllowed() {
        // MP 3.5.x 的 removeById 依赖 TableInfo 初始化，spy 打桩绕开
        doReturn(true).when(service).removeById(any(java.io.Serializable.class));

        AiPromptTemplate builtin = builtin(1L, "system_marketing", "marketing", "SYSTEM");
        when(mapper.selectById(1L)).thenReturn(builtin);
        assertThrows(CustomException.class, () -> service.deleteTemplate(1L));
        verify(mapper, never()).deleteById(any(java.io.Serializable.class));

        AiPromptTemplate custom = template("marketing", "WELCOME", "t", "c", null);
        custom.setId(2L);
        custom.setBuiltin(false);
        when(mapper.selectById(2L)).thenReturn(custom);
        service.deleteTemplate(2L);
        verify(service).removeById(eqSafe(2L));

        assertThrows(CustomException.class, () -> service.deleteTemplate(999L));
    }

    @Test
    void resetBuiltinRestoresDefaultContent() {
        AiPromptTemplate builtin = builtin(1L, "system_order_assistant",
                "order_assistant", "SYSTEM");
        builtin.setTitle("被改过");
        builtin.setContent("被改过的内容");
        when(mapper.selectById(1L)).thenReturn(builtin);

        service.resetBuiltin(1L);

        AiPromptDefaults.TemplateDefault d = AiPromptDefaults.get("system_order_assistant");
        AiPromptTemplate updated = captureUpdated();
        assertEquals(d.getTitle(), updated.getTitle());
        assertEquals(d.getContent(), updated.getContent());
        assertEquals(true, updated.getEnabled());
    }

    @Test
    void resetNonBuiltinOrMissingRejected() {
        AiPromptTemplate custom = template("marketing", "SYSTEM", "t", "c", null);
        custom.setId(2L);
        custom.setBuiltin(false);
        when(mapper.selectById(2L)).thenReturn(custom);
        assertThrows(CustomException.class, () -> service.resetBuiltin(2L));
        assertThrows(CustomException.class, () -> service.resetBuiltin(404L));
    }

    // ==================== 读取 / Seeder ====================

    @Test
    void getQuickQuestionsParsesOrReturnsEmpty() {
        AiPromptTemplate t = builtin(1L, "quick_order_assistant",
                "order_assistant", "QUICK");
        t.setQuickQuestions("[\"问题1\",\"问题2\"]");
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(t));
        assertEquals(Arrays.asList("问题1", "问题2"), service.getQuickQuestions("order_assistant"));

        t.setQuickQuestions("坏 JSON");
        assertTrue(service.getQuickQuestions("order_assistant").isEmpty());

        when(mapper.selectList(any())).thenReturn(new ArrayList<AiPromptTemplate>());
        assertTrue(service.getQuickQuestions("order_assistant").isEmpty());
    }

    @Test
    void getSystemPromptAndWelcomeFromEffectiveTemplate() {
        AiPromptTemplate sys = builtin(1L, "system_marketing", "marketing", "SYSTEM");
        sys.setContent("系统提示正文");
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(sys));
        assertEquals("系统提示正文", service.getSystemPrompt("marketing"));

        AiPromptTemplate welcome = builtin(2L, "welcome_marketing", "marketing", "WELCOME");
        welcome.setContent("欢迎正文");
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(welcome));
        assertEquals("欢迎正文", service.getWelcome("marketing"));
    }

    @Test
    void invalidSceneGettersReturnNullWithoutDb() {
        assertNull(service.getSystemPrompt("nope"));
        assertNull(service.getWelcome(null));
        assertTrue(service.getQuickQuestions(" ").isEmpty());
        verify(mapper, never()).selectList(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void seedOnEmptyDatabaseInsertsTwelveBuiltins() {
        doReturn(true).when(service).saveBatch(anyList());
        service.seedBuiltinsIfMissing();

        org.mockito.ArgumentCaptor<List<AiPromptTemplate>> cap =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(service).saveBatch(cap.capture());
        List<AiPromptTemplate> batch = cap.getValue();
        assertEquals(12, batch.size());
        // 抽查首条（static 块首条 put 的是 order_assistant 的 SYSTEM）：
        // code 与「场景序×10+类型序」sort 就位：order_assistant 场景序 3 → 4×10+1=41
        AiPromptTemplate first = batch.get(0);
        assertEquals("system_order_assistant", first.getCode());
        assertEquals(true, first.getBuiltin());
        assertEquals(Integer.valueOf(1), first.getVersion());
        assertEquals(Integer.valueOf(41), first.getSort());
    }

    @Test
    @SuppressWarnings("unchecked")
    void seedSkipsExistingCodes() {
        AiPromptTemplate existing = builtin(1L, "system_business_analysis",
                "business_analysis", "SYSTEM");
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(existing));
        doReturn(true).when(service).saveBatch(anyList());

        service.seedBuiltinsIfMissing();

        org.mockito.ArgumentCaptor<List<AiPromptTemplate>> cap =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(service).saveBatch(cap.capture());
        List<AiPromptTemplate> batch = cap.getValue();
        assertEquals(11, batch.size());
        for (AiPromptTemplate t : batch) {
            assertNotEquals("system_business_analysis", t.getCode());
            assertEquals(true, t.getBuiltin());
        }
    }

    // ==================== 辅助 ====================

    private AiPromptTemplate captureInserted() {
        org.mockito.ArgumentCaptor<AiPromptTemplate> cap =
                org.mockito.ArgumentCaptor.forClass(AiPromptTemplate.class);
        verify(mapper, org.mockito.Mockito.atLeastOnce()).insert(cap.capture());
        return cap.getValue();
    }

    private AiPromptTemplate captureUpdated() {
        org.mockito.ArgumentCaptor<AiPromptTemplate> cap =
                org.mockito.ArgumentCaptor.forClass(AiPromptTemplate.class);
        verify(mapper).updateById(cap.capture());
        return cap.getValue();
    }

    private static java.io.Serializable eqSafe(long id) {
        return org.mockito.ArgumentMatchers.eq(id);
    }

    private AiPromptTemplate template(String scene, String type, String title,
                                      String content, String quickQuestions) {
        AiPromptTemplate t = new AiPromptTemplate();
        t.setScene(scene);
        t.setType(type);
        t.setTitle(title);
        t.setContent(content);
        t.setQuickQuestions(quickQuestions);
        return t;
    }

    private AiPromptTemplate builtin(Long id, String code, String scene, String type) {
        AiPromptTemplate t = template(scene, type, "内置", "内置内容", null);
        t.setId(id);
        t.setCode(code);
        t.setBuiltin(true);
        t.setEnabled(true);
        t.setVersion(7);
        t.setSort(1);
        return t;
    }
}
