package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.utils.PageUtils;
import com.reggie.enums.PurchaseOrderStatus;
import com.reggie.module.inventory.mapper.SupplierMapper;
import com.reggie.module.inventory.model.PurchaseOrder;
import com.reggie.module.inventory.model.Supplier;
import com.reggie.module.inventory.service.PurchaseOrderService;
import com.reggie.module.inventory.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 供应商服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class SupplierServiceImpl extends ServiceImpl<SupplierMapper, Supplier> implements SupplierService {

    /**
     * 采购单服务（8.3.3 采购汇总聚合用）
     */
    @Autowired
    private PurchaseOrderService purchaseOrderService;

    /**
     * 分页查询（死代码：SupplierController 直接调用父类 page(pageInfo, qw)，
     * 未经过此方法。保留作为备用实现，但 TenantLineInnerInterceptor 已自动添加租户过滤）
     */
    @Override
    public Page<Supplier> pageQuery(int page, int pageSize, String name) {
        Page<Supplier> pageRequest = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Supplier> wrapper = new LambdaQueryWrapper<Supplier>()
                // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
                .orderByDesc(Supplier::getId);
        if (name != null && !name.trim().isEmpty()) {
            wrapper.like(Supplier::getName, name);
        }
        return this.page(pageRequest, wrapper);
    }

    /**
     * 查询已启用供应商（死代码：SupplierController 自己写了过滤逻辑，未调用此方法）
     */
    @Override
    public List<Supplier> listEnabled() {
        return this.list(new LambdaQueryWrapper<Supplier>()
                .eq(Supplier::getStatus, 1)
                // 修改点：删除冗余的手动 eq(tenantId)，由 TenantLineInnerInterceptor 统一处理
                .orderByAsc(Supplier::getId));
    }

    /**
     * 批量回填供应商采购汇总（8.3.3 采购汇总：累计采购金额 + 采购单笔数，排除已取消单）。
     * <p>单次 IN 查询全部采购单后按供应商分组聚合，避免逐行 count 查询。</p>
     */
    @Override
    public void fillPurchaseSummary(List<Supplier> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return;
        }
        List<Long> ids = suppliers.stream().map(Supplier::getId).collect(Collectors.toList());
        List<PurchaseOrder> orders = purchaseOrderService.list(new LambdaQueryWrapper<PurchaseOrder>()
                .in(PurchaseOrder::getSupplierId, ids)
                .ne(PurchaseOrder::getStatus, PurchaseOrderStatus.CANCELLED.getValue()));
        // 按供应商分组聚合
        Map<Long, BigDecimal> amountMap = new HashMap<>();
        Map<Long, Integer> countMap = new HashMap<>();
        for (PurchaseOrder order : orders) {
            Long sid = order.getSupplierId();
            if (sid == null) {
                continue;
            }
            amountMap.merge(sid, order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount(), BigDecimal::add);
            countMap.merge(sid, 1, Integer::sum);
        }
        for (Supplier supplier : suppliers) {
            BigDecimal amount = amountMap.get(supplier.getId());
            supplier.setTotalPurchaseAmount(amount == null ? BigDecimal.ZERO : amount);
            Integer count = countMap.get(supplier.getId());
            supplier.setPurchaseCount(count == null ? 0 : count);
        }
    }
}


