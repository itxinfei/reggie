package com.reggie.module.schedule.task;

import com.reggie.module.member.service.CouponTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：自动将过期未使用的优惠券标记为已过期
 *
 * @author 心飞为你飞
 * @since 2026-08-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponExpirationTask {

    private final CouponTemplateService couponTemplateService;

    /**
     * 每小时检查一次过期优惠券
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void expireUnusedCoupons() {
        log.info("开始执行优惠券过期检查定时任务");
        try {
            couponTemplateService.expireCoupons();
            log.info("优惠券过期检查完成");
        } catch (Exception e) {
            log.error("优惠券过期检查异常", e);
        }
    }
}
