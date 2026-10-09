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
    @Select("SELECT COUNT(*) FROM group_buy_participation WHERE group_buy_id = #{campaignId} AND status IN " +
            "('JOINED','PAID')")
    int countParticipants(@Param("campaignId") Long campaignId);

    /**
     * 查询进行中的拼团活动
     */
    @Select("SELECT * FROM group_buy_campaign WHERE status = 'OPEN' AND start_time <= #{now} AND end_time >= #{now} " +
            "AND is_deleted = 0")
    java.util.List<GroupBuyCampaign> selectActiveCampaigns(@Param("now") LocalDateTime now);

    /**
     * 加行锁读取活动（SELECT ... FOR UPDATE），供参团临界区串行化使用（P0 超员修复）。
     * <p>
     * group_buy_campaign 无参与人数计数列，无法照 FlashSaleMapper.deductStock 的
     * 「原子 UPDATE 计数 + WHERE 上限」范式落库；改为对活动行加排他锁，同一活动的并发
     * 参团请求在数据库层排队，"人数上限校验 + 防重 + 插入参与记录"在锁保护下原子执行。
     * 必须在事务中调用；tenant_id 条件由 MyBatis-Plus 租户插件自动注入。
     * </p>
     *
     * @param campaignId 拼团活动ID
     * @return 加锁后的活动行（不存在/已删除/跨租户返回 null）
     */
    @Select("SELECT * FROM group_buy_campaign WHERE id = #{campaignId} AND is_deleted = 0 FOR UPDATE")
    GroupBuyCampaign selectByIdForUpdate(@Param("campaignId") Long campaignId);
}
