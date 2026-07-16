package com.reggie.module.printer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.printer.model.PrinterConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * <p>
 * 打印机配置 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface PrinterConfigMapper extends BaseMapper<PrinterConfig> {

    /**
     * 打印机配置统计（总数、启用、停用、连接类型种类数）
     * <p>printer_config 表在租户忽略列表中，需在 SQL 中手动处理租户过滤（tenantId 为 null 时不过滤）</p>
     *
     * @param tenantId 租户ID，可为 null（超管视图跳过租户过滤）
     * @return 聚合结果：totalPrinters/activePrinters/inactivePrinters/typeCount
     */
    @Select("SELECT "
            + "COUNT(*) AS totalPrinters, "
            + "COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS activePrinters, "
            + "COALESCE(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS inactivePrinters, "
            + "COUNT(DISTINCT type) AS typeCount "
            + "FROM printer_config "
            + "WHERE (#{tenantId} IS NULL OR tenant_id = #{tenantId})")
    Map<String, Object> statPrinters(@Param("tenantId") Long tenantId);
}
