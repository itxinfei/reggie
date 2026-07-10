package com.reggie.module.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.ai.model.UserProfile;

/**
 * AI用户画像服务
 * 基于历史对话、点单、反馈数据构建用户长期记忆
 *
 * @author reggie
 * @since 2026-07-09
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
}
