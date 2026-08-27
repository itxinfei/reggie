package com.reggie.module.urgency.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.urgency.model.UrgencyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 催单记录 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-27
 */
@Mapper
public interface UrgencyMapper extends BaseMapper<UrgencyRecord> {

    /**
     * 按会员ID查询催单记录（按创建时间倒序）
     *
     * @param memberId 会员ID
     * @param tenantId 租户ID
     * @param limit    返回条数上限
     * @return 催单记录列表
     */
    List<UrgencyRecord> listByMemberId(@Param("memberId") Long memberId,
                                        @Param("tenantId") Long tenantId,
                                        @Param("limit") Integer limit);

    /**
     * 统计指定会员今日催单次数
     *
     * @param memberId 会员ID
     * @param tenantId 租户ID
     * @param date     日期
     * @return 催单次数
     */
    Integer countTodayByMember(@Param("memberId") Long memberId,
                                @Param("tenantId") Long tenantId,
                                @Param("date") LocalDate date);

    /**
     * 获取催单统计数据
     *
     * @param tenantId 租户ID
     * @return 统计数据列表
     */
    List<Map<String, Object>> getUrgencyStats(@Param("tenantId") Long tenantId);

    /**
     * 统计今日催单总数
     *
     * @param tenantId 租户ID
     * @param date     日期
     * @return 催单总数
     */
    Integer countToday(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    /**
     * 统计本周催单总数
     *
     * @param tenantId   租户ID
     * @param weekStart  周起始日期
     * @return 催单总数
     */
    Integer countWeek(@Param("tenantId") Long tenantId, @Param("weekStart") LocalDate weekStart);

    /**
     * 计算平均响应时间（分钟）
     *
     * @param tenantId 租户ID
     * @return 平均响应时间
     */
    @Select("SELECT AVG(TIMESTAMPDIFF(MINUTE, create_time, update_time)) FROM urgency_record WHERE tenant_id = #{tenantId} AND update_time IS NOT NULL")
    BigDecimal avgResponseTime(@Param("tenantId") Long tenantId);
}
