package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.PurchaseOrderDetail;

import java.util.List;

/**
 * <p>
 * 采购订单明细服务接口
 * </p>
 * <p>管理采购订单的明细行（原料、数量、单价等）</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface PurchaseOrderDetailService extends IService<PurchaseOrderDetail> {

    /**
     * 根据采购订单ID查询明细列表
     *
     * @param orderId 采购订单ID
     * @return 明细列表
     */
    List<PurchaseOrderDetail> listByOrderId(Long orderId);
}
