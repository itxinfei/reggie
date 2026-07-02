=== Commits ===
323d590 feat: add SecurityConstants for security configurations

=== Diff Stat ===
 src/main/java/com/reggie/common/SecurityConstants.java     | 52 ++++++++++++++++++++++++++++++++++++++++++++++++++++
 src/test/java/com/reggie/common/SecurityConstantsTest.java | 16 ++++++++++++++++
 2 files changed, 68 insertions(+)

=== Diff (context=10) ===
diff --git a/src/main/java/com/reggie/common/SecurityConstants.java b/src/main/java/com/reggie/common/SecurityConstants.java
new file mode 100644
index 0000000..4515d5f
--- /dev/null
+++ b/src/main/java/com/reggie/common/SecurityConstants.java
@@ -0,0 +1,52 @@
+package com.reggie.common;
+
+/**
+ * 安全相关常量
+ */
+public class SecurityConstants {
+
+    /**
+     * 密码最小长度
+     */
+    public static final int PASSWORD_MIN_LENGTH = 6;
+
+    /**
+     * 密码最大长度
+     */
+    public static final int PASSWORD_MAX_LENGTH = 20;
+
+    /**
+     * 手机号正则
+     */
+    public static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";
+
+    /**
+     * 登录失败最大次数
+     */
+    public static final int MAX_LOGIN_FAIL_COUNT = 5;
+
+    /**
+     * 登录失败锁定时间（分钟）
+     */
+    public static final int LOGIN_LOCK_DURATION = 15;
+
+    /**
+     * 会话超时时间（秒）
+     */
+    public static final int SESSION_TIMEOUT = 1800; // 30分钟
+
+    /**
+     * 密码类型：MD5（旧）
+     */
+    public static final String PASSWORD_TYPE_MD5 = "MD5";
+
+    /**
+     * 密码类型：BCrypt（新）
+     */
+    public static final String PASSWORD_TYPE_BCRYPT = "BCRYPT";
+
+    /**
+     * 默认密码
+     */
+    public static final String DEFAULT_PASSWORD = "123456";
+}
diff --git a/src/test/java/com/reggie/common/SecurityConstantsTest.java b/src/test/java/com/reggie/common/SecurityConstantsTest.java
new file mode 100644
index 0000000..861bfb2
--- /dev/null
+++ b/src/test/java/com/reggie/common/SecurityConstantsTest.java
@@ -0,0 +1,16 @@
+package com.reggie.common;
+
+import org.junit.jupiter.api.Test;
+import static org.junit.jupiter.api.Assertions.*;
+
+class SecurityConstantsTest {
+
+    @Test
+    void testConstantsExist() {
+        assertNotNull(SecurityConstants.PASSWORD_MAX_LENGTH);
+        assertNotNull(SecurityConstants.PASSWORD_MIN_LENGTH);
+        assertNotNull(SecurityConstants.PHONE_PATTERN);
+        assertTrue(SecurityConstants.PASSWORD_MIN_LENGTH > 0);
+        assertTrue(SecurityConstants.PASSWORD_MAX_LENGTH >= SecurityConstants.PASSWORD_MIN_LENGTH);
+    }
+}
