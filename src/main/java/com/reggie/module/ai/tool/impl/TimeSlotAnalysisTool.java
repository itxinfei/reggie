package com.reggie.module.ai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.tool.AiTool;
import com.reggie.module.ai.tool.AiToolDates;
import com.reggie.module.ai.tool.ToolExecResult;
import com.reggie.module.report.service.ReportService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.reggie.module.ai.tool.ToolSchemas.object;
import static com.reggie.module.ai.tool.ToolSchemas.stringType;

/**
 * 时段客流分析工具（薄封装 {@link ReportService#getTimeSlotAnalysis}）。
 * 返回早市(6-10)/午市(10-14)/下午茶(14-17)/晚市(17-21)/夜宵(21-次日6) 五段订单数与金额，
 * 用于回答“午市和晚市占比/哪个时段最忙”。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Component
public class TimeSlotAnalysisTool implements AiTool {

    @Resource
    private ReportService reportService;

    @Override
    public String name() {
        return "get_time_slot_analysis";
    }

    @Override
    public String label() {
        return "时段客流分析";
    }

    @Override
    public String description() {
        return "查询一段时间内 5 个营业时段(早市6-10/午市10-14/下午茶14-17/晚市17-21/夜宵21-次日6)"
                + "的订单量 count 与金额 amount(元)。用于回答午市晚市占比、高峰时段、营业时段分布。"
                + "startDate/endDate 为 yyyy-MM-dd，缺省近 7 天。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("startDate", stringType("开始日期 yyyy-MM-dd，缺省近 7 天"));
        props.put("endDate", stringType("结束日期 yyyy-MM-dd，缺省今天"));
        return object(props, null);
    }

    @Override
    public ToolExecResult execute(JsonNode args, Long tenantId) {
        AiToolDates.DateRange range = AiToolDates.normalizeRange(args);
        List<Map<String, Object>> slots = reportService.getTimeSlotAnalysis(
                range.startText(), range.endText(), tenantId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startDate", range.startText());
        data.put("endDate", range.endText());
        data.put("slots", slots);

        String summary = range.startText() + "~" + range.endText() + " 共 "
                + (slots == null ? 0 : slots.size()) + " 个时段";
        return ToolExecResult.ok(summary, data);
    }
}
