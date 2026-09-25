package com.reggie.module.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.marketing.model.BuyGetFree;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * Buy Get Free Mapper
 *
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface BuyGetFreeMapper extends BaseMapper<BuyGetFree> {

    /**
     * 原子回写买赠活动使用次数（每个命中订单 +1，不是赠品件数）。
     *
     * @param activityId 买赠活动ID
     * @param delta      增量（每单 1）
     * @return 受影响行数（0=活动不存在/异常）
     */
    @Update("UPDATE buy_get_free SET usage_count = usage_count + #{delta}, " +
            "update_time = CURRENT_TIMESTAMP WHERE id = #{activityId}")
    int incrementUsageCount(@Param("activityId") Long activityId, @Param("delta") int delta);
}
