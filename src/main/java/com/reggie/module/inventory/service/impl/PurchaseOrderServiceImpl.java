package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.inventory.mapper.PurchaseOrderMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import com.reggie.enums.PurchaseOrderStatus;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.service.PurchaseOrderDetailService;
import com.reggie.module.inventory.service.PurchaseOrderService;
import com.reggie.module.inventory.service.StockRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 采购单服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements PurchaseOrderService {

    /** 采购单明细服务 */
    @Autowired
    private PurchaseOrderDetailService detailService;

    /** 库存记录服务 */
    @Autowired
    private StockRecordService stockRecordService;

    /** 食材服务 */
    @Autowired
    private MaterialService materialService;

    @Override
    public PurchaseOrder createOrder(Long supplierId, String operator, String remark) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
        qw.likeRight(PurchaseOrder::getOrderNo, "PO" + datePrefix);
        qw.orderByDesc(PurchaseOrder::getOrderNo).last("LIMIT 1");
        PurchaseOrder last = getOne(qw);
        int seq = last != null ? Integer.parseInt(last.getOrderNo().substring(10)) + 1 : 1;

        PurchaseOrder po = new PurchaseOrder();
        po.setTenantId(BaseContext.getCurrentTenantId());
        po.setOrderNo("PO" + datePrefix + String.format("%03d", seq));
        po.setSupplierId(supplierId);
        po.setStatus(PurchaseOrderStatus.DRAFT.getValue());
        po.setOperator(operator);
        po.setRemark(remark);
        save(po);
        return po;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDetail(Long orderId, Long materialId, BigDecimal qty, BigDecimal unitPrice) {
        PurchaseOrder po = getById(orderId);
        if (po == null) {
            throw new CustomException("采购单不存在");
        }
        if (!PurchaseOrderStatus.DRAFT.getValue().equals(po.getStatus())) {
            throw new CustomException("采购单不是草稿状态，无法添加明细");
        }
        Material material = materialService.getById(materialId);
        if (material == null) {
            throw new CustomException("食材不存在");
        }

        PurchaseOrderDetail detail = new PurchaseOrderDetail();
        detail.setPurchaseOrderId(orderId);
        detail.setMaterialId(materialId);
        detail.setQty(qty);
        detail.setUnitPrice(unitPrice);
        detail.setAmount(unitPrice != null ? unitPrice.multiply(qty).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        detail.setReceivedQty(BigDecimal.ZERO);
        detailService.save(detail);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receiveOrder(Long orderId) {
        PurchaseOrder po = getById(orderId);
        if (po == null) {
            throw new CustomException("采购单不存在");
        }
        if (!PurchaseOrderStatus.ORDERED.getValue().equals(po.getStatus()) && !PurchaseOrderStatus.PARTIAL.getValue().equals(po.getStatus())) {
            throw new CustomException("采购单状态不允许收货");
        }

        List<PurchaseOrderDetail> details = detailService.list(
            new LambdaQueryWrapper<PurchaseOrderDetail>().eq(PurchaseOrderDetail::getPurchaseOrderId, orderId));
        boolean allReceived = true;
        for (PurchaseOrderDetail detail : details) {
            BigDecimal toReceive = detail.getQty().subtract(detail.getReceivedQty());
            if (toReceive.compareTo(BigDecimal.ZERO) > 0) {
                stockRecordService.stockIn(detail.getMaterialId(), toReceive,
                    detail.getUnitPrice(), orderId, "采购入库", po.getOperator());
                detail.setReceivedQty(detail.getQty());
                detailService.updateById(detail);
            } else {
                allReceived = false;
            }
        }

        po.setStatus(allReceived ? PurchaseOrderStatus.RECEIVED.getValue() : PurchaseOrderStatus.PARTIAL.getValue());
        po.setTotalAmount(details.stream()
            .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP));
        updateById(po);
    }

    @Override
    public List<PurchaseOrderDetail> getDetailsByOrderId(Long orderId) {
        return detailService.list(
            new LambdaQueryWrapper<PurchaseOrderDetail>().eq(PurchaseOrderDetail::getPurchaseOrderId, orderId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        PurchaseOrder po = getById(orderId);
        if (po == null) {
            throw new CustomException("采购单不存在");
        }
        if (PurchaseOrderStatus.RECEIVED.getValue().equals(po.getStatus()) || PurchaseOrderStatus.CANCELLED.getValue().equals(po.getStatus())) {
            throw new CustomException("采购单状态不允许取消");
        }
        po.setStatus(PurchaseOrderStatus.CANCELLED.getValue());
        updateById(po);
    }
}
