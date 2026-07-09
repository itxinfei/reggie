package com.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.Orders;

import java.util.List;

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
}
