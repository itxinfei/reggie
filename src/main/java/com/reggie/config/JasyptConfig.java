package com.reggie.config;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Jasypt 配置加密器
 *
 * 用途：对 application 配置文件中的敏感信息（数据库密码、Redis 密码、API Key 等）进行加密存储。
 * 使用 {ENC(...)} 格式标记加密值，启动时由 Jasypt 自动解密。
 *
 * 工作流程：
 * 1. 管理员在本地使用 jasypt-cli 或命令行生成加密值：
 *    java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI
 *        input="your-password" password=secret-key algorithm=PBEWithMD5AndTripleDES
 * 2. 将输出的加密值放入配置文件：password: ENC(AQID...加密值...)
 * 3. 部署时通过环境变量 JASYPT_ENCRYPTOR_PASSWORD 传入加密密钥（禁止硬编码）
 *
 * 安全注意事项：
 * - JASYPT_ENCRYPTOR_PASSWORD 必须通过环境变量传入，禁止硬编码在代码或配置文件中
 * - 加密密钥与加密值一样敏感，应视为同等级别的凭证管理
 * - 不同环境应使用不同的加密密钥
 * - 若密钥泄露，需重新加密所有 ENC(...) 值并轮换密钥
 *
 * 仅在生产环境启用，开发环境使用明文密码便于调试。
 *
 * @author reggie
 * @since 2026-08-27
 */
@Slf4j
@Configuration
@Profile("prod")
public class JasyptConfig {

    /**
     * Jasypt 加密密钥，通过环境变量 JASYPT_ENCRYPTOR_PASSWORD 传入
     *
     * 安全设计：无默认值，若生产环境未设置 JASYPT_ENCRYPTOR_PASSWORD，
     * Spring 启动时将抛出 MissingRequiredPropertiesException，
     * 避免因使用弱默认密钥导致加密形同虚设。
     *
     * 部署方式：通过环境变量传入（K8s Secret / CI/CD 密钥管理 / 手动 export），
     * 禁止硬编码在代码、配置文件或 Dockerfile 中。
     */
    @Value("${JASYPT_ENCRYPTOR_PASSWORD}")
    private String encryptorPassword;

    /**
     * 创建 Jasypt 字符串加密器 Bean
     *
     * 算法说明：
     * - PBEWithMD5AndTripleDES：MD5 哈希 + 3DES 加密，与 jasypt-spring-boot-starter 2.x 默认算法一致
     * - poolSize=3：使用 3 个线程池提高并发解密性能
     * - saltGeneratorClassName：使用 UUID 生成随机盐，增加暴力破解难度
     * - ivGeneratorClassName：IV 向量生成器，3DES 需要
     *
     * @return Jasypt 字符串加密器
     */
    @Bean("jasyptStringEncryptor")
    public StringEncryptor stringEncryptor() {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();

        config.setPassword(encryptorPassword);
        config.setAlgorithm("PBEWithMD5AndTripleDES");
        config.setKeyObtentionIterations("1000");
        config.setPoolSize("3");
        config.setSaltGeneratorClassName(
                "org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName(
                "org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");

        encryptor.setConfig(config);

        log.info("Jasypt 配置加密器初始化完成（算法: PBEWithMD5AndTripleDES）");
        return encryptor;
    }
}