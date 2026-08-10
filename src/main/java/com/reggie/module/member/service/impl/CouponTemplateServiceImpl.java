package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.member.mapper.CouponTemplateMapper;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.CouponUser;
import com.reggie.enums.CouponStatus;
import com.reggie.module.member.service.CouponTemplateService;
import com.reggie.module.member.service.CouponUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 优惠券模板服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class CouponTemplateServiceImpl extends ServiceImpl<CouponTemplateMapper, CouponTemplate> implements CouponTemplateService {

    /** 用户优惠券服务 */
    @Autowired
    private CouponUserService couponUserService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean claimCoupon(Long memberId, Long templateId) {
        CouponTemplate template = getById(templateId);
        if (template == null || template.getStatus() != 1) {
            return false;
        }

        // SQL 原子扣减：remain_count = remain_count - 1，WHERE remain_count > 0 防止超发
        LambdaUpdateWrapper<CouponTemplate> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CouponTemplate::getId, templateId);
        wrapper.gt(CouponTemplate::getRemainCount, 0);
        wrapper.setSql("remain_count = remain_count - 1");
        boolean deducted = update(wrapper);

        if (!deducted) {
            return false; // 库存不足，领取失败
        }

        // 修改点：移除 check-then-act 防重复校验（并发下存在 TOCTOU 漏洞），
        // 改为依赖 coupon_user 表的 uk_member_template 唯一索引保证幂等；
        // 插入冲突时捕获 DuplicateKeyException，回滚 remain_count 并返回 false
        CouponUser couponUser = new CouponUser();
        couponUser.setMemberId(memberId);
        couponUser.setTemplateId(templateId);
        couponUser.setCode(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        couponUser.setStatus(CouponStatus.UNUSED.getValue());
        if (template.getValidDays() != null) {
            couponUser.setExpireTime(LocalDateTime.now().plusDays(template.getValidDays()));
        }
        try {
            couponUserService.save(couponUser);
        } catch (DuplicateKeyException e) {
            // 已领取过（唯一索引冲突），回滚已扣减的库存
            LambdaUpdateWrapper<CouponTemplate> rollbackWrapper = new LambdaUpdateWrapper<>();
            rollbackWrapper.eq(CouponTemplate::getId, templateId);
            rollbackWrapper.setSql("remain_count = remain_count + 1");
            update(rollbackWrapper);
            return false;
        }
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

    @Override
    public Map<String, Object> getStats() {
        // 修改点：后端聚合替代前端 pageSize=1000 拉全量；仅查询所需三列，租户条件由拦截器自动注入
        LambdaQueryWrapper<CouponTemplate> qw = new LambdaQueryWrapper<>();
        qw.select(CouponTemplate::getStatus, CouponTemplate::getTotalCount, CouponTemplate::getRemainCount);
        List<CouponTemplate> list = list(qw);

        long totalCoupons = list.size();
        long enabledCount = list.stream().filter(c -> c.getStatus() != null && c.getStatus() == 1).count();
        long disabledCount = list.stream().filter(c -> c.getStatus() != null && c.getStatus() == 0).count();
        long exhaustedCount = list.stream()
                .filter(c -> c.getRemainCount() != null && c.getRemainCount() <= 0).count();
        long totalIssued = list.stream()
                .filter(c -> c.getTotalCount() != null)
                .mapToLong(CouponTemplate::getTotalCount)
                .sum();
        long totalClaimed = list.stream().mapToLong(c -> {
            int total = c.getTotalCount() != null ? c.getTotalCount() : 0;
            int remain = c.getRemainCount() != null ? c.getRemainCount() : 0;
            return Math.max(0, total - remain);
        }).sum();
        String usageRate = totalIssued > 0
                ? String.format("%.1f%%", totalClaimed * 100.0 / totalIssued)
                : "0%";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalCoupons", totalCoupons);
        result.put("enabledCount", enabledCount);
        result.put("disabledCount", disabledCount);
        result.put("exhaustedCount", exhaustedCount);
        result.put("claimedCount", totalClaimed);
        result.put("usageRate", usageRate);
        return result;
    }
}