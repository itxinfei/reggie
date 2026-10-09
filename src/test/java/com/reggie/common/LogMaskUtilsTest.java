package com.reggie.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogMaskUtilsTest {

    @Test
    void testMaskPhone() {
        assertEquals("138****1234", LogMaskUtils.maskPhone("13812341234"));
        assertNull(LogMaskUtils.maskPhone(null));
        assertEquals("", LogMaskUtils.maskPhone(""));
    }

    @Test
    void testMaskIdCard() {
        assertEquals("110***********1234", LogMaskUtils.maskIdCard("110101199001011234"));
        assertNull(LogMaskUtils.maskIdCard(null));
    }

    @Test
    void testMaskAddress() {
        String addr = "北京市朝阳区建国路88号SOHO现代城";
        String masked = LogMaskUtils.maskAddress(addr);
        assertTrue(masked.contains("***"));
        assertTrue(masked.startsWith("北京"));
    }
}
