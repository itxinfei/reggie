package com.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.Orders;

import java.util.List;
import java.util.Map;

public interface OrderService extends IService<Orders> {

    /**
     * 用户下单
     * @param orders
     */
    public void submit(Orders orders);

    Page<Orders> orderPage(int page, int pageSize, String number, String beginTime, String endTime);

    void updateStatus(Integer status, Long id);

    /**
     * 用户订单分页查询
     * @param page 页码
     * @param pageSize 每页条数
     * @param status 订单状态（可选，为null则查询全部）
     */
    Page<?> userPage(int page, int pageSize, Integer status);

    List<Orders> userList();

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
}
