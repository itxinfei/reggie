package com.reggie.module.delivery.service;

import com.reggie.module.delivery.dto.RiderTaskVO;

import java.util.List;

/**
 * 骑手任务查询服务：组装骑手端所需的订单 + 取餐门店 + 送餐地址 + 明细 + 时间戳。
 *
 * @author reggie
 * @since 2026-09-23
 */
public interface RiderTaskQueryService {

    /**
     * 我的任务列表。
     *
     * @param riderId 当前骑手ID
     * @param scope   范围：todo=待我接单的派单，delivering=配送中，history=历史
     * @return 任务列表
     */
    List<RiderTaskVO> listMine(Long riderId, String scope);

    /**
     * 抢单大厅：本租户待接单(status=2)且未指派骑手(rider_id IS NULL)的订单。
     *
     * @return 任务列表
     */
    List<RiderTaskVO> listHall();

    /**
     * 任务详情。仅本人已绑定的任务，或大厅中尚未被抢走的订单可查看。
     *
     * @param orderId 订单ID
     * @param riderId 当前骑手ID
     * @return 任务详情
     */
    RiderTaskVO getDetail(Long orderId, Long riderId);
}
