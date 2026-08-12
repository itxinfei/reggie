package com.reggie.module.order.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 订单管理服务接口，提供订单提交、查询、状态流转及幂等性保护功能
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface OrderService extends IService<Orders> {

    /**
     * 用户下单
     *
     * @param orders 订单信息
     */
    public void submit(Orders orders);

    /**
     * 堂食扫码下单（无需购物车和地址簿）
     *
     * @param orders 订单信息（含 source/tableId/tableName）
     * @param orderDetails 订单明细列表（直接从前端传入）
     */
    void submitEatInOrder(Orders orders, List<OrderDetail> orderDetails);

    /**
     * 后台订单分页查询，支持按订单号和时间范围筛选
     *
     * @param page 页码
     * @param pageSize 每页条数
     * @param number 订单号（可选）
     * @param beginTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param status 订单状态（可选）
     * @return 分页订单列表
     */
    Page<Orders> orderPage(int page, int pageSize, String number, String beginTime, String endTime, Integer status);

    /**
     * 修改订单状态
     *
     * @param status 目标状态
     * @param id 订单ID
     */
    void updateStatus(Integer status, Long id);

    /**
     * 用户订单分页查询
     * @param page 页码
     * @param pageSize 每页条数
     * @param status 订单状态（可选，为null则查询全部）
     */
    Page<?> userPage(int page, int pageSize, Integer status);

    /**
     * 查询当前用户的历史订单列表
     *
     * @return 用户订单列表
     */
    List<Orders> userList();

    /**
     * 再来一单，根据历史订单重新下单
     *
     * @param orderId 历史订单ID
     */
    void again(Long orderId);

    // ==================== 后台订单管理 ====================

    /**
     * 后台接单（待接单→配送中）
     * @param id 订单ID
     */
    void confirmOrder(Long id);

    /**
     * 后台拒单（待接单→已取消）
     * @param id 订单ID
     */
    void rejectOrder(Long id);

    /**
     * 后台完成订单（配送中→已完成）
     * @param id 订单ID
     */
    void completeOrder(Long id);

    /**
     * 取消订单（任意状态→已取消）
     * @param id 订单ID
     * @param reason 取消原因
     */
    void cancelOrder(Long id, String reason);

    /**
     * 订单统计（今日/待处理/各状态数量）
     * @return 统计Map
     */
    Map<String, Object> getOrderStatistics();

    // ==================== 幂等性保护 ====================

    /**
     * 检查幂等令牌是否已使用
     * @param idempotencyKey 幂等令牌
     * @return 已存在的订单（如果重复提交），null（如果首次提交）
     */
    Orders checkIdempotency(String idempotencyKey);

    /**
     * 回填订单中的用户信息（用户名、手机号、地址、收货人）
     *
     * @param order 订单实体
     */
    void backfillUserInfo(Orders order);
}

