package com.reggie.module.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.inventory.model.StockRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 库存变动记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface StockRecordMapper extends BaseMapper<StockRecord> {

    /**
     * 统计指定时间区间内指定类型的出入库记录数量
     */
    @Select("SELECT COUNT(*) FROM stock_record WHERE 1=1 "
            + "AND create_time BETWEEN #{start} AND #{end} AND type = #{type}")
    long countByTypeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                             @Param("type") String type);

    /**
     * 统计指定时间区间内指定类型的出入库总金额
     */
    @Select("SELECT COALESCE(SUM(total_amount), 0) FROM stock_record WHERE 1=1 "
            + "AND create_time BETWEEN #{start} AND #{end} AND type = #{type} AND total_amount IS NOT NULL")
    BigDecimal sumAmountByTypeBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end,
                                      @Param("type") String type);

    /**
     * 查询指定时间之后的库存记录（用于近30天趋势，避免全表扫描）
     */
    @Select("SELECT id, material_id AS materialId, type, qty, total_amount AS totalAmount, "
            + "create_time AS createdTime FROM stock_record WHERE 1=1 AND create_time >= #{start} "
            + "ORDER BY create_time ASC")
    List<StockRecord> selectSince(@Param("start") LocalDateTime start);
}
