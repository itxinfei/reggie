package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.Member;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 会员管理服务接口
 * </p>
 * <p>提供会员注册、余额扣减、积分增加、折扣计算等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface MemberService extends IService<Member> {

    /**
     * 手机号注册会员
     *
     * @param phone 手机号
     * @param name  会员姓名
     * @return 会员信息
     */
    Member registerByPhone(String phone, String name);

    /**
     * 扣减会员余额
     *
     * @param memberId 会员ID
     * @param amount   扣减金额
     * @return 是否扣减成功
     */
    boolean deductBalance(Long memberId, BigDecimal amount);

    /**
     * 增加会员积分
     *
     * @param memberId 会员ID
     * @param points   积分数量
     * @param bizType  业务类型
     * @param bizId    关联业务ID
     */
    void addPoints(Long memberId, int points, String bizType, Long bizId);

    /**
     * 回退会员积分（拒单/取消时调用）
     * <p>扣除已发放积分并写入一条 OUT 类型流水，积分不低于 0。</p>
     *
     * @param memberId 会员ID
     * @param points   扣减积分数（正数）
     * @param bizType  业务类型（与发放时一致，便于对账）
     * @param bizId    关联业务ID
     */
    void deductPoints(Long memberId, int points, String bizType, Long bizId);

    /**
     * 批量填充会员等级名称（逻辑字段 levelName，不落库）
     * @param members 会员列表
     */
    void fillLevelName(List<Member> members);

    /**
     * 根据用户ID（C端用户）查询关联会员
     *
     * @param userId 用户ID
     * @return 会员信息，未关联返回 null
     */
    Member getByUserId(Long userId);

    /**
     * 计算会员折扣后金额
     *
     * @param memberId 会员ID
     * @param amount   原金额
     * @return 折扣后金额（无等级或无折扣率则返回原金额）
     */
    BigDecimal calculateDiscount(Long memberId, BigDecimal amount);

    /**
     * 按等级统计会员数量明细
     * <p>域4 改造：从 MemberController 下沉，Controller 不再直接操作 Mapper</p>
     *
     * @return 每组 {levelId, cnt}，levelId 为 null 表示无等级
     */
    List<Map<String, Object>> countByLevel();
}
