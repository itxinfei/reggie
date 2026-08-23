package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.CouponAvailableDTO;
import com.reggie.module.member.model.CouponUser;

import java.math.BigDecimal;
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
     * 核销优惠券（标记为已使用，记录订单与用券时间）
     * <p>仅当优惠券属于该用户且处于未使用、未过期状态时才可核销。</p>
     *
     * @param userId   用户ID（与 coupon_user.member_id 一致）
     * @param couponId 用户优惠券ID
     * @param orderId  关联订单ID
     * @return 是否核销成功
     */
    boolean useCoupon(Long userId, Long couponId, Long orderId);

    /**
     * 查询会员在当前订单金额下可使用的优惠券列表
     * <p>仅返回未使用、未过期、且满足使用门槛（满额条件）的优惠券，并附带针对当前订单的可抵扣金额。</p>
     *
     * @param userId      用户ID（与 coupon_user.member_id 一致）
     * @param orderAmount 当前订单应付金额
     * @return 可用优惠券展示列表（按可抵扣金额降序）
     */
    List<CouponAvailableDTO> availableCoupons(Long userId, BigDecimal orderAmount);

    /**
     * 恢复已核销的优惠券（拒单/取消时回退）
     * <p>仅当该券当前处于已使用状态且关联订单与入参一致时才恢复为未使用，并清空用券时间与订单关联。</p>
     *
     * @param couponId 用户优惠券ID
     * @param orderId  关联订单ID
     * @return 是否恢复成功
     */
    boolean restoreCoupon(Long couponId, Long orderId);
}
