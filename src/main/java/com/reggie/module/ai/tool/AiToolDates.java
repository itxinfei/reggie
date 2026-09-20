package com.reggie.module.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * 工具入参日期解析与钳制（统一口径，防止模型传入超大日期跨度拖垮查询）。
 * <ul>
 *   <li>入参一律 yyyy-MM-dd；非法/缺失回落默认值，绝不抛异常打断对话；</li>
 *   <li>结束日不晚于今天；区间最长 {@value #MAX_RANGE_DAYS} 天；</li>
 *   <li>开始日晚于结束日时回退为结束日当天。</li>
 * </ul>
 *
 * @author reggie
 * @since 2026-09-20
 */
public final class AiToolDates {

    /** 最大查询跨度（天） */
    public static final int MAX_RANGE_DAYS = 92;

    private AiToolDates() {
    }

    /** 解析后的日期区间（含首尾） */
    public static class DateRange {
        private final LocalDate start;
        private final LocalDate end;

        DateRange(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }

        public LocalDate getStart() {
            return start;
        }

        public LocalDate getEnd() {
            return end;
        }

        public String startText() {
            return start.toString();
        }

        public String endText() {
            return end.toString();
        }
    }

    /**
     * 解析区间：默认近 7 天（含今天）；模型可传 startDate/endDate（yyyy-MM-dd）。
     */
    public static DateRange normalizeRange(JsonNode args) {
        LocalDate today = LocalDate.now();
        LocalDate end = parseDate(args == null ? null : args.path("endDate"), today);
        if (end.isAfter(today)) {
            end = today;
        }
        LocalDate start = parseDate(args == null ? null : args.path("startDate"), end.minusDays(6));
        if (start.isAfter(end)) {
            start = end;
        }
        long days = ChronoUnit.DAYS.between(start, end);
        if (days > MAX_RANGE_DAYS - 1L) {
            start = end.minusDays(MAX_RANGE_DAYS - 1L);
        }
        return new DateRange(start, end);
    }

    /**
     * 解析单个日期；非法/缺失返回默认值，且不晚于今天。
     */
    public static LocalDate singleDate(JsonNode args, String field, LocalDate defaultValue) {
        LocalDate parsed = parseDate(args == null ? null : args.path(field), defaultValue);
        LocalDate today = LocalDate.now();
        if (parsed.isAfter(today)) {
            return today;
        }
        return parsed;
    }

    private static LocalDate parseDate(JsonNode node, LocalDate defaultValue) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return defaultValue;
        }
        String text = node.asText("");
        if (text.isEmpty()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(text.trim().substring(0, Math.min(10, text.trim().length())));
        } catch (DateTimeParseException e) {
            return defaultValue;
        }
    }
}
