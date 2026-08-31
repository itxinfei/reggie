package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.BatchFillHelper;
import com.reggie.common.CustomException;
import com.reggie.module.inventory.mapper.PurchaseOrderDetailMapper;
import com.reggie.module.inventory.mapper.PurchaseOrderMapper;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.enums.PurchaseOrderStatus;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.service.PurchaseOrderDetailService;
import com.reggie.module.inventory.service.PurchaseOrderService;
import com.reggie.module.inventory.service.StockRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 采购单服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
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

    /** 供应商服务 */
    @Autowired
    private com.reggie.module.inventory.service.SupplierService supplierService;

    /** 采购单明细Mapper（用于原子收货 CAS） */
    @Autowired
    private PurchaseOrderDetailMapper purchaseOrderDetailMapper;

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
        // 租户归属校验：防止跨租户越权添加采购明细
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(po.getTenantId())) {
            throw new CustomException("无权操作其他租户的采购单");
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
        // 租户归属校验：防止跨租户越权收货
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(po.getTenantId())) {
            throw new CustomException("无权操作其他租户的采购单");
        }
        if (!PurchaseOrderStatus.ORDERED.getValue().equals(po.getStatus()) && !PurchaseOrderStatus.PARTIAL.getValue().equals(po.getStatus())) {
            throw new CustomException("采购单状态不允许收货");
        }

        List<PurchaseOrderDetail> details = detailService.list(
            new LambdaQueryWrapper<PurchaseOrderDetail>().eq(PurchaseOrderDetail::getPurchaseOrderId, orderId));

        // 修改点：明细收货用原子 CAS（received_qty<qty 才更新），据返回行数判断是否真正入库，
        // 消除并发重复收货导致库存翻倍
        for (PurchaseOrderDetail detail : details) {
            int rows = purchaseOrderDetailMapper.receiveFully(detail.getId(), detail.getQty());
            if (rows > 0) {
                // 首次收货成功——按未收数量入库（已收数量从内存快照取，CAS 保证仅一个线程入库）
                // 防御性 null 检查：qty/receivedQty 可能在数据库中为 null（历史数据）
                BigDecimal qty = detail.getQty() != null ? detail.getQty() : BigDecimal.ZERO;
                BigDecimal alreadyReceived = detail.getReceivedQty() != null ? detail.getReceivedQty() : BigDecimal.ZERO;
                BigDecimal toReceive = qty.subtract(alreadyReceived);
                if (toReceive.compareTo(BigDecimal.ZERO) > 0) {
                    stockRecordService.stockIn(detail.getMaterialId(), toReceive,
                        detail.getUnitPrice(), orderId, "采购入库", po.getOperator());
                }
            }
            // rows == 0 表示该明细已被他人收货，跳过入库
        }

        BigDecimal totalAmount = details.stream()
            .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);

        // 修改点：CAS 状态更新——仅当状态仍为 ORDERED/PARTIAL 时才置为 RECEIVED，
        // 返回 0 表示已被他人收货完成或状态已变更，抛异常回滚整个事务（含库存入库）
        LambdaUpdateWrapper<PurchaseOrder> casUpdate = new LambdaUpdateWrapper<>();
        casUpdate.eq(PurchaseOrder::getId, orderId)
            .in(PurchaseOrder::getStatus, PurchaseOrderStatus.ORDERED.getValue(), PurchaseOrderStatus.PARTIAL.getValue())
            .set(PurchaseOrder::getStatus, PurchaseOrderStatus.RECEIVED.getValue())
            .set(PurchaseOrder::getTotalAmount, totalAmount);
        int updated = baseMapper.update(null, casUpdate);
        if (updated == 0) {
            throw new CustomException("采购单已被他人收货或状态已变更");
        }
    }

    public List<PurchaseOrder> list(Wrapper<PurchaseOrder> queryWrapper) {
        List<PurchaseOrder> list = super.list(queryWrapper);
        if (!org.springframework.util.CollectionUtils.isEmpty(list)) {
            fillSupplierName(list);
        }
        return list;
    }

    /**
     * 修改点：重写带条件分页（Controller 实际调用 page(pageInfo, qw)），在父类分页结果上回填供应商名称，
     * 否则采购单列表 supplierName 列空白。IService.page 为泛型方法 <E extends IPage<T>>，子类必须以相同泛型签名重写。
     */
    @Override
    public <E extends IPage<PurchaseOrder>> E page(E page, Wrapper<PurchaseOrder> queryWrapper) {
        E result = super.page(page, queryWrapper);
        List<PurchaseOrder> records = result.getRecords();
        if (!org.springframework.util.CollectionUtils.isEmpty(records)) {
            fillSupplierName(records);
        }
        return result;
    }

    @Override
    public List<PurchaseOrderDetail> getDetailsByOrderId(Long orderId) {
        List<PurchaseOrderDetail> details = detailService.list(
            new LambdaQueryWrapper<PurchaseOrderDetail>().eq(PurchaseOrderDetail::getPurchaseOrderId, orderId));
        if (!org.springframework.util.CollectionUtils.isEmpty(details)) {
            fillMaterialName(details);
        }
        return details;
    }

    /**
     * 批量填充采购单的供应商名称
     */
    private void fillSupplierName(List<PurchaseOrder> orders) {
        BatchFillHelper.fillNames(
                orders,
                PurchaseOrder::getSupplierId,
                ids -> supplierService.list(new LambdaQueryWrapper<Supplier>().in(Supplier::getId, ids))
                        .stream().collect(Collectors.toMap(Supplier::getId, Supplier::getName, (v1, v2) -> v1)),
                PurchaseOrder::setSupplierName);
    }

    /**
     * 批量填充采购明细的物料名称
     */
    private void fillMaterialName(List<PurchaseOrderDetail> details) {
        BatchFillHelper.fillNames(
                details,
                PurchaseOrderDetail::getMaterialId,
                ids -> materialService.list(new LambdaQueryWrapper<Material>().in(Material::getId, ids))
                        .stream().collect(Collectors.toMap(Material::getId, Material::getName, (v1, v2) -> v1)),
                PurchaseOrderDetail::setMaterialName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveOrder(Long orderId) {
        PurchaseOrder po = getById(orderId);
        if (po == null) {
            throw new CustomException("采购单不存在");
        }
        // 租户归属校验：防止跨租户越权审批采购单
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(po.getTenantId())) {
            throw new CustomException("无权操作其他租户的采购单");
        }
        if (!PurchaseOrderStatus.DRAFT.getValue().equals(po.getStatus())) {
            throw new CustomException("只有草稿状态的采购单才能审核");
        }
        po.setStatus(PurchaseOrderStatus.ORDERED.getValue());
        updateById(po);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        PurchaseOrder po = getById(orderId);
        if (po == null) {
            throw new CustomException("采购单不存在");
        }
        // 租户归属校验：防止跨租户越权取消采购单
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(po.getTenantId())) {
            throw new CustomException("无权操作其他租户的采购单");
        }
        if (PurchaseOrderStatus.RECEIVED.getValue().equals(po.getStatus()) || PurchaseOrderStatus.CANCELLED.getValue().equals(po.getStatus())) {
            throw new CustomException("采购单状态不允许取消");
        }
        po.setStatus(PurchaseOrderStatus.CANCELLED.getValue());
        updateById(po);
    }
}

