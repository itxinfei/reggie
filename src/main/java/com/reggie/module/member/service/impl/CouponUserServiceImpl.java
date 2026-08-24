package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.member.mapper.CouponUserMapper;
import com.reggie.module.member.model.CouponAvailableDTO;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.service.CouponTemplateService;
import com.reggie.module.member.service.CouponUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户优惠券服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class CouponUserServiceImpl extends ServiceImpl<CouponUserMapper, CouponUser> implements CouponUserService {

    // 修改点：注入优惠券模板服务，用于选券场景关联模板信息与计算可抵扣金额
    // 注意：必须用字段注入而非构造器注入——CouponTemplateServiceImpl 又字段注入了本服务，
    // 构造器注入会形成无法解析的循环依赖（BeanCurrentlyInCreation），导致应用启动失败。
    @Autowired
    private CouponTemplateService couponTemplateService;

    @Override
    public boolean useCoupon(Long userId, Long couponId, Long orderId) {
        if (userId == null || couponId == null || orderId == null) {
            return false;
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        // 仅核销属于该用户、属于当前租户、未使用、未过期的优惠券
        CouponUser couponUser = lambdaQuery()
                .eq(CouponUser::getId, couponId)
                .eq(CouponUser::getMemberId, userId)
                .eq(currentTenantId != null, CouponUser::getTenantId, currentTenantId)
                .eq(CouponUser::getStatus, "unused")
                .one();
        if (couponUser == null) {
            return false;
        }
        if (couponUser.getExpireTime() != null
                && couponUser.getExpireTime().isBefore(LocalDateTime.now())) {
            // 修复 P2-6：UPDATE 附加 expire_time < NOW() 条件，防止误标记未过期券
            lambdaUpdate().eq(CouponUser::getId, couponId)
                    .eq(CouponUser::getExpireTime, couponUser.getExpireTime())
                    .lt(CouponUser::getExpireTime, LocalDateTime.now())
                    .set(CouponUser::getStatus, "expired")
                    .update();
            return false;
        }
        LambdaUpdateWrapper<CouponUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CouponUser::getId, couponId)
                .eq(CouponUser::getStatus, "unused")
                .set(CouponUser::getStatus, "used")
                .set(CouponUser::getUsedTime, LocalDateTime.now())
                .set(CouponUser::getOrderId, orderId);
        return update(updateWrapper);
    }

    @Override
    public List<CouponAvailableDTO> availableCoupons(Long userId, BigDecimal orderAmount) {
        List<CouponAvailableDTO> result = new ArrayList<>();
        if (userId == null || orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return result;
        }
        // 仅取未使用、未过期、属于当前租户的优惠券
        LocalDateTime now = LocalDateTime.now();
        List<CouponUser> userCoupons = lambdaQuery()
                .eq(CouponUser::getMemberId, userId)
                .eq(CouponUser::getTenantId, BaseContext.getCurrentTenantId())
                .eq(CouponUser::getStatus, "unused")
                .list();
        if (CollectionUtils.isEmpty(userCoupons)) {
            return result;
        }
        // 批量加载关联模板，避免循环单条查询
        List<Long> templateIds = userCoupons.stream()
                .map(CouponUser::getTemplateId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, CouponTemplate> templateMap = couponTemplateService.listByIds(templateIds)
                .stream()
                .collect(Collectors.toMap(CouponTemplate::getId, t -> t, (a, b) -> a));

        for (CouponUser userCoupon : userCoupons) {
            CouponTemplate template = templateMap.get(userCoupon.getTemplateId());
            if (template == null) {
                continue;
            }
            // 过期过滤
            if (userCoupon.getExpireTime() != null && userCoupon.getExpireTime().isBefore(now)) {
                continue;
            }
            // 门槛过滤：订单金额需达到满额条件
            BigDecimal conditionAmount = template.getConditionAmount() == null
                    ? BigDecimal.ZERO : template.getConditionAmount();
            if (orderAmount.compareTo(conditionAmount) < 0) {
                continue;
            }
            BigDecimal currentDiscount = computeDiscount(template, orderAmount);
            if (currentDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            CouponAvailableDTO dto = new CouponAvailableDTO();
            BeanUtils.copyProperties(template, dto);
            dto.setId(userCoupon.getId());
            dto.setCurrentDiscount(currentDiscount);
            result.add(dto);
        }
        // 按可抵扣金额降序，便于收银台优先推荐
        result.sort(Comparator.comparing(CouponAvailableDTO::getCurrentDiscount).reversed());
        return result;
    }

    /**
     * 根据优惠券模板与订单金额计算可抵扣金额
     *
     * @param template    优惠券模板
     * @param orderAmount 订单金额
     * @return 可抵扣金额（不小于 0）
     */
    private BigDecimal computeDiscount(CouponTemplate template, BigDecimal orderAmount) {
        if ("DISCOUNT".equals(template.getType()) && template.getDiscountRate() != null) {
            // 折扣券：订单金额 * (1 - 折扣率)
            BigDecimal rate = template.getDiscountRate();
            if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                return BigDecimal.ZERO;
            }
            BigDecimal discount = orderAmount.multiply(BigDecimal.ONE.subtract(rate))
                    .setScale(2, RoundingMode.HALF_UP);
            return discount;
        }
        // 满减券/代金券：直接取满减金额
        BigDecimal discountAmount = template.getDiscountAmount() == null
                ? BigDecimal.ZERO : template.getDiscountAmount();
        return discountAmount.compareTo(orderAmount) > 0 ? orderAmount : discountAmount;
    }

    @Override
    public boolean restoreCoupon(Long couponId, Long orderId) {
        if (couponId == null || orderId == null) {
            return false;
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        // 仅当券当前为已使用、属于当前租户、且关联订单一致时恢复，避免跨租户误恢复或重复恢复
        LambdaUpdateWrapper<CouponUser> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CouponUser::getId, couponId)
                .eq(CouponUser::getOrderId, orderId)
                .eq(CouponUser::getStatus, "used")
                .eq(currentTenantId != null, CouponUser::getTenantId, currentTenantId)
                .set(CouponUser::getStatus, "unused")
                .set(CouponUser::getUsedTime, null)
                .set(CouponUser::getOrderId, null);
        return update(updateWrapper);
    }
}


