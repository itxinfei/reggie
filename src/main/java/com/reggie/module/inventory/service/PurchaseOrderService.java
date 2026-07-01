package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import java.math.BigDecimal;
import java.util.List;

public interface PurchaseOrderService extends IService<PurchaseOrder> {
    PurchaseOrder createOrder(Long supplierId, String operator, String remark);
    void addDetail(Long orderId, Long materialId, BigDecimal qty, BigDecimal unitPrice);
    void receiveOrder(Long orderId);
    void cancelOrder(Long orderId);
    List<PurchaseOrderDetail> getDetailsByOrderId(Long orderId);
}
