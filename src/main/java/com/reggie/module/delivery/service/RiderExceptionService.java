package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.delivery.model.RiderExceptionOrder;

import java.util.Map;

/**
 * 骑手异常工单服务：异常上报、骑手查询、管理端分页处理、改派。
 *
 * @author reggie
 * @since 2026-09-28
 */
public interface RiderExceptionService {

    /**
     * 骑手上报异常。
     *
     * @param riderId     上报骑手ID
     * @param tenantId    租户ID
     * @param orderId     订单ID（须为该骑手在途/已接单的订单）
     * @param exceptionType 异常类型
     * @param description 异常描述
     * @return 工单
     */
    RiderExceptionOrder submit(Long riderId, Long tenantId, Long orderId, Integer exceptionType, String description);

    /**
     * 骑手查询自己的异常工单（按创建时间倒序）。
     */
    Page<RiderExceptionOrder> pageMine(Long riderId, Long tenantId, int page, int size);

    /**
     * 管理端分页查询异常工单。
     *
     * @param tenantId     租户ID
     * @param exceptionType 异常类型（可选）
     * @param status       状态（可选）
     * @param orderNumber  订单号模糊（可选）
     * @param page         页码
     * @param size         每页大小
     */
    Page<RiderExceptionOrder> adminPage(Long tenantId, Integer exceptionType, Integer status,
                                       String orderNumber, int page, int size);

    /**
     * 管理端处理工单。
     *
     * @param id          工单ID
     * @param tenantId    租户ID
     * @param operatorId  处理人ID
     * @param action      处理动作：RESOLVE 解决 / CLOSE 关闭 / REASSIGN 改派
     * @param handleNote  处理备注
     * @param newRiderId  改派目标骑手ID（action=REASSIGN 时必填）
     * @return 处理后的工单
     */
    RiderExceptionOrder handle(Long id, Long tenantId, Long operatorId, String action,
                              String handleNote, Long newRiderId);

    /**
     * 统计各状态工单数量（管理端待办徽标用）。
     *
     * @param tenantId 租户ID
     * @return 键为状态值，值为数量
     */
    Map<Integer, Long> countByStatus(Long tenantId);
}
