package com.reggie.module.dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dining.model.Reservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 预订记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface ReservationMapper extends BaseMapper<Reservation> {

    /**
     * 按状态统计预订数量（SQL 聚合，避免全量加载到内存）
     *
     * @param tenantId 租户ID
     * @return 每个状态的计数，字段名 status / cnt
     */
    @Select("SELECT status, COUNT(*) AS cnt FROM dining_reservation "
            + "WHERE tenant_id = #{tenantId} AND is_deleted = 0 GROUP BY status")
    List<Map<String, Object>> countByStatus(@Param("tenantId") Long tenantId);
}
