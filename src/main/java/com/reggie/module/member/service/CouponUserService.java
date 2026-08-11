package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.CouponUser;

import java.util.List;

/**
 * <p>
 * 用户优惠券服务接口
 * </p>
 * <p>管理用户已领取的优惠券记录</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface CouponUserService extends IService<CouponUser> {

    /**
     * 查询用户已领取的优惠券列表
     *
     * @param userId 用户ID
     * @param status 使用状态（可选）
     * @return 优惠券列表
     */
    List<CouponUser> listByUserId(Long userId, String status);

    /**
     * 批量更新优惠券状态
     *
     * @param couponIds 优惠券用户记录ID列表
     * @param status    目标状态
     */
    void batchUpdateStatus(List<Long> couponIds, String status);
}
