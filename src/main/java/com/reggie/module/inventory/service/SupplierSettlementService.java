package com.reggie.module.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.inventory.model.SupplierSettlement;

/**
 * <p>
 * 供应商结算单服务接口
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
public interface SupplierSettlementService extends IService<SupplierSettlement> {

    /**
     * 分页查询结算单
     */
    Page<SupplierSettlement> pageSettlements(int page, int pageSize, Long supplierId, String status);

    /**
     * 创建结算单
     */
    SupplierSettlement createSettlement(SupplierSettlement settlement);
}
