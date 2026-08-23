package com.reggie.module.urgency.service;

import com.reggie.common.R;

import java.util.List;
import java.util.Map;

/**
 * 催单服务接口
 * 面向中小餐厅老板，提供订单催单倒计时管理能力
 *
 * @author reggie
 * @since 2026-08-23
 */
public interface UrgencyService {

    /**
     * 获取催单概览统计
     *
     * @param tenantId 租户ID
     * @return 概览数据（催单中订单数/超时可催/平均等待时间/最长等待时间）
     */
    Map<String, Object> getUrgencyOverview(Long tenantId);

    /**
     * 获取催单列表
     *
     * @param tenantId 租户ID
     * @param status   状态筛选（可选）
     * @return 催单列表
     */
    List<Map<String, Object>> getUrgencyList(Long tenantId, String status);

    /**
     * 催单操作
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    R<Void> callNext(Long orderId);

    /**
     * 查看催单详情
     *
     * @param orderId  订单ID
     * @param tenantId 租户ID
     * @return 订单详情含制作进度
     */
    Map<String, Object> getUrgencyDetail(Long orderId, Long tenantId);

    /**
     * 获取叫号排队列表
     *
     * @param tenantId 租户ID
     * @return 排队数据（当前叫号/等待列表）
     */
    Map<String, Object> getQueueList(Long tenantId);

    /**
     * 获取催单统计汇总
     *
     * @param tenantId 租户ID
     * @return 催单统计（今日总数/完成率/平均响应时间）
     */
    Map<String, Object> getUrgencySummary(Long tenantId);
}
