package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.module.member.mapper.CouponUserMapper;
import com.reggie.module.member.model.CouponAvailableDTO;
import com.reggie.module.member.model.CouponTemplate;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.service.CouponTemplateService;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.CouponUserService;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class CouponUserServiceImpl extends ServiceImpl<CouponUserMapper, CouponUser> implements CouponUserService {

    // 修改点：注入优惠券模板服务，用于选券场景关联模板信息与计算可抵扣金额
    // 注意：必须用字段注入而非构造器注入——CouponTemplateServiceImpl 又字段注入了本服务，
    // 构造器注入会形成无法解析的循环依赖（BeanCurrentlyInCreation），导致应用启动失败。
    @Autowired
    private CouponTemplateService couponTemplateService;

    /**
     * 会员服务（修改点 2026-09-18）：用于把调用方传入的 user.id 反查成会员主键 member.id。
     * 同模块依赖，且与 CouponTemplateService 一样采用字段注入，避免构造器循环依赖。
     */
    @Autowired(required = false)
    private MemberService memberService;

    /**
     * 处理 use coupon。
     * @param userId 参数 userId
     * @param couponId 参数 couponId
     * @param orderId 参数 orderId
     * @return 返回结果
     */
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

    /**
     * 修改点(2026-09-18)：按 user.id 反查会员主键 member.id（券归属按会员主键存储）。
     *
     * @param userId 用户ID
     * @return 会员主键；未找到或服务不可用时返回 null
     */
    private Long resolveMemberIdByUserId(Long userId) {
        if (userId == null || memberService == null) {
            return null;
        }
        try {
            Member member = memberService.getByUserId(userId);
            return member != null ? member.getId() : null;
        } catch (Exception e) {
            // 宽异常兜底：有意捕获 Exception，会员查询失败不应影响券列表查询
            log.warn("[优惠券] 会员信息查询失败: userId={}", userId);
            return null;
        }
    }

    /**
     * 处理 available coupons。
     * @param userId 参数 userId
     * @param orderAmount 参数 orderAmount
     * @return 返回结果
     */
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
        // 修改点(2026-09-18)：coupon_user.member_id 存的是「会员主键 member.id」，
        // 但收银台等调用方传的是「user.id」，直接按 userId 匹配会导致券永远查不到
        // （收银台表现：识别会员后「暂无可用券」恒成立）。兼容两种入参：
        // 先按会员主键查，查不到再按 user.id 反查会员主键重试一次。
        if (CollectionUtils.isEmpty(userCoupons)) {
            Long memberId = resolveMemberIdByUserId(userId);
            if (memberId != null && !memberId.equals(userId)) {
                userCoupons = lambdaQuery()
                        .eq(CouponUser::getMemberId, memberId)
                        .eq(CouponUser::getTenantId, BaseContext.getCurrentTenantId())
                        .eq(CouponUser::getStatus, "unused")
                        .list();
            }
        }
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

    /**
     * 处理 restore coupon。
     * @param couponId 参数 couponId
     * @param orderId 参数 orderId
     * @return 返回结果
     */
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


