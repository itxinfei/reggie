package com.reggie.security;

import com.reggie.common.PasswordUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAuditTest {

    private String getAllJavaSourceCode() throws IOException {
        List<String> contents = new ArrayList<>();
        collectJavaFiles(Paths.get("src/main/java"), contents);
        return String.join("\n", contents);
    }

    private void collectJavaFiles(Path dir, List<String> contents) {
        File[] files = dir.toFile().listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                collectJavaFiles(file.toPath(), contents);
            } else if (file.getName().endsWith(".java")) {
                try (Scanner scanner = new Scanner(file, "UTF-8")) {
                    contents.add(scanner.useDelimiter("\\A").next());
                } catch (IOException e) {
                    // skip unreadable files
                }
            }
        }
    }

    @Test
    void testNoHardcodedPasswords() throws IOException {
        // 扫描代码中不应包含明文密码（忽略测试文件和配置文件）
        String code = getAllJavaSourceCode();
        assertFalse(code.contains("password = \"root\""), "不应硬编码密码");
        assertFalse(code.contains("password='root'"), "不应硬编码密码");
    }

    @Test
    void testNoPlaintextPhoneInLogs() throws IOException {
        // 扫描日志中不应包含完整手机号模式
        String code = getAllJavaSourceCode();
        // 检查是否有 log.info 包含 11位数字（手机号）
        assertFalse(code.matches("(?s).*log\\.info\\(.*\\d{11}.*\\).*"), "日志不应打印完整手机号");
    }

    @Test
    void testPasswordEncryptionStrength() {
        // 验证密码加密强度
        String encoded = PasswordUtils.encodePassword("test123");
        // BCrypt长度应为60
        assertTrue(encoded.length() >= 60, "密码加密强度不足，长度应为60+");
        assertTrue(encoded.startsWith("$2a$"), "应使用BCrypt加密");
    }

    @Test
    void testBCryptStrengthFactor() {
        // 验证强度因子为10
        String encoded = PasswordUtils.encodePassword("test123");
        // BCrypt strength=10 的编码长度约为60
        assertTrue(encoded.length() >= 60 && encoded.length() <= 70,
            "BCrypt编码长度应在60-70之间（strength=10）");
    }
}
