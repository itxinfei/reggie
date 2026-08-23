package com.reggie.module.retention.service;

import com.reggie.common.R;

import java.util.List;
import java.util.Map;

/**
 * 会员留存服务接口
 *
 * @author reggie
 * @since 2026-08-23
 */
public interface RetentionService {

    /**
     * 获取会员留存概览（分层统计）
     *
     * @param tenantId 租户ID
     * @return 分层统计数据
     */
    Map<String, Object> getRetentionOverview(Long tenantId);

    /**
     * 获取会员列表（支持按等级和状态筛选）
     *
     * @param tenantId 租户ID
     * @param level    等级筛选（可选）
     * @param status   状态筛选（可选）
     * @return 会员列表
     */
    List<Map<String, Object>> getMemberList(Long tenantId, String level, String status);

    /**
     * 获取积分排行 Top20
     *
     * @param tenantId 租户ID
     * @return 积分排行列表
     */
    List<Map<String, Object>> getPointsRanking(Long tenantId);

    /**
     * 获取流失预警会员（>30天未下单）
     *
     * @param tenantId 租户ID
     * @return 流失预警列表
     */
    List<Map<String, Object>> getChurnWarning(Long tenantId);

    /**
     * 获取智能推荐（推荐发券对象 + 推荐券类型）
     *
     * @param tenantId 租户ID
     * @return 智能推荐列表
     */
    List<Map<String, Object>> getSmartRecommend(Long tenantId);

    /**
     * 向指定会员发送优惠券
     *
     * @param memberId 会员ID
     * @return 操作结果
     */
    R<Void> sendCoupon(Long memberId);

    /**
     * 批量发送优惠券
     *
     * @param memberIds 会员ID列表
     * @return 操作结果
     */
    R<Void> batchSendCoupon(List<Long> memberIds);
}
