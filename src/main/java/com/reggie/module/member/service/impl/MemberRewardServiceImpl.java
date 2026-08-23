package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.enums.PointsRecordType;
import com.reggie.module.member.mapper.MemberMapper;
import com.reggie.module.member.model.CouponUser;
import com.reggie.module.member.model.Member;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.CouponUserService;
import com.reggie.module.member.service.MemberRewardService;
import com.reggie.module.member.service.MemberService;
import com.reggie.module.member.service.PointsRecordService;
import com.reggie.module.order.model.Orders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 会员权益结算服务实现
 * <p>订单成交后统一发放积分、核销优惠券、扣减储值。</p>
 *
 * @author reggie
 * @since 2026-08-14
 */
@Slf4j
@Service
public class MemberRewardServiceImpl extends ServiceImpl<MemberMapper, Member> implements MemberRewardService {

    /** 积分规则：每消费 1 元获得的积分（固定比例，如需配置化可改为读取配置中心/数据库参数） */
    private static final int POINTS_PER_YUAN = 1;

    @Autowired
    private MemberService memberService;

    @Autowired
    private CouponUserService couponUserService;

    @Autowired
    private PointsRecordService pointsRecordService;

    /**
     * 订单成交后发放会员权益（积分 + 优惠券核销）
     * <p>
     * 设计说明：
     * - 本方法由异步事件监听器 {@link com.reggie.module.member.listener.OrderCompletedListener} 调用，
     *   在独立线程（recommendExecutor）中执行，不绑定订单主事务。
     * - addPoints / useCoupon 各自已有独立 @Transactional，保证单步操作原子性。
     * - 两步之间非事务原子：因异步调用 + 代理绕过（注入实现类），外层 @Transactional 不生效。
     * - 幂等设计（已发放则跳过）保证重复调用安全；部分失败通过日志告警 + 补偿机制处理。
     * - 若 addPoints 成功但 useCoupon 失败，系统处于不一致状态（已发积分但未核销券），
     *   需依赖补偿任务或对账修复。当前设计接受此风险（异步场景）。
     * </p>
     */
    @Override
    public void grantReward(Orders order) {
        if (order == null || order.getUserId() == null) {
            return;
        }
        Long userId = order.getUserId();

        // 1. 积分发放：按实收金额（amount）计算，整取积分（幂等：同一订单仅发放一次）
        try {
            boolean alreadyGranted = pointsRecordService.lambdaQuery()
                    .eq(PointsRecord::getBizType, "ORDER")
                    .eq(PointsRecord::getBizId, order.getId())
                    .count() > 0;
            if (alreadyGranted) {
                log.info("[会员权益] 订单{}积分已发放，跳过重复发放", order.getId());
            } else {
                Member member = memberService.getByUserId(userId);
                if (member != null && order.getAmount() != null
                        && order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                    int points = order.getAmount().setScale(0, RoundingMode.FLOOR).intValue() * POINTS_PER_YUAN;
                    if (points > 0) {
                        memberService.addPoints(member.getId(), points, "ORDER", order.getId());
                        log.info("[会员权益] 订单{}发放积分{}给用户{}会员{}", order.getId(), points, userId, member.getId());
                    }
                }
            }
        } catch (Exception e) {
            // 积分发放失败不影响主交易流程
            log.error("[会员权益] 订单" + order.getId() + " 积分发放失败: " + e.getMessage());
        }

        // 2. 优惠券核销：仅当本单记录了使用的优惠券时核销
        if (order.getUsedCouponId() != null) {
            try {
                boolean ok = couponUserService.useCoupon(userId, order.getUsedCouponId(), order.getId());
                log.info("[会员权益] 订单{}核销优惠券{}结果={}", order.getId(), order.getUsedCouponId(), ok);
            } catch (Exception e) {
                log.error("[会员权益] 订单" + order.getId() + " 优惠券核销失败: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean deductStoredBalance(Long userId, BigDecimal amount) {
        if (userId == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        Member member = memberService.getByUserId(userId);
        if (member == null) {
            return false;
        }
        // 租户归属校验：仅允许扣减当前租户会员余额，防止越权跨租户盗扣
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (currentTenantId != null && !currentTenantId.equals(member.getTenantId())) {
            log.warn("[储值扣减] 跨租户越权拦截: userId={}, memberTenant={}, curTenant={}",
                    userId, member.getTenantId(), currentTenantId);
            return false;
        }
        return memberService.deductBalance(member.getId(), amount);
    }

    /**
     * 订单取消/拒单后回退会员权益（积分回退 + 优惠券恢复）
     * <p>
     * 设计说明：与 {@link #grantReward} 一致，异步调用不绑定订单主事务。
     * 两步（deductPoints + restoreCoupon）各自独立 @Transactional，
     * 中间状态不一致通过幂等设计 + 补偿任务处理。
     * </p>
     */
    @Override
    public void reverseRewards(Long orderId, Long tenantId) {
        if (orderId == null) {
            return;
        }
        // 幂等：先查本单是否已发放积分，未发放则视为无需回退
        // 优先使用传入的 tenantId（异步线程中 BaseContext 可能为 null），保证查询不被跨租户污染
        LambdaQueryWrapper<PointsRecord> pointsRecordWrapper = new LambdaQueryWrapper<>();
        pointsRecordWrapper.eq(PointsRecord::getBizType, "ORDER")
                .eq(PointsRecord::getBizId, orderId)
                .eq(PointsRecord::getType, PointsRecordType.IN.getValue());
        if (tenantId != null) {
            pointsRecordWrapper.eq(PointsRecord::getTenantId, tenantId);
        } else {
            Long currentTenantId = BaseContext.getCurrentTenantId();
            pointsRecordWrapper.eq(currentTenantId != null, PointsRecord::getTenantId, currentTenantId);
        }
        PointsRecord granted = pointsRecordService.getOne(pointsRecordWrapper);
        if (granted == null) {
            log.info("[会员权益回退] 订单{}无已发放积分，跳过回退", orderId);
            return;
        }
        try {
            if (granted.getMemberId() != null) {
                memberService.deductPoints(granted.getMemberId(), granted.getPoints(), "ORDER_REVERSE", orderId);
                log.info("[会员权益回退] 订单{}回退积分{}", orderId, granted.getPoints());
            }
        } catch (Exception e) {
            log.error("[会员权益回退] 订单" + orderId + " 积分回退失败: " + e.getMessage());
        }

        // 回退已核销优惠券：根据订单号反查本单使用的券（used + order_id 一致）
        LambdaQueryWrapper<CouponUser> couponWrapper = new LambdaQueryWrapper<>();
        couponWrapper.eq(CouponUser::getOrderId, orderId)
                .eq(CouponUser::getStatus, "used");
        if (tenantId != null) {
            couponWrapper.eq(CouponUser::getTenantId, tenantId);
        } else {
            Long currentTenantId = BaseContext.getCurrentTenantId();
            couponWrapper.eq(currentTenantId != null, CouponUser::getTenantId, currentTenantId);
        }
        try {
            CouponUser usedCoupon = couponUserService.getOne(couponWrapper);
            if (usedCoupon != null) {
                boolean ok = couponUserService.restoreCoupon(usedCoupon.getId(), orderId);
                log.info("[会员权益回退] 订单{}恢复优惠券{}结果={}", orderId, usedCoupon.getId(), ok);
            }
        } catch (Exception e) {
            log.error("[会员权益回退] 订单" + orderId + " 优惠券恢复失败: " + e.getMessage());
        }
    }
}
