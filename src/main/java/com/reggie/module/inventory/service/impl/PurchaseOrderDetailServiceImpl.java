package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.inventory.mapper.PurchaseOrderDetailMapper;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import com.reggie.module.inventory.service.PurchaseOrderDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 采购单明细服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
/**
 * PurchaseOrderDetail service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class PurchaseOrderDetailServiceImpl extends ServiceImpl<PurchaseOrderDetailMapper, PurchaseOrderDetail> implements PurchaseOrderDetailService {

    @Override
    public List<PurchaseOrderDetail> listByOrderId(Long orderId) {
        return this.list(new LambdaQueryWrapper<PurchaseOrderDetail>()
                .eq(PurchaseOrderDetail::getPurchaseOrderId, orderId)
                .eq(PurchaseOrderDetail::getTenantId, BaseContext.getCurrentTenantId())
                .orderByAsc(PurchaseOrderDetail::getId));
    }
}


