package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import java.math.BigDecimal;
import java.util.List;

/**
 * 采购订单服务接口
 * 提供采购订单的创建、明细添加、收货确认、取消等功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface PurchaseOrderService extends IService<PurchaseOrder> {

    /**
     * 创建采购订单
     *
     * @param supplierId 供应商ID
     * @param operator   操作人
     * @param remark     备注
     * @return 采购订单
     */
    PurchaseOrder createOrder(Long supplierId, String operator, String remark);

    /**
     * 向采购订单添加明细行
     *
     * @param orderId    订单ID
     * @param materialId 原料ID
     * @param qty        采购数量
     * @param unitPrice  单价
     */
    void addDetail(Long orderId, Long materialId, BigDecimal qty, BigDecimal unitPrice);

    /**
     * 确认收货（入库）
     *
     * @param orderId 订单ID
     */
    void receiveOrder(Long orderId);

    /**
     * 审核通过采购订单（DRAFT → ORDERED）
     *
     * @param orderId 订单ID
     */
    void approveOrder(Long orderId);

    /**
     * 取消采购订单
     *
     * @param orderId 订单ID
     */
    void cancelOrder(Long orderId);

    /**
     * 获取采购订单的明细列表
     *
     * @param orderId 订单ID
     * @return 明细列表
     */
    List<PurchaseOrderDetail> getDetailsByOrderId(Long orderId);
}
