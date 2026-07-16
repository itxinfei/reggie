package com.reggie.module.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.member.model.CouponTemplate;
import java.util.Map;

/**
 * <p>
 * 优惠券模板服务接口
 * </p>
 * <p>提供优惠券领取、使用、过期清理等功能</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
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

    /**
     * 优惠券模板统计：总数、启用/禁用/已领完数量、累计发放/领取数、使用率
     * 修改点：替代前端 pageSize=1000 拉全量后在浏览器聚合的统计方式，改为后端聚合
     * @return 统计结果 Map
     */
    Map<String, Object> getStats();
}
