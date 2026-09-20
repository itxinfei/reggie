package com.reggie.module.ai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.tool.AiTool;
import com.reggie.module.ai.tool.AiToolDates;
import com.reggie.module.ai.tool.ToolExecResult;
import com.reggie.module.report.service.ReportService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.reggie.module.ai.tool.ToolSchemas.object;
import static com.reggie.module.ai.tool.ToolSchemas.stringType;

/**
 * 支付方式分析工具（薄封装 {@link ReportService#getPaymentAnalysis}）。
 * 口径：2=微信、3=支付宝、现金/银行卡/储值/货到付款并入 balance，与后台报表-支付分析一致。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Component
public class PaymentAnalysisTool implements AiTool {

    @Resource
    private ReportService reportService;

    @Override
    public String name() {
        return "get_payment_analysis";
    }

    @Override
    public String label() {
        return "支付方式分析";
    }

    @Override
    public String description() {
        return "查询一段时间内各支付方式的订单笔数 count 与金额 amount(元)，"
                + "分为 wechat(微信)、alipay(支付宝)、balance(现金/银行卡/余额/货到付款合并)、other。"
                + "用于回答微信支付宝占比、收款方式分布。startDate/endDate 为 yyyy-MM-dd，缺省近 7 天。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("startDate", stringType("开始日期 yyyy-MM-dd，缺省近 7 天"));
        props.put("endDate", stringType("结束日期 yyyy-MM-dd，缺省今天"));
        return object(props, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolExecResult execute(JsonNode args, Long tenantId) {
        AiToolDates.DateRange range = AiToolDates.normalizeRange(args);
        Map<String, Object> payment = reportService.getPaymentAnalysis(
                range.startText(), range.endText(), tenantId);
        if (payment == null) {
            payment = new LinkedHashMap<>();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startDate", range.startText());
        data.put("endDate", range.endText());
        data.put("payment", payment);

        Object wechat = payment.get("wechat");
        Object alipay = payment.get("alipay");
        String summary = "微信 " + countOf(wechat) + " 笔 / 支付宝 " + countOf(alipay) + " 笔";
        return ToolExecResult.ok(summary, data);
    }

    private Object countOf(Object group) {
        if (group instanceof Map) {
            Object count = ((Map<String, Object>) group).get("count");
            return count != null ? count : 0;
        }
        return 0;
    }
}
