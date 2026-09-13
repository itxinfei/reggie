package com.reggie.module.billing.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.billing.dto.SubscribeDTO;
import com.reggie.module.billing.mapper.TenantSubscriptionMapper;
import com.reggie.module.billing.model.BillingPlan;
import com.reggie.module.billing.model.TenantSubscription;
import com.reggie.module.billing.service.BillingPlanService;
import com.reggie.module.billing.service.TenantSubscriptionService;
import com.reggie.module.billing.vo.SubscriptionEntitlementVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 租户订阅计费服务实现
 *
 * @author reggie
 * @since 2026-09-12
 */
@Slf4j
@Service
public class TenantSubscriptionServiceImpl
        extends ServiceImpl<TenantSubscriptionMapper, TenantSubscription>
        implements TenantSubscriptionService {

    /** 订阅单号时间格式 */
    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /** 默认支付渠道：模拟开通 */
    private static final String DEFAULT_PAY_CHANNEL = "MOCK";
    /** 不限额度标记 */
    private static final int UNLIMITED = -1;

    @Autowired
    private BillingPlanService billingPlanService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantSubscription subscribe(SubscribeDTO dto) {
        Long tenantId = requireTenant();
        if (dto == null || dto.getPlanId() == null) {
            throw new CustomException("套餐ID不能为空");
        }
        BillingPlan plan = billingPlanService.getById(dto.getPlanId());
        if (plan == null) {
            throw new CustomException("套餐不存在");
        }
        if (!Integer.valueOf(BillingPlan.STATUS_ON).equals(plan.getStatus())) {
            throw new CustomException("该套餐已下架，无法订阅");
        }
        int cycle = normalizeCycle(dto.getBillingCycle());
        BigDecimal amount = calculateAmount(plan, cycle);

        TenantSubscription sub = new TenantSubscription();
        sub.setTenantId(tenantId);
        sub.setPlanId(plan.getId());
        sub.setPlanCode(plan.getPlanCode());
        sub.setPlanName(plan.getPlanName());
        sub.setMaxStores(plan.getMaxStores());
        sub.setMaxEmployees(plan.getMaxEmployees());
        sub.setOrderNo(generateOrderNo());
        sub.setBillingCycle(cycle);
        sub.setAmount(amount);
        sub.setStatus(TenantSubscription.STATUS_PENDING);
        sub.setPayChannel(dto.getPayChannel() == null || dto.getPayChannel().trim().isEmpty()
                ? DEFAULT_PAY_CHANNEL : dto.getPayChannel().trim());
        sub.setRemark(dto.getRemark());
        save(sub);
        log.info("[SaaS计费] 生成订阅单 tenant={}, orderNo={}, plan={}, amount={}",
                tenantId, sub.getOrderNo(), plan.getPlanCode(), amount);
        return sub;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TenantSubscription markPaid(Long id) {
        TenantSubscription sub = getOwnedSubscription(id);
        if (!Integer.valueOf(TenantSubscription.STATUS_PENDING).equals(sub.getStatus())) {
            throw new CustomException("仅待支付订阅单可确认支付");
        }
        LocalDateTime now = LocalDateTime.now();

        // 续费叠加：若当前已有未到期的有效订阅，则在其到期时间基础上顺延，避免剩余天数浪费
        LocalDateTime baseStart = now;
        LocalDateTime baseEnd = now;
        TenantSubscription current = findActiveSubscription(now);
        if (current != null && current.getEndTime() != null && current.getEndTime().isAfter(now)) {
            baseEnd = current.getEndTime();
            log.info("[SaaS计费] 续费叠加：在旧订阅 orderNo={} 到期时间 {} 基础上顺延",
                    current.getOrderNo(), baseEnd);
        }

        LocalDateTime endTime = plusCycle(baseEnd, sub.getBillingCycle());
        sub.setStatus(TenantSubscription.STATUS_ACTIVE);
        sub.setPayTime(now);
        sub.setStartTime(baseStart);
        sub.setEndTime(endTime);
        updateById(sub);
        log.info("[SaaS计费] 订阅开通成功 tenant={}, orderNo={}, 有效期至 {}",
                sub.getTenantId(), sub.getOrderNo(), endTime);
        return sub;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        TenantSubscription sub = getOwnedSubscription(id);
        if (!Integer.valueOf(TenantSubscription.STATUS_PENDING).equals(sub.getStatus())) {
            throw new CustomException("仅待支付订阅单可取消");
        }
        sub.setStatus(TenantSubscription.STATUS_CANCELED);
        updateById(sub);
        log.info("[SaaS计费] 取消订阅单 orderNo={}", sub.getOrderNo());
    }

    @Override
    public TenantSubscription getActiveSubscription() {
        return findActiveSubscription(LocalDateTime.now());
    }

    @Override
    public SubscriptionEntitlementVO getEntitlement() {
        return SubscriptionEntitlementVO.from(getActiveSubscription(), LocalDateTime.now());
    }

    @Override
    public Page<TenantSubscription> pageMy(int page, int pageSize, Integer status) {
        requireTenant();
        Page<TenantSubscription> p = PageUtils.of(page, pageSize);
        return lambdaQuery()
                .eq(status != null, TenantSubscription::getStatus, status)
                .orderByDesc(TenantSubscription::getId)
                .page(p);
    }

    @Override
    public Page<TenantSubscription> adminPage(int page, int pageSize, Integer status, Long tenantId) {
        Page<TenantSubscription> p = PageUtils.of(page, pageSize);
        return (Page<TenantSubscription>) baseMapper.adminPage(p, status, tenantId);
    }

    @Override
    public int expireOverdue() {
        int n = baseMapper.expireOverdueBatch(LocalDateTime.now(),
                TenantSubscription.STATUS_ACTIVE, TenantSubscription.STATUS_EXPIRED);
        if (n > 0) {
            log.info("[SaaS计费] 过期扫描处理订阅记录 {} 条", n);
        }
        return n;
    }

    @Override
    public boolean checkStoreLimit(int currentStoreCount) {
        TenantSubscription active = getActiveSubscription();
        if (active == null || active.getMaxStores() == null) {
            return false;
        }
        if (active.getMaxStores() == UNLIMITED) {
            return true;
        }
        return currentStoreCount <= active.getMaxStores();
    }

    @Override
    public boolean checkEmployeeLimit(int currentEmployeeCount) {
        TenantSubscription active = getActiveSubscription();
        if (active == null || active.getMaxEmployees() == null) {
            return false;
        }
        if (active.getMaxEmployees() == UNLIMITED) {
            return true;
        }
        return currentEmployeeCount <= active.getMaxEmployees();
    }

    @Override
    public BigDecimal calculateAmount(BillingPlan plan, int cycle) {
        if (plan == null || plan.getPrice() == null) {
            throw new CustomException("套餐价格未配置");
        }
        int c = normalizeCycle(cycle);
        if (c == TenantSubscription.CYCLE_YEAR) {
            return plan.getAnnualPrice() != null ? plan.getAnnualPrice() : plan.getPrice().multiply(new BigDecimal("12"));
        }
        return plan.getPrice();
    }

    // ============================ 私有辅助方法 ============================

    /**
     * 获取当前租户上下文，缺失则拒绝（订阅属于租户自身行为）。
     *
     * @return 当前租户ID
     */
    private Long requireTenant() {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文缺失，无法操作订阅");
        }
        return tenantId;
    }

    /**
     * 校验计费周期合法并返回规范化值。
     *
     * @param cycle 入参周期
     * @return 合法周期
     */
    private int normalizeCycle(Integer cycle) {
        if (cycle == null || (cycle != TenantSubscription.CYCLE_MONTH && cycle != TenantSubscription.CYCLE_YEAR)) {
            throw new CustomException("非法的计费周期（1=月付，2=年付）");
        }
        return cycle;
    }

    /**
     * 读取订阅并校验归属当前租户。
     *
     * @param id 订阅ID
     * @return 订阅记录
     */
    private TenantSubscription getOwnedSubscription(Long id) {
        if (id == null) {
            throw new CustomException("订阅记录ID不能为空");
        }
        TenantSubscription sub = getById(id);
        if (sub == null) {
            throw new CustomException("订阅记录不存在");
        }
        Long currentTenant = requireTenant();
        if (!currentTenant.equals(sub.getTenantId())) {
            throw new CustomException("无权操作其他租户的订阅记录");
        }
        return sub;
    }

    /**
     * 查找指定时刻当前租户的有效订阅：生效中、时间窗口命中，取到期时间最晚的一条。
     *
     * @param now 当前时刻
     * @return 有效订阅，无则 null
     */
    private TenantSubscription findActiveSubscription(LocalDateTime now) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(TenantSubscription::getTenantId, tenantId)
                .eq(TenantSubscription::getStatus, TenantSubscription.STATUS_ACTIVE)
                .le(TenantSubscription::getStartTime, now)
                .gt(TenantSubscription::getEndTime, now)
                .orderByDesc(TenantSubscription::getEndTime)
                .last("LIMIT 1")
                .one();
    }

    /**
     * 在基准时间上叠加一个计费周期。
     *
     * @param base  基准时间
     * @param cycle 计费周期
     * @return 到期时间
     */
    private LocalDateTime plusCycle(LocalDateTime base, int cycle) {
        if (cycle == TenantSubscription.CYCLE_YEAR) {
            return base.plusYears(1);
        }
        return base.plusMonths(1);
    }

    /**
     * 生成订阅单号：SUB + 14位时间 + 3位随机数。
     *
     * @return 单号
     */
    private String generateOrderNo() {
        return "SUB" + LocalDateTime.now().format(ORDER_NO_FMT)
                + String.format("%03d", ThreadLocalRandom.current().nextInt(1000));
    }
}
