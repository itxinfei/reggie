package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.CouponTemplate;

/**
 * 优惠券模板服务接口
 * 提供优惠券领取、使用、过期清理等功能
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface CouponTemplateService extends IService<CouponTemplate> {

    /**
     * 领取优惠券
     *
     * @param memberId   会员ID
     * @param templateId 优惠券模板ID
     * @return 是否领取成功
     */
    boolean claimCoupon(Long memberId, Long templateId);

    /**
     * 使用优惠券
     *
     * @param couponUserId 用户优惠券ID
     * @param orderId      订单ID
     * @return 是否使用成功
     */
    boolean useCoupon(Long couponUserId, Long orderId);

    /**
     * 批量清理过期优惠券
     */
    void expireCoupons();
}
