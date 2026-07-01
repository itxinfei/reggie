package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.CouponTemplate;

public interface CouponTemplateService extends IService<CouponTemplate> {
    boolean claimCoupon(Long memberId, Long templateId);
    boolean useCoupon(Long couponUserId, Long orderId);
    void expireCoupons();
}
