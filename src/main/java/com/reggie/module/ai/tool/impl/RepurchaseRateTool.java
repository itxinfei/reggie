package com.reggie.module.ai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.tool.AiTool;
import com.reggie.module.ai.tool.AiToolDates;
import com.reggie.module.ai.tool.ToolExecResult;
import com.reggie.module.ai.tool.ToolFormats;
import com.reggie.module.report.service.ReportService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.reggie.module.ai.tool.ToolSchemas.object;
import static com.reggie.module.ai.tool.ToolSchemas.stringType;

/**
 * 复购率工具（薄封装 {@link ReportService#getRepurchaseRate}）。
 * 口径：仅已完成订单(status=4)，窗口内同一下单用户下单 ≥2 次计为复购。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Component
public class RepurchaseRateTool implements AiTool {

    @Resource
    private ReportService reportService;

    @Override
    public String name() {
        return "get_repurchase_rate";
    }

    @Override
    public String label() {
        return "复购率分析";
    }

    @Override
    public String description() {
        return "查询一段时间内的顾客复购率：totalRate 为区间总复购率(百分比)，totalUsers 下单用户数，"
                + "repurchaseUsers 复购用户数。period 为统计粒度 day/week/month/year，缺省 day。"
                + "startDate/endDate 为 yyyy-MM-dd，缺省近 7 天。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        Map<String, Object> period = stringType("统计粒度 day/week/month/year，缺省 day");
        period.put("enum", Arrays.asList("day", "week", "month", "year"));
        props.put("period", period);
        props.put("startDate", stringType("开始日期 yyyy-MM-dd，缺省近 7 天"));
        props.put("endDate", stringType("结束日期 yyyy-MM-dd，缺省今天"));
        return object(props, null);
    }

    @Override
    public ToolExecResult execute(JsonNode args, Long tenantId) {
        String period = "day";
        if (args != null) {
            String p = args.path("period").asText("day");
            if ("week".equals(p) || "month".equals(p) || "year".equals(p)) {
                period = p;
            }
        }
        AiToolDates.DateRange range = AiToolDates.normalizeRange(args);
        Map<String, Object> repurchase = reportService.getRepurchaseRate(
                period, range.startText(), range.endText(), tenantId);
        if (repurchase == null) {
            repurchase = new LinkedHashMap<>();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startDate", range.startText());
        data.put("endDate", range.endText());
        data.put("period", period);
        data.put("repurchase", repurchase);

        String summary = "复购率 " + ToolFormats.money(repurchase.get("totalRate")) + "%，"
                + "复购用户 " + repurchase.getOrDefault("repurchaseUsers", 0)
                + "/" + repurchase.getOrDefault("totalUsers", 0);
        return ToolExecResult.ok(summary, data);
    }
}
