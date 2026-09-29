package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.delivery.model.RiderEvaluation;

import java.util.Map;

/**
 * 骑手评价服务。
 *
 * @author reggie
 * @since 2026-09-28
 */
public interface RiderEvaluationService extends IService<RiderEvaluation> {

    /**
     * 顾客提交骑手评价（同一订单对同一骑手仅一次有效评价，提交即自动通过）。
     *
     * @param evaluation 评价信息（orderId、riderId、starRating、content、tags、anonymous）
     * @return 已保存的评价
     */
    RiderEvaluation submit(RiderEvaluation evaluation);

    /**
     * 查询某订单对某骑手的评价（用于顾客判断是否已评价）。
     */
    RiderEvaluation getByOrderAndRider(Long tenantId, Long orderId, Long riderId);

    /**
     * 顾客视角：分页查询自己提交的评价。
     */
    Page<RiderEvaluation> pageByUserId(Long tenantId, Long userId, int page, int size);

    /**
     * 公开视角：分页查询某骑手已通过评价。
     */
    Page<RiderEvaluation> pageByRiderId(Long tenantId, Long riderId, int page, int size);

    /**
     * 骑手视角：分页查询收到的评价（当前登录骑手）。
     */
    Page<RiderEvaluation> pageReceived(Long tenantId, Long riderId, int page, int size);

    /**
     * 骑手评分统计（平均分 + 数量）。
     */
    Map<String, Object> getRiderStats(Long tenantId, Long riderId);

    /**
     * 管理端分页查询（支持骑手姓名、评分、状态筛选）。
     */
    Page<RiderEvaluation> adminPage(Long tenantId, String riderName, Integer starRating,
                                    Integer status, int page, int size);

    /**
     * 商家/管理员回复评价。
     */
    boolean reply(Long id, String replyContent, Long operatorId, Long tenantId);
}
