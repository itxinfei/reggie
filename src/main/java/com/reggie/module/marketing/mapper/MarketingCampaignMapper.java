package com.reggie.module.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.marketing.model.MarketingCampaign;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 营销活动 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface MarketingCampaignMapper extends BaseMapper<MarketingCampaign> {

    /**
     * 分页查询营销活动，附带推送数量（LEFT JOIN subquery）
     *
     * @param tenantId 租户ID
     * @param name 活动名称（模糊搜索，可选）
     * @param status 状态筛选（可选）
     * @param type 活动类型（可选）
     * @param offset 分页偏移
     * @param limit 分页条数
     * @return 每行包含 push_count 字段
     */
    @Select("<script>"
            + "SELECT mc.*, "
            + "  COALESCE(("
            + "    SELECT COUNT(*) FROM marketing_message mm "
            + "    WHERE mm.campaign_id = mc.id"
            + "  ), 0) AS push_count "
            + "FROM marketing_campaign mc "
            + "WHERE mc.tenant_id = #{tenantId} "
            + "<if test='name != null and name != \"\"'>"
            + "  AND mc.name LIKE CONCAT('%', #{name}, '%') "
            + "</if>"
            + "<if test='status != null'>"
            + "  AND mc.status = #{status} "
            + "</if>"
            + "<if test='type != null'>"
            + "  AND mc.campaign_type = #{type} "
            + "</if>"
            + "ORDER BY mc.priority DESC, mc.create_time DESC "
            + "LIMIT #{offset}, #{limit}"
            + "</script>")
    List<Map<String, Object>> selectPageWithPushCount(
            @Param("tenantId") Long tenantId,
            @Param("name") String name,
            @Param("status") Integer status,
            @Param("type") Integer type,
            @Param("offset") int offset,
            @Param("limit") int limit);

    /**
     * 统计符合条件的活动总数（与分页SQL的WHERE条件一致）
     *
     * @param tenantId 租户ID
     * @param name 活动名称
     * @param status 状态
     * @param type 活动类型
     * @return 符合条件的活动总数
     */
    @Select("<script>"
            + "SELECT COUNT(*) FROM marketing_campaign "
            + "WHERE tenant_id = #{tenantId} "
            + "<if test='name != null and name != \"\"'>"
            + "  AND name LIKE CONCAT('%', #{name}, '%') "
            + "</if>"
            + "<if test='status != null'>"
            + "  AND status = #{status} "
            + "</if>"
            + "<if test='type != null'>"
            + "  AND campaign_type = #{type} "
            + "</if>"
            + "</script>")
    long countWithFilter(@Param("tenantId") Long tenantId,
                         @Param("name") String name,
                         @Param("status") Integer status,
                         @Param("type") Integer type);

    /**
     * 获取营销活动全局统计数据
     *
     * @param tenantId 租户ID
     * @return 包含 total/active/draft/ended/paused/totalParticipants/totalPushed 的Map
     */
    @Select("SELECT "
            + "  COUNT(*) AS total, "
            + "  SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS active, "
            + "  SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END) AS draft, "
            + "  SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS ended, "
            + "  SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS paused, "
            + "  COALESCE(SUM(current_participants), 0) AS total_participants, "
            + "  COALESCE((SELECT COUNT(*) FROM marketing_message mm "
            + "    WHERE mm.tenant_id = #{tenantId}), 0) AS total_pushed "
            + "FROM marketing_campaign "
            + "WHERE tenant_id = #{tenantId}")
    Map<String, Object> getCampaignStats(@Param("tenantId") Long tenantId);

    /**
     * 获取单个活动的推送消息数量
     *
     * @param campaignId 活动ID
     * @return 推送消息数量
     */
    @Select("SELECT COUNT(*) FROM marketing_message WHERE campaign_id = #{campaignId}")
    int countPushByCampaignId(@Param("campaignId") Long campaignId);
}

