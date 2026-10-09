package com.reggie.module.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.ai.model.UserProfile;

/**
 * <p>
 * AI用户画像服务
 * </p>
 * <p>基于历史对话、点单、反馈数据构建用户长期记忆</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface UserProfileService extends IService<UserProfile> {

    /**
     * 获取或创建用户画像
     */
    UserProfile getOrCreateProfile(Long userId);

    /**
     * 构建画像摘要（供AI Prompt注入使用）
     * 返回一段描述用户偏好的自然语言文本
     */
    String buildProfileSummary(Long userId);

    /**
     * 更新用户画像（基于最新数据重新分析）
     */
    void refreshProfile(Long userId);

    /**
     * 按需刷新用户画像（带节流控制）
     * 如果距上次刷新不足冷却时间，则跳过本次刷新
     * 适用于每次对话触发的异步画像更新场景
     *
     * @param userId 用户ID
     */
    void refreshIfNeeded(Long userId);
}
