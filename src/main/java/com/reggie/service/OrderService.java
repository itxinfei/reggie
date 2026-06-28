package com.reggie.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.entity.Orders;

public interface OrderService extends IService<Orders> {

    /**
     * 用户下单
     * @param orders
     */
    public void submit(Orders orders);

    public Page orderPage(int page, int pageSize, String number, String beginTime, String endTime);

    public void updateStatus(Integer status, Long id);

    Page userPage(int page, int pageSize);

    void again(Long orderId);
}
