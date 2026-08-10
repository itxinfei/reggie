package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * <p>
 * 采购订单明细 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface PurchaseOrderDetailMapper extends BaseMapper<PurchaseOrderDetail> {

    /**
     * 原子收货：仅当 received_qty < qty 时才置为全收（received_qty = qty）。
     * 修改点：CAS 防并发重复收货导致库存翻倍，据返回行数判断是否真正入库。
     * 租户过滤由 TenantLineInnerInterceptor 自动注入，无需手动拼接 tenant_id
     * @param id 明细ID
     * @param qty 订单数量
     * @return 受影响行数，0 表示已被他人收货
     */
    @Update("UPDATE purchase_order_detail SET received_qty = #{qty} " +
            "WHERE id = #{id} AND IFNULL(received_qty, 0) < #{qty}")
    int receiveFully(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
