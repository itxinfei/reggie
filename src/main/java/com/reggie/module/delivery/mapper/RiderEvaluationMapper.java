package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.RiderEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * 骑手评价 Mapper。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Mapper
public interface RiderEvaluationMapper extends BaseMapper<RiderEvaluation> {

    /**
     * 统计骑手已通过评价的平均分与数量（逻辑删除与未通过评价不计入）。
     *
     * @param riderId  骑手ID
     * @param tenantId 租户ID
     * @return 包含 avg（平均分）、cnt（数量）的映射
     */
    @Select("SELECT COALESCE(AVG(star_rating), 0) AS avg, COUNT(*) AS cnt FROM rider_evaluation "
            + "WHERE rider_id = #{riderId} AND tenant_id = #{tenantId} AND status = 1 AND is_deleted = 0")
    Map<String, Object> selectRiderStats(@Param("riderId") Long riderId, @Param("tenantId") Long tenantId);
}
