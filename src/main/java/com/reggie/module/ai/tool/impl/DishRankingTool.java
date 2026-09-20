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

import static com.reggie.module.ai.tool.ToolSchemas.integerType;
import static com.reggie.module.ai.tool.ToolSchemas.object;
import static com.reggie.module.ai.tool.ToolSchemas.stringType;

/**
 * 菜品销量排行工具（薄封装 {@link ReportService#getDishRanking}）。
 * 按订单明细聚合销量（含全部状态订单），与后台报表-菜品排行口径一致。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Component
public class DishRankingTool implements AiTool {

    @Resource
    private ReportService reportService;

    @Override
    public String name() {
        return "get_dish_ranking";
    }

    @Override
    public String label() {
        return "菜品销量排行";
    }

    @Override
    public String description() {
        return "查询一段时间内的菜品销量排行榜，每项含菜品名 name、销量份数 count、销售额 revenue(元)。"
                + "用于回答“最近什么菜卖得最好/热销 Top N/本周冠军菜”。"
                + "startDate/endDate 为 yyyy-MM-dd，缺省近 7 天；limit 缺省 10，最大 50。";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("startDate", stringType("开始日期 yyyy-MM-dd，缺省近 7 天"));
        props.put("endDate", stringType("结束日期 yyyy-MM-dd，缺省今天"));
        props.put("limit", integerType("返回条数，缺省 10，最大 50"));
        return object(props, null);
    }

    @Override
    public ToolExecResult execute(JsonNode args, Long tenantId) {
        AiToolDates.DateRange range = AiToolDates.normalizeRange(args);
        int limit = 10;
        if (args != null && args.path("limit").isInt()) {
            limit = Math.max(1, Math.min(50, args.path("limit").asInt(10)));
        }
        List<Map<String, Object>> ranking = reportService.getDishRanking(
                range.startText(), range.endText(), limit, tenantId, null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startDate", range.startText());
        data.put("endDate", range.endText());
        data.put("ranking", ranking);

        String summary;
        if (ranking == null || ranking.isEmpty()) {
            summary = range.startText() + "~" + range.endText() + " 暂无销量数据";
        } else {
            Map<String, Object> top = ranking.get(0);
            summary = "Top" + ranking.size() + "，冠军：" + top.get("name")
                    + "（" + top.get("count") + " 份）";
        }
        return ToolExecResult.ok(summary, data);
    }
}
