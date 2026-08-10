package com.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.entity.OrderDetail;
import com.reggie.mapper.OrderDetailMapper;
import com.reggie.service.OrderDetailService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 订单明细服务实现类
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class OrderDetailServiceImpl extends ServiceImpl<OrderDetailMapper, OrderDetail> implements OrderDetailService {

    @Override
    public List<OrderDetail> listByOrderId(Long orderId) {
        return this.list(new LambdaQueryWrapper<OrderDetail>()
                .eq(OrderDetail::getOrderId, orderId)
                .eq(OrderDetail::getTenantId, BaseContext.getCurrentTenantId())
                .orderByAsc(OrderDetail::getId));
    }
}

