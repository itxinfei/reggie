package com.reggie.module.retention.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.retention.model.RetentionMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 会员留存 Mapper 接口
 *
 * @author reggie
 * @since 2026-08-27
 */
@Mapper
public interface RetentionMapper extends BaseMapper<RetentionMember> {

    /**
     * 获取活跃会员列表
     *
     * @param tenantId 租户ID
     * @param limit    限制数量
     * @return 会员列表
     */
    List<RetentionMember> listActiveMembers(@Param("tenantId") Long tenantId,
                                            @Param("limit") Integer limit);

    /**
     * 获取流失风险会员列表（距上次下单超过指定天数）
     *
     * @param tenantId       租户ID
     * @param daysThreshold  天数阈值
     * @return 流失风险会员列表
     */
    List<RetentionMember> listChurnRiskMembers(@Param("tenantId") Long tenantId,
                                               @Param("daysThreshold") Integer daysThreshold);

    /**
     * 获取留存趋势数据
     *
     * @param tenantId  租户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 趋势数据列表
     */
    List<Map<String, Object>> getRetentionTrend(@Param("tenantId") Long tenantId,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate);

    /**
     * 按等级获取会员列表
     *
     * @param tenantId 租户ID
     * @param level    等级
     * @return 会员列表
     */
    List<RetentionMember> listByLevel(@Param("tenantId") Long tenantId,
                                      @Param("level") String level);

    /**
     * 按等级统计会员数量
     *
     * @param tenantId 租户ID
     * @param level    等级
     * @return 会员数量
     */
    Integer countByLevel(@Param("tenantId") Long tenantId,
                         @Param("level") String level);
}