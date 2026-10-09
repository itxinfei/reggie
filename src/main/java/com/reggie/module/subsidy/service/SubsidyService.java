package com.reggie.module.subsidy.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.subsidy.dto.SubsidyGrantDTO;
import com.reggie.module.subsidy.model.MealSubsidyAccount;
import com.reggie.module.subsidy.model.MealSubsidyRecord;
import com.reggie.module.subsidy.vo.SubsidyAccountVO;

import java.math.BigDecimal;

/**
 * 餐补 Service（企业内部订餐）
 * <p>余额变动三原则：① 原子 SQL（条件扣减/回充）；② 流水同步记账；
 * ③ 幂等键（核销=支付单tradeNo，回充=退款单refundNo）。</p>
 */
public interface SubsidyService extends IService<MealSubsidyAccount> {

    /**
     * 发放餐补（单个用户或按部门批量，二选一，部门优先）。
     *
     * @param dto        发放请求
     * @param tenantId   租户ID
     * @param operatorId 操作管理员ID
     * @return 实际发放的人数
     */
    int grant(SubsidyGrantDTO dto, Long tenantId, Long operatorId);

    /**
     * 订单核销（支付链路调用）：原子扣减余额并记 CONSUME 流水。
     * 幂等：同一 tradeNo 重复调用直接返回（不重复扣款）。
     *
     * @param userId  用户ID
     * @param tenantId 租户ID
     * @param orderId 订单ID
     * @param amount  扣减金额
     * @param tradeNo 支付单交易号
     */
    void consumeForOrder(Long userId, Long tenantId, Long orderId, BigDecimal amount, String tradeNo);

    /**
     * 订单退款回充（退款链路调用）：原子回充余额并记 REFUND 流水。
     * 幂等：同一 refundNo 重复调用直接返回（不重复回充）。
     *
     * @param tenantId 租户ID
     * @param userId   用户ID
     * @param orderId  订单ID
     * @param amount   回充金额
     * @param refundNo 退款单号（幂等键）
     */
    void refundForOrder(Long tenantId, Long userId, Long orderId, BigDecimal amount, String refundNo);

    /**
     * 查询用户餐补账户（不存在返回 null，C 端余额展示用）。
     */
    MealSubsidyAccount getMyAccount(Long userId, Long tenantId);

    /**
     * 管理端账户分页（附带顾客姓名/手机号/部门名）。
     */
    IPage<SubsidyAccountVO> pageAccounts(Long tenantId, int page, int pageSize, Long departmentId, String phone);

    /**
     * 流水分页。
     */
    IPage<MealSubsidyRecord> pageRecords(Long tenantId, int page, int pageSize, Long userId, String recordType);

    /**
     * 租户级汇总：accountCount / totalBalance / totalGranted / totalUsed。
     */
    java.util.Map<String, Object> stats(Long tenantId);

    /**
     * 部门对账（按用户归属部门聚合，未挂部门归入"未分配"）：
     * 有效订单数/金额（待接单/配送中/已完成）、餐补发放、餐补核销、企业自付估算。
     *
     * @param tenantId  租户ID
     * @param startDate 起始日期 yyyy-MM-dd（含）
     * @param endDate   结束日期 yyyy-MM-dd（含）
     * @return 每行: departmentName / orderCount / orderAmount / subsidyGranted / subsidyUsed / selfPay
     */
    java.util.List<java.util.Map<String, Object>> departmentReconciliation(Long tenantId,
                                                                          String startDate, String endDate);
}
