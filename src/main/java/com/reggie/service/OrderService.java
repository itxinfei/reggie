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

    Page<?> userPage(int page, int pageSize);

    List<Orders> userList();

    void again(Long orderId);
}
