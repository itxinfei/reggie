package com.reggie.module.groupbuy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.groupbuy.model.GroupBuyCampaign;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * <p>
 * 拼团活动 Mapper 接口
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
@Mapper
public interface GroupBuyCampaignMapper extends BaseMapper<GroupBuyCampaign> {

    /**
     * 统计指定活动当前已参团人数
     */
    @Select("SELECT COUNT(*) FROM group_buy_participation WHERE group_buy_id = #{campaignId} AND status IN ('JOINED','PAID') AND is_deleted = 0")
    int countParticipants(@Param("campaignId") Long campaignId);

    /**
     * 查询进行中的拼团活动
     */
    @Select("SELECT * FROM group_buy_campaign WHERE status = 'OPEN' AND start_time <= #{now} AND end_time >= #{now} AND is_deleted = 0")
    java.util.List<GroupBuyCampaign> selectActiveCampaigns(@Param("now") LocalDateTime now);
}
