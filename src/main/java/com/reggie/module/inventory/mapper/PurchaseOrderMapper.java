package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 采购订单 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    /**
     * 统计指定时间区间内的采购总金额
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM purchase_order WHERE 1=1 "
            + "AND created_time BETWEEN #{start} AND #{end}")
    BigDecimal sumAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计指定时间区间内的采购单数量
     */
    @Select("SELECT COUNT(*) FROM purchase_order WHERE 1=1 "
            + "AND created_time BETWEEN #{start} AND #{end}")
    long countBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 统计指定时间区间内处于待收货（已下单/部分到货）的采购单数量
     */
    @Select("SELECT COUNT(*) FROM purchase_order WHERE 1=1 "
            + "AND created_time BETWEEN #{start} AND #{end} AND status IN (#{ordered}, #{partial})")
    long countPendingReceive(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                             @Param("ordered") String ordered, @Param("partial") String partial);

    /**
     * 查询指定时间之后的采购单（用于近30天趋势，避免全表扫描）
     */
    @Select("SELECT id, order_no AS orderNo, supplier_id AS supplierId, total_amount AS totalAmount, "
            + "status, created_time AS createdTime FROM purchase_order WHERE 1=1 AND created_time >= #{start} "
            + "ORDER BY created_time ASC")
    List<PurchaseOrder> selectSince(@Param("start") LocalDateTime start);
}
