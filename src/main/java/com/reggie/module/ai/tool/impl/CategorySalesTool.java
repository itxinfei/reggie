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
 * 菜品分类销量工具（薄封装 {@link ReportService#getCategorySales}）。
 * 用于回答“热菜/凉菜哪个品类卖得多、分类占比”。
 *
 * @author reggie
 * @since 2026-09-20
 */
@Component
public class CategorySalesTool implements AiTool {

    @Resource
    private ReportService reportService;

    @Override
    public String name() {
        return "get_category_sales";
    }

    @Override
    public String label() {
        return "分类销量占比";
    }

    @Override
    public String description() {
        return "查询一段时间内各菜品分类(如热菜/凉菜/饮品)的销量份数，每项含分类名 name 与销量 count(份)，按销量降序。"
                + "用于回答品类销售结构、哪个分类卖得最好。startDate/endDate 为 yyyy-MM-dd，缺省近 7 天。";
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
        List<Map<String, Object>> categories = reportService.getCategorySales(
                range.startText(), range.endText(), tenantId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("startDate", range.startText());
        data.put("endDate", range.endText());
        data.put("categories", categories);

        String summary;
        if (categories == null || categories.isEmpty()) {
            summary = range.startText() + "~" + range.endText() + " 暂无分类销量数据";
        } else {
            Map<String, Object> top = categories.get(0);
            summary = "共 " + categories.size() + " 个分类，最高：" + top.get("name")
                    + "（" + top.get("count") + " 份）";
        }
        return ToolExecResult.ok(summary, data);
    }
}
