package com.reggie.module.ai.controller;

import com.reggie.common.R;
import com.reggie.module.ai.provider.AiProviderManager;
import com.reggie.module.ai.service.AiPromptTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AIChatController#getSceneConfig} 单元测试。
 * 核心安全契约：scene-config 只面向终端用户下发开场白/快捷问题/能力开关，
 * <b>绝不读取或下发 system prompt</b>；并验证员工/顾客的场景回落规则与模板异常兜底。
 *
 * @author reggie
 * @since 2026-09-21
 */
class SceneConfigTest {

    private AIChatController controller;
    private AiProviderManager providerManager;
    private AiPromptTemplateService promptTemplateService;

    @BeforeEach
    void setUp() {
        controller = new AIChatController();
        providerManager = mock(AiProviderManager.class);
        promptTemplateService = mock(AiPromptTemplateService.class);

        Map<String, Boolean> caps = new HashMap<>();
        caps.put("vision", Boolean.TRUE);
        when(providerManager.getCapabilities()).thenReturn(caps);
        when(providerManager.supportsToolCalling()).thenReturn(true);

        when(promptTemplateService.getWelcome(anyString())).thenReturn("模板欢迎语");
        when(promptTemplateService.getQuickQuestions(anyString()))
                .thenReturn(Arrays.asList("问题一", "问题二"));

        ReflectionTestUtils.setField(controller, "aiProviderManager", providerManager);
        ReflectionTestUtils.setField(controller, "promptTemplateService", promptTemplateService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void employeeSceneConfigExposesOnlyUserFacingFields() {
        R<Map<String, Object>> r = controller.getSceneConfig("business_analysis", employeeRequest());
        Map<String, Object> data = r.getData();

        // 只允许面向用户的字段，任何形式的 system prompt 键都不得出现
        assertFalse(data.containsKey("systemPrompt"));
        assertFalse(data.containsKey("system"));
        assertFalse(data.containsKey("prompt"));
        assertEquals("business_analysis", data.get("scene"));
        assertEquals("模板欢迎语", data.get("welcome"));
        assertEquals(Arrays.asList("问题一", "问题二"), data.get("quickQuestions"));

        Map<String, Object> capabilities = (Map<String, Object>) data.get("capabilities");
        assertEquals(true, capabilities.get("chat"));
        assertEquals(true, capabilities.get("vision"));
        assertEquals(true, capabilities.get("tools"));

        // 员工需要场景切换清单
        assertNotNull(data.get("scenes"));
        // 关键：控制器压根不读取 system prompt
        verify(promptTemplateService, never()).getSystemPrompt(anyString());
    }

    @Test
    void customerCannotReachEmployeeScene() {
        // 顾客试图请求员工场景，必须回落到点餐场景
        R<Map<String, Object>> r = controller.getSceneConfig("business_analysis", customerRequest());
        Map<String, Object> data = r.getData();

        assertEquals("order_assistant", data.get("scene"));
        assertFalse(data.containsKey("systemPrompt"));
        // 顾客不下发后台场景清单
        assertNull(data.get("scenes"));
        verify(promptTemplateService, never()).getSystemPrompt(anyString());
    }

    @Test
    void customerDefaultSceneWithoutParam() {
        R<Map<String, Object>> r = controller.getSceneConfig(null, customerRequest());
        assertEquals("order_assistant", r.getData().get("scene"));
    }

    @Test
    void employeeInvalidSceneFallsBackToBusinessAnalysis() {
        R<Map<String, Object>> r = controller.getSceneConfig("nope", employeeRequest());
        assertEquals("business_analysis", r.getData().get("scene"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void capabilitiesFollowProviderSwitches() {
        Map<String, Boolean> caps = new HashMap<>();
        caps.put("vision", Boolean.FALSE);
        when(providerManager.getCapabilities()).thenReturn(caps);
        when(providerManager.supportsToolCalling()).thenReturn(false);

        R<Map<String, Object>> r = controller.getSceneConfig("business_analysis", employeeRequest());
        Map<String, Object> capabilities = (Map<String, Object>) r.getData().get("capabilities");
        assertEquals(false, capabilities.get("vision"));
        assertEquals(false, capabilities.get("tools"));
        // chat 永远可用
        assertEquals(true, capabilities.get("chat"));
    }

    @Test
    void templateReadFailureFallsBackToStaticWelcome() {
        when(promptTemplateService.getWelcome(anyString())).thenThrow(new RuntimeException("db down"));
        when(promptTemplateService.getQuickQuestions(anyString())).thenThrow(new RuntimeException("db down"));

        R<Map<String, Object>> r = controller.getSceneConfig("business_analysis", employeeRequest());
        Map<String, Object> data = r.getData();

        // 模板库异常时静态兜底，端点不失败、且仍无 system prompt
        assertNotNull(data.get("welcome"));
        List<?> quick = (List<?>) data.get("quickQuestions");
        assertTrue(quick != null && !quick.isEmpty());
        assertFalse(data.containsKey("systemPrompt"));
    }

    // ==================== 辅助 ====================

    /** 带员工会话的请求 */
    private HttpServletRequest employeeRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("employee")).thenReturn(new Object());
        return request;
    }

    /** 无登录会话的顾客请求 */
    private HttpServletRequest customerRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getSession(false)).thenReturn(null);
        return request;
    }
}
