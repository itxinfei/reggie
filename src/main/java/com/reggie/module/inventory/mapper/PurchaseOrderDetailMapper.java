package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.PurchaseOrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
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

    /**
     * 部分收货：增量更新 received_qty += #{receiveQty}，上限为 qty。
     * CAS 保证不超收：仅当 IFNULL(received_qty,0) + #{receiveQty} <= qty 时才更新。
     *
     * @param id         明细ID
     * @param receiveQty 本次收货数量
     * @return 受影响行数，0 表示超收或已满
     */
    @Update("UPDATE purchase_order_detail " +
            "SET received_qty = IFNULL(received_qty, 0) + #{receiveQty} " +
            "WHERE id = #{id} AND IFNULL(received_qty, 0) + #{receiveQty} <= qty")
    int receivePartial(@Param("id") Long id, @Param("receiveQty") BigDecimal receiveQty);

    /**
     * 行锁当前读：查询明细已收数量并加 FOR UPDATE 行锁。
     * 全量收货据此拿最新已提交的 received_qty（而非进入方法时的内存快照），并与并发的部分/全量
     * 收货串行，消除"部分已收 + 全量按旧快照多入库"。租户条件由 TenantLineInnerInterceptor 自动注入。
     *
     * @param id 明细ID
     * @return 当前已收数量（null 已由 SQL IFNULL 兜底为 0）
     */
    @Select("SELECT IFNULL(received_qty, 0) FROM purchase_order_detail WHERE id = #{id} FOR UPDATE")
    BigDecimal selectReceivedForUpdate(@Param("id") Long id);
}
