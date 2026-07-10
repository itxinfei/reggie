package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购订单明细 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface PurchaseOrderDetailMapper extends BaseMapper<PurchaseOrderDetail> {
}
