package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 用户 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface UserMapper extends BaseMapper<User>{

    /**
     * 按门店(tenant_id)聚合指定时间区间内的新增用户数（总部控制台用）
     * <p>仅在超管视图（tenantId 上下文为空）下跨门店返回全部分组；用于替代逐店 N+1 查询</p>
     *
     * @param start 起始时间（含）
     * @param end   结束时间（不含）
     * @return 每个 tenantId 的 newUsers
     */
    @Select("SELECT tenant_id AS tenantId, COUNT(*) AS newUsers "
            + "FROM `user` WHERE create_time >= #{start} AND create_time < #{end} "
            + "GROUP BY tenant_id")
    List<Map<String, Object>> statNewUsersByTenant(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);
}
