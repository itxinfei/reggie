package com.reggie.module.ai.tool;

import com.reggie.module.order.model.Orders;

/**
 * AI 经营分析的统一数据口径基线。
 *
 * <p>背景：后台各报表页的统计口径不一（营业额/日报只算已完成订单，菜品排行/时段/
 * 支付分析含全部状态）。AI 在同一次回答中常组合多个工具，口径不一致会导致
 * 「菜品销售额合计与营业额对不上、时段金额含取消单虚高」等自相矛盾。</p>
 *
 * <p>约定：AI 工具一律通过 {@code ReportService} 的 {@code statusFilter} 重载
 * 传入 {@link #BASELINE_STATUS}（仅已完成订单），保证 7 个工具在同一问题上
 * 数字可相互印证。后台报表页保持原口径不变。</p>
 *
 * @author reggie
 * @since 2026-09-30
 */
public final class AiMetricsCaliber {

    private AiMetricsCaliber() {
    }

    /** AI 经营分析统一订单过滤基线：仅已完成订单（status=4） */
    public static final Integer BASELINE_STATUS = Orders.STATUS_COMPLETED;

    /** 供注入 prompt 的口径说明（让模型在回答中如实说明统计范围） */
    public static final String CALIBER_NOTE = "以下经营数据均为「已完成订单」口径，不含待支付/已取消订单。";
}
