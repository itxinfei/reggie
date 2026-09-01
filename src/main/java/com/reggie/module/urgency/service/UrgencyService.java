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

    /**
     * 发起催单操作（含频率控制）
     * 每人每天最多催单次数受限，超出返回错误
     *
     * @param orderId  订单ID
     * @param memberId 会员ID
     * @param orderNo  订单号
     * @return 催单结果
     */
    R<Map<String, Object>> triggerUrgency(Long orderId, Long memberId, String orderNo);

    /**
     * 查询催单记录列表
     *
     * @param memberId 会员ID
     * @return 催单记录列表
     */
    R<Map<String, Object>> getUrgencyRecords(Long memberId);

    /**
     * 获取催单统计数据
     *
     * @return 催单统计（总次数/今日/本周/平均响应时间）
     */
    R<Map<String, Object>> getUrgencyStats();

    /**
     * 频率检查
     *
     * @param memberId 会员ID
     * @return 频率控制信息
     */
   R<Map<String, Object>> checkFrequency(Long memberId);

    /**
     * 获取未接单实时监控看板（商家主动漏单预警）
     * <p>查询当前租户待接单状态订单，按等待时长分级（正常/预警/告警/漏单）返回，
     * 供接单大屏轮询展示与语音播报。阈值取自系统配置。</p>
     *
     * @param tenantId 租户ID
     * @return 监控数据（统计概览 + 订单列表 + 阈值配置）
     */
    Map<String, Object> getUnacceptedMonitor(Long tenantId);

    /**
     * 定时扫描待接单订单并主动告警（漏单预警核心）
     * <p>由 {@code UnacceptedOrderScanTask} 周期性调用。对超黄金时长仍未接单的订单
     * 通知店长（短信/通知记录），对超漏单阈值（3 倍黄金时长）的订单升级为漏单告警。
     * 使用 Redis Set 做每订单一次性去重，Redis 不可用时跳过本轮主动通知（fail-closed）。</p>
     *
     * @param tenantId 租户ID
     * @return 本轮新触发的通知数
     */
    int scanUnacceptedAndAlert(Long tenantId);
}
