package com.reggie.module.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AiToolDates} 单元测试：默认近 7 天、坏参回落、未来日期与超长跨度钳制。
 *
 * @author reggie
 * @since 2026-09-21
 */
class AiToolDatesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void nullArgsDefaultsToLastSevenDays() {
        AiToolDates.DateRange range = AiToolDates.normalizeRange(null);
        LocalDate today = LocalDate.now();
        assertEquals(today, range.getEnd());
        assertEquals(today.minusDays(6), range.getStart());
    }

    @Test
    void normalRangeParsed() {
        JsonNode args = parse("{\"startDate\":\"2026-09-01\",\"endDate\":\"2026-09-10\"}");
        AiToolDates.DateRange range = AiToolDates.normalizeRange(args);
        assertEquals(LocalDate.of(2026, 9, 1), range.getStart());
        assertEquals(LocalDate.of(2026, 9, 10), range.getEnd());
        assertEquals("2026-09-01", range.startText());
    }

    @Test
    void futureEndClampedToToday() {
        JsonNode args = parse("{\"endDate\":\"2099-01-01\"}");
        AiToolDates.DateRange range = AiToolDates.normalizeRange(args);
        assertEquals(LocalDate.now(), range.getEnd());
    }

    @Test
    void startAfterEndFallsBackToEnd() {
        JsonNode args = parse("{\"startDate\":\"2026-09-20\",\"endDate\":\"2026-09-01\"}");
        AiToolDates.DateRange range = AiToolDates.normalizeRange(args);
        assertEquals(range.getEnd(), range.getStart());
    }

    @Test
    void rangeExceedingMaxClamped() {
        JsonNode args = parse("{\"startDate\":\"2020-01-01\",\"endDate\":\"2026-09-10\"}");
        AiToolDates.DateRange range = AiToolDates.normalizeRange(args);
        long days = java.time.temporal.ChronoUnit.DAYS.between(range.getStart(), range.getEnd());
        assertEquals(AiToolDates.MAX_RANGE_DAYS - 1L, days);
    }

    @Test
    void malformedDatesFallBackToDefaults() {
        JsonNode args = parse("{\"startDate\":\"not-a-date\",\"endDate\":\"\"}");
        AiToolDates.DateRange range = AiToolDates.normalizeRange(args);
        LocalDate today = LocalDate.now();
        assertEquals(today, range.getEnd());
        assertEquals(today.minusDays(6), range.getStart());
    }

    @Test
    void singleDateFutureClampedToToday() {
        JsonNode args = parse("{\"date\":\"2099-05-01\"}");
        LocalDate result = AiToolDates.singleDate(args, "date", LocalDate.of(2026, 1, 1));
        assertEquals(LocalDate.now(), result);
    }

    @Test
    void singleDateMissingReturnsDefault() {
        LocalDate defaultValue = LocalDate.of(2026, 1, 1);
        LocalDate result = AiToolDates.singleDate(parse("{}"), "date", defaultValue);
        assertEquals(defaultValue, result);
    }

    @Test
    void singleDateNormalValueParsed() {
        JsonNode args = parse("{\"date\":\"2026-03-08\"}");
        LocalDate result = AiToolDates.singleDate(args, "date", LocalDate.now());
        assertEquals(LocalDate.of(2026, 3, 8), result);
        assertTrue(result.isBefore(LocalDate.now()) || result.equals(LocalDate.now()));
    }
}
