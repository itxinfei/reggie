package com.reggie.module.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.notification.model.NotificationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * <p>
 * 通知发送记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface NotificationRecordMapper extends BaseMapper<NotificationRecord> {

    /**
     * 统计指定时间范围内的通知发送情况（按渠道计次 + 成功/失败求和）
     * <p>notification_record 表在租户忽略列表中，需在 SQL 中手动处理租户过滤（tenantId 为 null 时不过滤，适配超管视图）</p>
     *
     * @param start    起始时间（含）
     * @param end      结束时间（含）
     * @param tenantId 租户ID，可为 null（超管视图跳过租户过滤）
     * @return 聚合结果：todaySms/todayPush/todaySuccess/todayFail
     */
    @Select("SELECT "
            + "COALESCE(SUM(CASE WHEN channel = 1 THEN 1 ELSE 0 END), 0) AS todaySms, "
            + "COALESCE(SUM(CASE WHEN channel = 2 THEN 1 ELSE 0 END), 0) AS todayPush, "
            + "COALESCE(SUM(success_count), 0) AS todaySuccess, "
            + "COALESCE(SUM(fail_count), 0) AS todayFail "
            + "FROM notification_record "
            + "WHERE create_time BETWEEN #{start} AND #{end} "
            + "AND (#{tenantId} IS NULL OR tenant_id = #{tenantId})")
    Map<String, Object> statBetween(@Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end,
                                    @Param("tenantId") Long tenantId);
}
