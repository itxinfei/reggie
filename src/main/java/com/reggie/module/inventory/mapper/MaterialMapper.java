package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.Material;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
