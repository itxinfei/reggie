package com.reggie.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * AI 智能输入提示服务（搜索联想模式）
 * <p>参照抖音搜索 sug 的实现模式（前缀匹配 + 热度加权 + 个性化），在本地数据上做轻量实现：
 * 候选来源 = 场景快捷问题（运营配置）+ 菜品名（点餐场景）+ 个人历史提问 + 店铺热点提问，
 * 前端防抖调用，用户点选后回填输入框（不自动发送）。</p>
 *
 * @author reggie
 * @since 2026-09-30
 */
public interface AiInputSuggestionService {

    /**
     * 根据输入前缀返回候选问题。
     *
     * @param userId    当前用户ID（可空：空时跳过个人历史源）
     * @param actorType EMPLOYEE/CUSTOMER（决定可用场景与候选源）
     * @param scene     场景（非法/空回落身份默认场景）
     * @param q         输入前缀（空/空白时返回场景快捷问题作为起步候选）
     * @return [{text: 候选文本, source: quick|dish|mine|hot}]，最多 {@code MAX_SUGGESTIONS} 条
     */
    List<Map<String, Object>> suggest(Long userId, String actorType, String scene, String q);
}
