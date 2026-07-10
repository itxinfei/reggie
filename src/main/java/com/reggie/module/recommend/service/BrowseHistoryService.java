package com.reggie.module.recommend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.recommend.model.BrowseHistory;

import java.util.List;
import java.util.Map;

/**
 * 浏览历史服务接口
 * 记录和分析用户菜品浏览行为
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface BrowseHistoryService extends IService<BrowseHistory> {

    /**
     * 记录用户浏览行为
     *
     * @param userId     用户ID
     * @param targetType 目标类型 1:菜品 2:套餐
     * @param targetId   目标ID
     * @param targetName 目标名称
     * @param duration   停留时长(秒)
     * @param actionType 行为类型
     */
    void recordBrowse(Long userId, Integer targetType, Long targetId,
                      String targetName, Integer duration, Integer actionType);

    /**
     * 获取用户最近浏览记录
     *
     * @param userId 用户ID
     * @param limit  条数上限
     * @return 浏览记录列表
     */
    List<BrowseHistory> getRecentHistory(Long userId, int limit);

    /**
     * 获取用户浏览最多的菜品分类统计
     *
     * @param userId 用户ID
     * @param limit  TOP N
     * @return 分类统计结果
     */
    List<Map<String, Object>> getTopCategories(Long userId, int limit);
}
