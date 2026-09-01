package com.reggie.module.groupbuy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.groupbuy.model.GroupBuyParticipation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 拼团参与记录 Mapper 接口
 * </p>
 *
 * @author reggie
 * @since 2026-09-01
 */
@Mapper
public interface GroupBuyParticipationMapper extends BaseMapper<GroupBuyParticipation> {

    @Select("SELECT COUNT(1) FROM group_buy_participation WHERE group_buy_id = #{campaignId} AND status IN ('JOINED','PAID')")
    int countParticipants(@Param("campaignId") Long campaignId);
}
