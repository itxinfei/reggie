package com.reggie.module.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link AiUrlUtils} 单元测试：裸域名补 /v1、已带版本路径直拼、完整端点原样返回。
 *
 * @author reggie
 * @since 2026-09-21
 */
class AiUrlUtilsTest {

    @Test
    void nullOrBlankBaseUrlReturnedAsIs() {
        assertNull(AiUrlUtils.resolveEndpoint(null, "/chat/completions"));
        assertEquals("  ", AiUrlUtils.resolveEndpoint("  ", "/chat/completions"));
    }

    @Test
    void bareDomainGetsV1Prefix() {
        assertEquals("https://api.iamhc.cn/v1/chat/completions",
                AiUrlUtils.resolveEndpoint("https://api.iamhc.cn", "/chat/completions"));
    }

    @Test
    void trailingSlashesAreStrippedBeforeResolve() {
        assertEquals("https://api.iamhc.cn/v1/embeddings",
                AiUrlUtils.resolveEndpoint("https://api.iamhc.cn///", "/embeddings"));
    }

    @Test
    void versionedPathAppendsEndpointDirectly() {
        assertEquals("https://api.deepseek.com/v1/chat/completions",
                AiUrlUtils.resolveEndpoint("https://api.deepseek.com/v1", "/chat/completions"));
    }

    @Test
    void fullEndpointUrlReturnedAsIs() {
        assertEquals("https://host/v1/chat/completions",
                AiUrlUtils.resolveEndpoint("https://host/v1/chat/completions", "/chat/completions"));
    }

    @Test
    void customGatewayPathAppendsEndpoint() {
        assertEquals("https://host/gateway/ai/models",
                AiUrlUtils.resolveEndpoint("https://host/gateway/ai", "/models"));
    }

    @Test
    void baseUrlWithoutSchemeResolvesByFirstSlash() {
        // 无 :// 时按首个斜杠判定路径存在，直接拼接
        assertEquals("host:8080/v1/chat/completions",
                AiUrlUtils.resolveEndpoint("host:8080", "/chat/completions"));
    }
}
