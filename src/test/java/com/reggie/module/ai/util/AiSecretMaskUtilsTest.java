package com.reggie.module.ai.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link AiSecretMaskUtils} 单元测试：URL 查询参数值一律脱敏，参数名保留。
 *
 * @author reggie
 * @since 2026-09-21
 */
class AiSecretMaskUtilsTest {

    @Test
    void nullOrEmptyReturnedAsIs() {
        assertNull(AiSecretMaskUtils.maskUrl(null));
        assertEquals("", AiSecretMaskUtils.maskUrl(""));
    }

    @Test
    void urlWithoutQueryReturnedAsIs() {
        assertEquals("https://host/v1/chat/completions",
                AiSecretMaskUtils.maskUrl("https://host/v1/chat/completions"));
    }

    @Test
    void singleParamValueMasked() {
        assertEquals("https://aip.baidubce.cn/path?access_token=***",
                AiSecretMaskUtils.maskUrl("https://aip.baidubce.cn/path?access_token=abcd1234"));
    }

    @Test
    void multipleParamValuesAllMasked() {
        assertEquals("https://host/path?a=***&b=***",
                AiSecretMaskUtils.maskUrl("https://host/path?a=1&b=2"));
    }

    @Test
    void emptyValueParamGetsMaskInserted() {
        assertEquals("https://host/path?a=***",
                AiSecretMaskUtils.maskUrl("https://host/path?a="));
    }

    @Test
    void valuelessParamKeptAsIs() {
        // ?flag 无等号，保持原样不处理
        assertEquals("https://host/path?flag",
                AiSecretMaskUtils.maskUrl("https://host/path?flag"));
    }

    @Test
    void mixedValuelessAndValuedParams() {
        assertEquals("https://host/path?flag&a=***",
                AiSecretMaskUtils.maskUrl("https://host/path?flag&a=secret"));
    }
}
