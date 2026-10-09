package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * <p>
 * 原料 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface MaterialMapper extends BaseMapper<Material> {

    /**
     * 统计预警食材数量（库存 <= 最低阈值 且 启用）
     * 租户过滤由 TenantLineInnerInterceptor 自动注入，无需手动拼接 tenant_id
     */
    @Select("SELECT COUNT(*) FROM material WHERE 1=1 "
            + "AND status = 1 AND stock_qty IS NOT NULL AND min_stock IS NOT NULL AND stock_qty <= min_stock")
    long countLowStock();

    /**
     * 统计库存总价值 = SUM(stock_qty * unit_price)
     */
    @Select("SELECT COALESCE(SUM(stock_qty * unit_price), 0) FROM material WHERE 1=1 "
            + "AND stock_qty IS NOT NULL AND unit_price IS NOT NULL")
    BigDecimal sumInventoryValue();

    /**
     * 查询预警食材明细（库存 <= 最低阈值 且 启用），按库存升序，最多 limit 条
     */
    @Select("SELECT id, name, stock_qty AS stockQty, min_stock AS minStock, unit "
            + "FROM material WHERE 1=1 AND status = 1 "
            + "AND stock_qty IS NOT NULL AND min_stock IS NOT NULL AND stock_qty <= min_stock "
            + "ORDER BY stock_qty ASC LIMIT #{limit}")
    List<Material> selectLowStock(@Param("limit") int limit);

    /**
     * 原子扣减食材库存：stock_qty = stock_qty - #{qty}，WHERE stock_qty >= #{qty} 防止超卖扣成负数
     * 租户过滤由 TenantLineInnerInterceptor 自动注入，无需手动拼接 tenant_id
     * @param id 食材ID
     * @param qty 扣减数量（正数）
     * @return 受影响行数，0 表示库存不足或食材不存在
     */
    @Update("UPDATE material SET stock_qty = stock_qty - #{qty}, update_time = NOW() " +
            "WHERE id = #{id} AND stock_qty >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * 原子增加食材库存：stock_qty = IFNULL(stock_qty, 0) + #{qty}
     * 租户过滤由 TenantLineInnerInterceptor 自动注入，无需手动拼接 tenant_id
     * @param id 食材ID
     * @param qty 增加数量（正数）
     * @return 受影响行数
     */
    @Update("UPDATE material SET stock_qty = IFNULL(stock_qty, 0) + #{qty}, update_time = NOW() " +
            "WHERE id = #{id}")
    int addStock(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /**
     * CAS 原子设置食材库存：仅当当前 stock_qty = #{expectedQty} 时才更新为 #{actualQty}。
     * 盘点场景用此方法替代 adjustStockTo，防止盘点期间并发出入库导致 bookQty 陈旧覆盖。
     * 返回 0 表示库存已被并发修改，调用方应重试。
     * @param id 食材ID
     * @param expectedQty 预期账面数量（读取时的 stock_qty）
     * @param actualQty 实际盘点数量
     * @return 受影响行数，0 表示 CAS 失败或食材不存在
     */
    @Update("UPDATE material SET stock_qty = #{actualQty}, update_time = NOW() "
            + "WHERE id = #{id} AND stock_qty = #{expectedQty}")
    int casAdjustStock(@Param("id") Long id, @Param("expectedQty") BigDecimal expectedQty,
                       @Param("actualQty") BigDecimal actualQty);
}
