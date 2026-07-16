package com.reggie.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.member.model.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 会员 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface MemberMapper extends BaseMapper<Member> {

    /**
     * 原子扣减会员余额：balance = balance - #{amount}，WHERE balance >= #{amount} 防扣成负数
     * 修改点：改为参数化 @Update，消除 LambdaUpdateWrapper.setSql 的字符串拼接（违反“禁止拼接 SQL”规范）。
     * tenant_id 由 TenantLineInnerInterceptor 自动注入，无需手动拼接。
     * @param id 会员ID
     * @param amount 扣减金额（正数）
     * @return 受影响行数，0 表示余额不足或会员不存在
     */
    @Update("UPDATE member SET balance = balance - #{amount}, updated_time = NOW() " +
            "WHERE id = #{id} AND balance >= #{amount}")
    int deductBalanceById(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /**
     * 原子增加会员积分：points = IFNULL(points, 0) + #{points}
     * 修改点：改为参数化 @Update，消除 LambdaUpdateWrapper.setSql 的字符串拼接。
     * @param id 会员ID
     * @param points 增加积分数（正数）
     * @return 受影响行数
     */
    @Update("UPDATE member SET points = IFNULL(points, 0) + #{points}, updated_time = NOW() " +
            "WHERE id = #{id}")
    int incrementPointsById(@Param("id") Long id, @Param("points") int points);

    /**
     * 按会员等级统计会员数量：返回 level_id -> 数量 的明细
     * 修改点：用于会员统计看板，替代前端 pageSize=9999 拉全量后在浏览器按等级计数。
     * tenant_id 由 TenantLineInnerInterceptor 自动注入（原生 @Select 同样生效），无需手动拼接。
     * 注意：level_id 为 NULL 的会员会被归并到一组（levelId=null），由调用方单独处理“无等级”计数。
     * @return 每组 {levelId, cnt}
     */
    @Select("SELECT level_id AS levelId, COUNT(*) AS cnt FROM member WHERE 1=1 GROUP BY level_id")
    List<Map<String, Object>> countByLevel();
}
