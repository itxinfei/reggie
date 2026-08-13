package com.reggie.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.reggie.module.user.model.User;
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
     * 按手机号查询用户（跨租户，供匿名登录使用）
     * <p>
     * 用户登录（/user/login）是公开端点，LoginCheckFilter 放行时不会设置租户上下文，
     * 若走租户插件会注入 {@code tenant_id = -1} 导致永远查不到用户、登录失败。
     * 登录前尚不知用户归属哪个租户，必须跳过租户过滤、按手机号全局匹配，
     * 登录成功后租户上下文由用户自身 tenantId 恢复。
     * </p>
     *
     * @param phone 手机号
     * @return 用户信息，不存在则返回 null
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM `user` WHERE phone = #{phone} ORDER BY id ASC LIMIT 1")
    User selectByPhoneIgnoreTenant(@Param("phone") String phone);

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

