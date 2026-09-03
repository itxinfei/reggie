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

    /**
     * 统计已支付参与人数（仅 PAID），用于成团判定。
     * 成团以"实际付款人数"为准，JOINED 未付款不计入，避免未支付人数撑起虚假成团。
     */
    @Select("SELECT COUNT(1) FROM group_buy_participation WHERE group_buy_id = #{campaignId} AND status = 'PAID'")
    int countPaidParticipants(@Param("campaignId") Long campaignId);
}
