# Task 13: 创建 LogMaskUtils

**Files:**
- Create: `src/main/java/com/reggie/common/LogMaskUtils.java`
- Test: `src/test/java/com/reggie/common/LogMaskUtilsTest.java`

## 任务描述

创建日志脱敏工具类，用于在日志中脱敏手机号、身份证、地址等敏感信息。

## 具体要求

### LogMaskUtils.java

```java
package com.reggie.common;

/**
 * 日志脱敏工具类
 */
public class LogMaskUtils {

    /**
     * 手机号脱敏
     * 示例：13812341234 -> 138****1234
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return phone;
        }
        // 处理带区号的格式 "0592-1234-5678"
        if (phone.contains("-")) {
            String[] parts = phone.split("-");
            if (parts.length == 3) {
                return String.format("%s-%s-%s", parts[0], maskMiddle(parts[1], 2, 2), maskEnd(parts[2], 4));
            }
        }
        // 普通手机号 "13812341234"
        if (phone.length() == 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return maskGeneric(phone, 3, 4);
    }

    /**
     * 身份证号脱敏
     * 示例：110101199001011234 -> 110***********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 地址脱敏
     * 示例：北京市朝阳区建国路88号 -> 北京***88号
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() < 6) {
            return address;
        }
        return address.substring(0, 3) + "***" + address.substring(address.length() - 3);
    }

    /**
     * 通用脱敏（保留前n后m）
     */
    private static String maskGeneric(String str, int keepPrefix, int keepSuffix) {
        if (str == null || str.length() <= keepPrefix + keepSuffix) {
            return str;
        }
        int maskLength = str.length() - keepPrefix - keepSuffix;
        return str.substring(0, keepPrefix) + mask(maskLength) + str.substring(str.length() - keepSuffix);
    }

    private static String maskMiddle(String str, int keepPrefix, int keepSuffix) {
        return maskGeneric(str, keepPrefix, keepSuffix);
    }

    private static String maskEnd(String str, int keepPrefix) {
        return maskGeneric(str, keepPrefix, 0);
    }

    private static String mask(int length) {
        return "*".repeat(length);
    }
}
```

### LogMaskUtilsTest.java

```java
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
```

## 验收标准

- [ ] LogMaskUtils.java 创建成功
- [ ] 包含 maskPhone、maskIdCard、maskAddress 三个方法
- [ ] LogMaskUtilsTest.java 所有测试通过（Tests run: 3, Failures: 0）

