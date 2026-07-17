package com.reggie.module.dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dining.model.DiningTable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 桌台 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface DiningTableMapper extends BaseMapper<DiningTable> {

    /**
     * 按区域(area_id)分组统计桌台数量（关联区域名称）
     * <p>用于区域统计页"最大容量区域"指标，替代前端 pageSize:999 拉全量后前端分组，避免全表扫描</p>
     *
     * @return 每个区域的 areaId/areaName/cnt
     */
    @Select("SELECT dt.area_id AS areaId, ta.name AS areaName, COUNT(*) AS cnt "
            + "FROM dining_table dt LEFT JOIN dining_area ta ON ta.id = dt.area_id "
            + "WHERE 1=1 GROUP BY dt.area_id, ta.name")
    List<Map<String, Object>> statByArea();
}
