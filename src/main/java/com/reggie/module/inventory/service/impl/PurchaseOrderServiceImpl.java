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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购单服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class PurchaseOrderServiceImpl extends ServiceImpl<PurchaseOrderMapper, PurchaseOrder> implements
        PurchaseOrderService {

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

    /**
     * 创建 order。
     * @param supplierId 参数 supplierId
     * @param operator 参数 operator
     * @param remark 参数 remark
     * @return 返回结果
     */
    @Override
    public PurchaseOrder createOrder(Long supplierId, String operator, String remark) {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 并发下 MAX+1 生成单号可能撞号（两个请求同时读到同一 last），由唯一索引 uk_order_no 兜底：
        // 冲突时递增 seq 重试，最多 5 次，保证同一日单号唯一不重复
        int seq;
        try {
            LambdaQueryWrapper<PurchaseOrder> qw = new LambdaQueryWrapper<>();
            qw.likeRight(PurchaseOrder::getOrderNo, "PO" + datePrefix);
            qw.orderByDesc(PurchaseOrder::getOrderNo).last("LIMIT 1");
            PurchaseOrder last = getOne(qw);
            seq = last != null ? Integer.parseInt(last.getOrderNo().substring(10)) + 1 : 1;
        } catch (NumberFormatException e) {
            // 历史数据存在非纯数字后缀（异常单号）时，回退为当前时间戳后缀，避免阻塞创建
            seq = Integer.MAX_VALUE;
        }
        if (seq == Integer.MAX_VALUE) {
            PurchaseOrder po = new PurchaseOrder();
            po.setTenantId(BaseContext.getCurrentTenantId());
            po.setOrderNo("PO" + datePrefix + System.currentTimeMillis());
            po.setSupplierId(supplierId);
            po.setStatus(PurchaseOrderStatus.DRAFT.getValue());
            po.setOperator(operator);
            po.setRemark(remark);
            save(po);
            return po;
        }
        int attempts = 0;
        while (attempts++ < 5) {
            PurchaseOrder po = new PurchaseOrder();
            po.setTenantId(BaseContext.getCurrentTenantId());
            po.setOrderNo("PO" + datePrefix + String.format("%03d", seq));
            po.setSupplierId(supplierId);
            po.setStatus(PurchaseOrderStatus.DRAFT.getValue());
            po.setOperator(operator);
            po.setRemark(remark);
            try {
                save(po);
                return po;
            } catch (DuplicateKeyException e) {
                // 单号冲突（并发建单），seq+1 重试
                seq++;
            }
        }
        throw new CustomException("采购单号生成冲突，请重试");
    }

    /**
     * 新增 detail。
     * @param orderId 参数 orderId
     * @param materialId 参数 materialId
     * @param qty 参数 qty
     * @param unitPrice 参数 unitPrice
     */
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
        // 修改点：添加明细后实时重算采购单总金额，保证草稿期列表金额与明细合计一致（此前仅收货时重算）
        po.setTotalAmount(calcTotalAmount(orderId));
        if (!updateById(po)) {
            throw new CustomException("采购单已被他人更新，请刷新后重试");
        }
    }

    /**
     * 修改点：按明细计算采购单总金额 = Σ明细金额（qty×unitPrice）。
     *
     * @param orderId 采购单ID
     * @return 总金额（保留2位小数）
     */
    private BigDecimal calcTotalAmount(Long orderId) {
        List<PurchaseOrderDetail> details = detailService.list(
            new LambdaQueryWrapper<PurchaseOrderDetail>().eq(PurchaseOrderDetail::getPurchaseOrderId, orderId));
        return details.stream()
            .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 处理 receive order。
     * @param orderId 参数 orderId
     */
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
        if (!PurchaseOrderStatus.ORDERED.getValue().equals(po.getStatus()) && !PurchaseOrderStatus.PARTIAL.getValue()
                .equals(po.getStatus())) {
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
                BigDecimal alreadyReceived = detail.getReceivedQty() != null ? detail.getReceivedQty() : BigDecimal
                        .ZERO;
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
            .in(PurchaseOrder::getStatus, PurchaseOrderStatus.ORDERED.getValue(), PurchaseOrderStatus.PARTIAL
                    .getValue())
            .set(PurchaseOrder::getStatus, PurchaseOrderStatus.RECEIVED.getValue())
            .set(PurchaseOrder::getTotalAmount, totalAmount);
        int updated = baseMapper.update(null, casUpdate);
        if (updated == 0) {
            throw new CustomException("采购单已被他人收货或状态已变更");
        }
    }

    /**
     * 部分收货：按指定数量逐项入库，满收明细自动跳过，全部满收后整单变更为 RECEIVED
     *
     * @param orderId     采购订单ID
     * @param receiveQtys 明细ID → 本次收货数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receivePartialOrder(Long orderId, Map<Long, BigDecimal> receiveQtys) {
        PurchaseOrder po = getById(orderId);
        if (po == null) {
            throw new CustomException("采购单不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(po.getTenantId())) {
            throw new CustomException("无权操作其他租户的采购单");
        }
        if (!PurchaseOrderStatus.ORDERED.getValue().equals(po.getStatus())
                && !PurchaseOrderStatus.PARTIAL.getValue().equals(po.getStatus())) {
            throw new CustomException("采购单状态不允许收货");
        }

        List<PurchaseOrderDetail> details = detailService.list(
            new LambdaQueryWrapper<PurchaseOrderDetail>()
                .eq(PurchaseOrderDetail::getPurchaseOrderId, orderId));

        for (PurchaseOrderDetail detail : details) {
            BigDecimal receiveQty = receiveQtys.get(detail.getId());
            if (receiveQty == null || receiveQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue; // 跳过未指定或0数量的明细
            }
            int rows = purchaseOrderDetailMapper.receivePartial(detail.getId(), receiveQty);
            if (rows > 0) {
                stockRecordService.stockIn(detail.getMaterialId(), receiveQty,
                        detail.getUnitPrice(), po.getId(),
                        "采购单收货（部分）：" + po.getId(),
                        String.valueOf(BaseContext.getCurrentId()));
            }
            // rows==0 表示超收或已满，跳过
        }

        // 检查是否全部满收
        boolean allReceived = true;
        for (PurchaseOrderDetail detail : details) {
            BigDecimal qty = detail.getQty() != null ? detail.getQty() : BigDecimal.ZERO;
            BigDecimal received = detail.getReceivedQty() != null ? detail.getReceivedQty() : BigDecimal.ZERO;
            // 重新查询最新的 receivedQty（上面可能已更新）
            PurchaseOrderDetail fresh = detailService.getById(detail.getId());
            BigDecimal freshReceived = (fresh != null && fresh.getReceivedQty() != null) ? fresh.getReceivedQty() : BigDecimal.ZERO;
            if (freshReceived.compareTo(qty) < 0) {
                allReceived = false;
                break;
            }
        }

        // 更新采购单状态
        String newStatus = allReceived ? PurchaseOrderStatus.RECEIVED.getValue() : PurchaseOrderStatus.PARTIAL.getValue();
        LambdaUpdateWrapper<PurchaseOrder> uw = new LambdaUpdateWrapper<>();
        uw.eq(PurchaseOrder::getId, orderId)
          .in(PurchaseOrder::getStatus, PurchaseOrderStatus.ORDERED.getValue(), PurchaseOrderStatus.PARTIAL.getValue())
          .set(PurchaseOrder::getStatus, newStatus)
          .set(PurchaseOrder::getTotalAmount, calcTotalAmount(orderId));
        baseMapper.update(null, uw);
    }

    /**
     * 查询列表。
     * @param queryWrapper 参数 queryWrapper
     * @return 返回结果
     */
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

    /**
     * 获取 details by order id。
     * @param orderId 参数 orderId
     * @return 返回结果
     */
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
     * 批量填充采购明细的物料名称与单位（同一次物料查询，避免两轮 SQL）
     */
    private void fillMaterialName(List<PurchaseOrderDetail> details) {
        List<Long> materialIds = details.stream()
                .map(PurchaseOrderDetail::getMaterialId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (materialIds.isEmpty()) {
            return;
        }
        Map<Long, Material> materialMap = materialService.list(
                new LambdaQueryWrapper<Material>().in(Material::getId, materialIds))
                .stream().collect(Collectors.toMap(Material::getId, m -> m, (v1, v2) -> v1));
        details.forEach(d -> {
            Material material = materialMap.get(d.getMaterialId());
            if (material != null) {
                d.setMaterialName(material.getName());
                d.setUnit(material.getUnit());
            }
        });
    }

    /**
     * 审核通过 order。
     * @param orderId 参数 orderId
     */
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
        // 修改点：审核通过时同步重算总金额，兜底历史脏数据/手工改库导致的总金额与明细不一致
        po.setStatus(PurchaseOrderStatus.ORDERED.getValue());
        po.setTotalAmount(calcTotalAmount(orderId));
        if (!updateById(po)) {
            throw new CustomException("采购单已被他人更新，请刷新后重试");
        }
    }

    /**
     * 取消 order。
     * @param orderId 参数 orderId
     */
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
        if (PurchaseOrderStatus.RECEIVED.getValue().equals(po.getStatus()) || PurchaseOrderStatus.CANCELLED.getValue()
                .equals(po.getStatus())) {
            throw new CustomException("采购单状态不允许取消");
        }
        // 修改点：PARTIAL状态取消需冲销已部分收货入库的库存，防止库存虚高
        if (PurchaseOrderStatus.PARTIAL.getValue().equals(po.getStatus())) {
            List<PurchaseOrderDetail> details = detailService.list(
                new LambdaQueryWrapper<PurchaseOrderDetail>().eq(PurchaseOrderDetail::getPurchaseOrderId, orderId));
            for (PurchaseOrderDetail detail : details) {
                // 仅回滚实际已入库的数量（未收部分本就未增加库存，无需处理）
                BigDecimal received = detail.getReceivedQty() != null ? detail.getReceivedQty() : BigDecimal.ZERO;
                if (received.compareTo(BigDecimal.ZERO) > 0) {
                    stockRecordService.stockOut(detail.getMaterialId(), received,
                        orderId, "采购取消回滚", po.getOperator());
                }
            }
        }
        po.setStatus(PurchaseOrderStatus.CANCELLED.getValue());
        updateById(po);
    }
}

