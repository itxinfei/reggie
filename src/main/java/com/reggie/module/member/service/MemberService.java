package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.Member;
import java.math.BigDecimal;

/**
 * 会员管理服务接口
 * 提供会员注册、余额扣减、积分增加、折扣计算等功能
 *
 * @author reggie
 * @since 2026-07-09
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
     * 根据会员等级计算折扣后金额
     *
     * @param memberId 会员ID
     * @param amount   原始金额
     * @return 折扣后金额
     */
    BigDecimal calculateDiscount(Long memberId, BigDecimal amount);
}
