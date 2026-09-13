package com.reggie.module.billing.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.billing.dto.SubscribeDTO;
import com.reggie.module.billing.model.BillingPlan;
import com.reggie.module.billing.model.TenantSubscription;
import com.reggie.module.billing.vo.SubscriptionEntitlementVO;

import java.math.BigDecimal;

/**
 * 租户订阅计费服务
 *
 * @author reggie
 * @since 2026-09-12
 */
public interface TenantSubscriptionService extends IService<TenantSubscription> {

    /**
     * 发起订阅/续费：校验套餐已上架，按周期计价，生成待支付订阅单。
     *
     * @param dto 订阅请求
     * @return 待支付订阅记录
     */
    TenantSubscription subscribe(SubscribeDTO dto);

    /**
     * 确认支付并开通（待支付 → 生效中）。续费场景下权益时间在当前到期时间之后顺延，不浪费剩余天数。
     *
     * @param id 订阅记录ID
     * @return 生效后的订阅记录
     */
    TenantSubscription markPaid(Long id);

    /**
     * 取消待支付订阅单（仅待支付可取消）。
     *
     * @param id 订阅记录ID
     */
    void cancel(Long id);

    /**
     * 获取当前租户的有效订阅（生效中且未到期，取到期时间最晚的一条）。
     *
     * @return 有效订阅，无则返回 null
     */
    TenantSubscription getActiveSubscription();

    /**
     * 获取当前租户的权益视图。
     *
     * @return 权益视图
     */
    SubscriptionEntitlementVO getEntitlement();

    /**
     * 分页查询当前租户自己的订阅记录。
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   状态（可选）
     * @return 分页结果
     */
    Page<TenantSubscription> pageMy(int page, int pageSize, Integer status);

    /**
     * 平台运营跨租户分页查询全部订阅记录。
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   状态（可选）
     * @param tenantId 租户ID（可选）
     * @return 分页结果
     */
    Page<TenantSubscription> adminPage(int page, int pageSize, Integer status, Long tenantId);

    /**
     * 把已过到期时间但仍处于"生效中"的订阅批量置为"已过期"。
     *
     * @return 本次处理的记录条数
     */
    int expireOverdue();

    /**
     * 校验当前租户门店数是否在套餐额度内（不限额度或无有效订阅时的策略由调用方决定，本方法只做额度比较）。
     *
     * @param currentStoreCount 当前门店数
     * @return true=未超限
     */
    boolean checkStoreLimit(int currentStoreCount);

    /**
     * 校验当前租户员工数是否在套餐额度内。
     *
     * @param currentEmployeeCount 当前员工数
     * @return true=未超限
     */
    boolean checkEmployeeLimit(int currentEmployeeCount);

    /**
     * 按计费周期计算应付金额：月付取月价；年付取年价，年价为空时按月价×12。
     *
     * @param plan  套餐
     * @param cycle 计费周期：1=月付，2=年付
     * @return 金额
     */
    BigDecimal calculateAmount(BillingPlan plan, int cycle);
}
