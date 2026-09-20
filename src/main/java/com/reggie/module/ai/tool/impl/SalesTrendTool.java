package com.reggie.module.ai.tool.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.reggie.module.ai.tool.AiTool;
import com.reggie.module.ai.tool.ToolExecResult;
import com.reggie.module.ai.tool.ToolFormats;
import com.reggie.module.dashboard.service.DashboardService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.reggie.module.ai.tool.ToolSchemas.integerType;
import static com.reggie.module.ai.tool.ToolSchemas.object;

/**
 * 近 N 天销售趋势工具（薄封装 {@link DashboardService#getTrend(Long, int)}）。
 * <p>复用仪表盘单条聚合 SQL + Redis 30 分钟缓存，避免逐天 N+1 查询；
 * 每日营业额口径为已完成订单(status=4)。days 仅开放 7/14/30，与仪表盘口径一致。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Component
public class SalesTrendTool implements AiTool {

    @Resource
    private DashboardService dashboardService;

    @Override
    public String name() {
        return "get_sales_trend";
    }

    @Override
    public String label() {
        return "近期销售趋势";
    }

    @Override
    public String description() {
        return "查询近期每日销售趋势，返回每日 date、营业额 revenue(元,仅已完成订单)、订单数 orderCount。"
                + "days 只支持 7/14/30（近 7/14/30 天），缺省 7。用于回答最近生意趋势、这几天每天营业额、最近一周表现。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        Map<String, Object> days = integerType("统计天数，仅支持 7/14/30，缺省 7");
        days.put("enum", new int[]{7, 14, 30});
        props.put("days", days);
        return object(props, null);
    }

    @Override
    public ToolExecResult execute(JsonNode args, Long tenantId) {
        int days = 7;
        if (args != null && args.path("days").isInt()) {
            int d = args.path("days").asInt(7);
            if (d == 14 || d == 30) {
                days = d;
            }
        }
        List<Map<String, Object>> trend = dashboardService.getTrend(tenantId, days);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("days", days);
        data.put("trend", trend);

        BigDecimal total = BigDecimal.ZERO;
        if (trend != null) {
            for (Map<String, Object> day : trend) {
                Object revenue = day.get("revenue");
                if (revenue != null) {
                    try {
                        total = total.add(new BigDecimal(revenue.toString()));
                    } catch (NumberFormatException ignore) {
                        // 脏值跳过
                    }
                }
            }
        }
        String avg = ToolFormats.money(total.divide(new BigDecimal(days), 2, BigDecimal.ROUND_HALF_UP));
        String summary = "近 " + days + " 天日均营业额 ¥" + avg;
        return ToolExecResult.ok(summary, data);
    }
}
