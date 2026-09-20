package com.reggie.module.ai.config;

import com.reggie.module.ai.service.AiPromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * AI 提示词内置模板初始化（P3）。
 * <p>首次启动（或升级后新增内置 code）补插缺失模板；已存在的 code 一律不动，保留运营修改。</p>
 *
 * @author reggie
 * @since 2026-09-21
 */
@Slf4j
@Component
@Order(20)
public class AiPromptTemplateSeeder implements ApplicationRunner {

    @Resource
    private AiPromptTemplateService promptTemplateService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            promptTemplateService.seedBuiltinsIfMissing();
        } catch (Exception e) {
            // 初始化失败不阻断启动：聊天链路对模板读取有 yml/静态兜底
            log.warn("AI提示词内置模板初始化失败，不影响启动: {}", e.getMessage());
        }
    }
}
