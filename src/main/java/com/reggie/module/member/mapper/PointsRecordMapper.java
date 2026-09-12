package com.reggie.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.member.model.PointsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 积分记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {

    /**
     * 查询已过期且未处理的 IN 类型积分记录，按 member_id 分组汇总
     * @param now 当前时间（expire_time <= now 的记录视为过期）
     * @param tenantId 租户ID
     * @return 每组 {memberId, expiredPoints}
     */
    @Select("SELECT member_id AS memberId, SUM(points) AS expiredPoints " +
            "FROM points_record " +
            "WHERE type = 'IN' AND expire_time IS NOT NULL AND expire_time <= #{now} " +
            "AND is_deleted = 0 AND tenant_id = #{tenantId} " +
            "GROUP BY member_id")
    List<Map<String, Object>> sumExpiredByMemberId(@Param("now") LocalDateTime now,
                                                   @Param("tenantId") Long tenantId);

    /**
     * 将已过期的 IN 类型积分记录标记为逻辑删除（is_deleted=1），防止重复处理
     * @param now 当前时间
     * @param tenantId 租户ID
     * @return 受影响行数
     */
    @Update("UPDATE points_record SET is_deleted = 1 " +
            "WHERE type = 'IN' AND expire_time IS NOT NULL AND expire_time <= #{now} " +
            "AND is_deleted = 0 AND tenant_id = #{tenantId}")
    int markExpiredRecordsDeleted(@Param("now") LocalDateTime now,
                                  @Param("tenantId") Long tenantId);
}
