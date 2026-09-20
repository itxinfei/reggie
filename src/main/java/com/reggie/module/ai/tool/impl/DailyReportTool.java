package com.reggie.module.ai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.tool.AiTool;
import com.reggie.module.ai.tool.ToolExecResult;
import com.reggie.module.ai.tool.ToolFormats;
import com.reggie.module.report.service.ReportService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.reggie.module.ai.tool.ToolSchemas.object;
import static com.reggie.module.ai.tool.ToolSchemas.stringType;

/**
 * 单日经营日报工具（薄封装 {@link ReportService#getDailyReport}）。
 * 营业额口径为「已完成订单(status=4)」，与后台报表-日报一致。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Component
public class DailyReportTool implements AiTool {

    @Resource
    private ReportService reportService;

    @Override
    public String name() {
        return "get_daily_report";
    }

    @Override
    public String label() {
        return "经营日报";
    }

    @Override
    public String description() {
        return "查询某一天的经营日报，返回订单总数、已完成订单数、营业额(仅已完成订单,元)、取消订单数、客单价(元)。"
                + "参数 date 格式必须是 yyyy-MM-dd；用户说“今天/昨天/前天”时请先换算成具体日期再调用。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("date", stringType("查询日期 yyyy-MM-dd，缺省为昨天"));
        return object(props, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ToolExecResult execute(JsonNode args, Long tenantId) {
        LocalDate date = com.reggie.module.ai.tool.AiToolDates.singleDate(
                args, "date", LocalDate.now().minusDays(1));
        String dateText = date.toString();
        Map<String, Object> report = reportService.getDailyReport(dateText, tenantId);
        if (report == null) {
            report = new LinkedHashMap<>();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", dateText);
        data.put("report", report);

        Object amount = report.get("totalAmount");
        Object completed = report.get("completedOrders");
        String summary = dateText + " 营业额 ¥" + ToolFormats.money(amount) + "，已完成 "
                + (completed != null ? completed : 0) + " 单";
        return ToolExecResult.ok(summary, data);
    }
}
