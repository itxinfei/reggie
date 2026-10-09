package com.reggie.module.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.module.delivery.dto.RiderIncomeRecordVO;
import com.reggie.module.delivery.dto.RiderIncomeSummaryVO;

import java.util.Map;

/**
 * 骑手收入服务。
 * <p>
 * 仅基于现有 {@code orders} 与 {@code delivery_time_record} 做只读聚合，解决骑手「看不见收入」的痛点；
 * 可提现账户、结算单、提现转账等资金类能力不在本服务范围（见修复计划 P0-1 增量 2）。
 * </p>
 *
 * @author reggie
 * @since 2026-09-28
 */
public interface RiderIncomeService {

    /**
     * 获取骑手收入汇总（今日 / 本周 / 本月三个窗口）。
     *
     * @param riderId 骑手 ID
     * @return key 为 {@code today}/{@code week}/{@code month}，value 为对应窗口汇总
     */
    Map<String, RiderIncomeSummaryVO> getRiderIncomeSummary(Long riderId);

    /**
     * 获取骑手单笔收入明细（分页）。
     *
     * @param riderId 骑手 ID
     * @param range   时间窗口：today / week / month
     * @param page    页码（从 1 开始）
     * @param size    每页条数（将被 {@code PageUtils} 封顶）
     * @return 分页明细
     */
    Page<RiderIncomeRecordVO> getRiderIncomeRecords(Long riderId, String range, int page, int size);
}
