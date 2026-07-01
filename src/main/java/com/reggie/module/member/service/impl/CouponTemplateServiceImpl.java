package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.member.mapper.CouponTemplateMapper;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.CouponUser;
import com.reggie.enums.CouponStatus;
import com.reggie.module.member.service.CouponTemplateService;
import com.reggie.module.member.service.CouponUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate> implements CouponTemplateService {

    @Autowired
    private CouponUserService couponUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimCoupon(Long memberId, Long templateId) {
        CouponTemplate template = getById(templateId);
        if (template == null || template.getStatus() != 1) {
            return false;
        }
        if (template.getRemainCount() <= 0) {
            return false;
        }

        template.setRemainCount(template.getRemainCount() - 1);
        updateById(template);

        CouponUser couponUser = new CouponUser();
        couponUser.setMemberId(memberId);
        couponUser.setTemplateId(templateId);
        couponUser.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        couponUser.setStatus(CouponStatus.UNUSED.getValue());
        if (template.getValidDays() != null) {
            couponUser.setExpireTime(LocalDateTime.now().plusDays(template.getValidDays()));
        }
        couponUserService.save(couponUser);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useCoupon(Long couponUserId, Long orderId) {
        CouponUser couponUser = couponUserService.getById(couponUserId);
        if (couponUser == null || !CouponStatus.UNUSED.getValue().equals(couponUser.getStatus())) {
            return false;
        }
        couponUser.setStatus(CouponStatus.USED.getValue());
        couponUser.setUsedTime(LocalDateTime.now());
        couponUser.setOrderId(orderId);
        couponUserService.updateById(couponUser);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void expireCoupons() {
        LambdaQueryWrapper<CouponUser> qw = new LambdaQueryWrapper<>();
        qw.eq(CouponUser::getStatus, CouponStatus.UNUSED.getValue());
        qw.lt(CouponUser::getExpireTime, LocalDateTime.now());
        List<CouponUser> expiredList = couponUserService.list(qw);
        for (CouponUser cu : expiredList) {
            cu.setStatus(CouponStatus.EXPIRED.getValue());
        }
        couponUserService.updateBatchById(expiredList);
    }
}
