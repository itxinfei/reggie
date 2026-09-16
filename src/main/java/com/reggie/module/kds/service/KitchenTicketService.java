package com.reggie.module.kds.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.kds.model.KitchenTicket;
import com.reggie.module.kds.vo.KitchenBoardVO;

/**
 * 后厨出餐（KDS）服务。
 *
 * @author reggie
 * @since 2026-09-13
 */
public interface KitchenTicketService extends IService<KitchenTicket> {

    /**
     * 从已下单（待接单）订单幂等拉取生成厨房工单，已生成过的订单不重复建单。
     *
     * @return 本次新生成的工单数量
     */
    int pullPendingOrders();

    /**
     * 获取出餐大屏看板（按状态分栏 + 统计）。
     *
     * @param autoPull   是否先自动拉取新订单
     * @param stationCode 档口编码筛选（可选，null=全部档口）
     * @return 看板数据
     */
    KitchenBoardVO getBoard(boolean autoPull, String stationCode);

    /**
     * 开始制作：待制作 → 制作中。
     *
     * @param id 工单ID
     * @return 更新后的工单
     */
    KitchenTicket startCook(Long id);

    /**
     * 制作完成并叫号：制作中 → 待取餐，记录制作耗时。
     *
     * @param id 工单ID
     * @return 更新后的工单
     */
    KitchenTicket markReady(Long id);

    /**
     * 出餐/取餐完成：待取餐 → 已完成。
     *
     * @param id 工单ID
     * @return 更新后的工单
     */
    KitchenTicket finish(Long id);

    /**
     * 取消工单：待制作/制作中 → 已取消。
     *
     * @param id 工单ID
     * @return 更新后的工单
     */
    KitchenTicket cancel(Long id);

    /**
     * 切换加急标记。
     *
     * @param id 工单ID
     * @return 更新后的工单
     */
    KitchenTicket toggleUrgent(Long id);

    /**
     * 工单历史分页。
     *
     * @param page       页码
     * @param pageSize   每页条数
     * @param status     状态（可选）
     * @param stationCode 档口编码筛选（可选）
     * @return 分页结果
     */
    Page<KitchenTicket> pageTickets(int page, int pageSize, Integer status, String stationCode);
}
