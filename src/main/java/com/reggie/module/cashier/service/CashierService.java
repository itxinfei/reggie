package com.reggie.module.cashier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.cashier.model.CashierRecord;
import com.reggie.module.cashier.model.DailySettlement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 收银服务接口
 *
 * @author reggie
 * @since 2026-08-10
 */
public interface CashierService extends IService<CashierRecord> {

    // ==================== 收银记录管理 ====================

    /**
     * 获取收银记录列表
     *
     * @param payType    支付类型
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @param tenantId   租户ID
     * @return 收银记录列表
     */
    List<CashierRecord> getCashierRecordList(Integer payType, LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * 根据订单ID获取收银记录
     *
     * @param orderId  订单ID
     * @param tenantId 租户ID
     * @return 收银记录
     */
    CashierRecord getCashierRecordByOrderId(Long orderId, Long tenantId);

    /**
     * 保存收银记录
     *
     * @param cashierRecord 收银记录
     * @return 是否成功
     */
    boolean saveCashierRecord(CashierRecord cashierRecord);

    /**
     * 收银收款
     *
     * @param orderId      订单ID
     * @param orderNumber  订单号
     * @param amount       收银金额
     * @param actualAmount 实收金额
     * @param payType      支付方式（1现金 2微信 3支付宝 4银行卡 5会员储值），当前仅现金/会员储值走真实记账
     * @param cashierId    收银员ID
     * @param cashierName  收银员姓名
     * @param usedCouponId 使用的优惠券ID
     * @param memberUserId 会员用户ID
     * @param remark       备注
     * @return 收银记录
     */
    CashierRecord cashPayment(Long orderId, String orderNumber, BigDecimal amount, BigDecimal actualAmount,
                              Integer payType, Long cashierId, String cashierName,
                              Long usedCouponId, Long memberUserId, String remark);

    /**
     * 删除收银记录
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean deleteCashierRecord(Long id);

    // ==================== 日结管理 ====================

    /**
     * 获取日结列表
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 日结列表
     */
    List<DailySettlement> getDailySettlementList(LocalDate startDate, LocalDate endDate, Long tenantId);

    /**
     * 根据日期获取日结
     *
     * @param settlementDate 结算日期
     * @param tenantId       租户ID
     * @return 日结
     */
    DailySettlement getDailySettlementByDate(LocalDate settlementDate, Long tenantId);

    /**
     * 执行日结
     *
     * @param settlementDate 结算日期
     * @param userId         操作人ID
     * @param userName       操作人姓名
     * @param tenantId       租户ID
     * @return 日结
     */
    DailySettlement executeDailySettlement(LocalDate settlementDate, Long userId, String userName, Long tenantId);

    /**
     * 取消日结
     *
     * @param settlementDate 结算日期
     * @param tenantId       租户ID
     * @return 是否成功
     */
    boolean cancelDailySettlement(LocalDate settlementDate, Long tenantId);

    /**
     * 删除日结
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean deleteDailySettlement(Long id);

    // ==================== 统计分析 ====================

    /**
     * 获取收银统计
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 收银统计
     */
    Map<String, Object> getCashierStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * 获取支付方式统计
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 支付方式统计
     */
    Map<String, Object> getPaymentTypeStatistics(LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * 获取收银趋势
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 收银趋势
     */
    Map<String, Object> getCashierTrend(LocalDateTime startDate, LocalDateTime endDate, Long tenantId);

    /**
     * 获取日结汇总
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param tenantId  租户ID
     * @return 日结汇总
     */
    Map<String, Object> getDailySettlementSummary(LocalDate startDate, LocalDate endDate, Long tenantId);
}

