package com.reggie.module.ai.tool;

import com.reggie.module.dashboard.service.DashboardService;
import com.reggie.module.report.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 经营数据快照降级（P4）：当前供应商不支持 function calling（如 Baidu 压平协议、
 * 未勾选 tools 能力）时，business_analysis 场景把近 {@value #SNAPSHOT_DAYS} 天核心经营数据
 * 作为 system 上下文注入，让模型基于真实数字而不是凭空编造。
 * <p>按租户内存缓存 {@value #CACHE_TTL_SECONDS} 秒（仪表盘趋势本身另有 Redis 30 分钟缓存）；
 * 任何异常静默返回 null，绝不阻断聊天主链路。</p>
 *
 * @author reggie
 * @since 2026-09-20
 */
@Slf4j
@Component
public class BusinessSnapshotService {

    /** 快照缓存有效期：5 分钟 */
    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000L;
    private static final int CACHE_TTL_SECONDS = 300;

    private static final int SNAPSHOT_DAYS = 7;
    private static final int TOP_DISHES = 10;

    @Resource
    private ReportService reportService;

    @Resource
    private DashboardService dashboardService;

    private final Map<Long, CacheEntry> snapshotCache = new ConcurrentHashMap<>();

    /**
     * 构建可直接作为 system 消息追加的快照提示词；无数据或异常时返回 null。
     */
    public String buildSnapshotSystemPrompt(Long tenantId) {
        if (tenantId == null) {
            return null;
        }
        try {
            String snapshot = loadCached(tenantId);
            if (snapshot == null || snapshot.isEmpty()) {
                return null;
            }
            LocalDate end = LocalDate.now();
            LocalDate start = end.minusDays(SNAPSHOT_DAYS - 1L);
            return "以下是本店 " + start + " 至 " + end
                    + " 的真实经营数据快照（由后台报表系统生成）。回答经营问题时必须严格基于以下数据，"
                    + "不得编造数字；快照未覆盖的问题可结合餐饮经营常识给建议，并主动说明数据范围。\n"
                    + snapshot;
        } catch (Exception e) {
            log.warn("经营快照生成失败，降级为无快照对话: tenantId={}, err={}", tenantId, e.getMessage());
            return null;
        }
    }

    private String loadCached(Long tenantId) {
        long now = System.currentTimeMillis();
        CacheEntry cached = snapshotCache.get(tenantId);
        if (cached != null && now - cached.createdAt < CACHE_TTL_MILLIS) {
            return cached.text;
        }
        // 并发生成可接受（报表查询轻量且趋势有 Redis 缓存），不加 per-tenant 锁避免过度设计
        String text = loadSnapshot(tenantId);
        if (text != null && !text.isEmpty()) {
            snapshotCache.put(tenantId, new CacheEntry(text, now));
        }
        return text;
    }

    private String loadSnapshot(Long tenantId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(SNAPSHOT_DAYS - 1L);
        String startText = start.toString();
        String endText = end.toString();

        StringBuilder sb = new StringBuilder();

        // 1) 近 7 天每日经营（复用仪表盘单条聚合 SQL，自带 Redis 缓存；营业额仅含已完成订单）
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int totalOrders = 0;
        List<Map<String, Object>> trend = dashboardService.getTrend(tenantId, SNAPSHOT_DAYS);
        if (trend != null && !trend.isEmpty()) {
            sb.append("【近 ").append(SNAPSHOT_DAYS).append(" 天每日经营数据】（营业额口径：仅已完成订单）\n");
            for (Map<String, Object> day : trend) {
                BigDecimal revenue = toBigDecimal(day.get("revenue"));
                Object orderCount = day.get("orderCount");
                totalRevenue = totalRevenue.add(revenue);
                if (orderCount instanceof Number) {
                    totalOrders += ((Number) orderCount).intValue();
                }
                sb.append("- ").append(day.get("date")).append("：营业额 ¥")
                        .append(ToolFormats.money(revenue)).append("，订单 ")
                        .append(orderCount != null ? orderCount : 0).append(" 单\n");
            }
            BigDecimal avg = totalRevenue.divide(new BigDecimal(SNAPSHOT_DAYS), 2, BigDecimal.ROUND_HALF_UP);
            sb.append("合计：营业额 ¥").append(ToolFormats.money(totalRevenue))
                    .append("，订单 ").append(totalOrders).append(" 单，日均 ¥")
                    .append(ToolFormats.money(avg)).append("\n\n");
        }

        // 2) 热销 Top10（按订单明细聚合，含全部状态订单，与后台菜品排行同口径）
        List<Map<String, Object>> ranking = reportService.getDishRanking(
                startText, endText, TOP_DISHES, tenantId, null);
        if (ranking != null && !ranking.isEmpty()) {
            sb.append("【近 ").append(SNAPSHOT_DAYS).append(" 天热销菜品 Top").append(ranking.size())
                    .append("】（按销量份数，含全部状态订单）\n");
            int rank = 1;
            for (Map<String, Object> dish : ranking) {
                sb.append(rank++).append(". ").append(dish.get("name")).append("：")
                        .append(dish.get("count")).append(" 份，销售额 ¥")
                        .append(ToolFormats.money(dish.get("revenue"))).append("\n");
            }
            sb.append("\n");
        }

        // 3) 支付方式分布（2=微信、3=支付宝，现金/银行卡/余额/货到付款并入 balance）
        Map<String, Object> payment = reportService.getPaymentAnalysis(startText, endText, tenantId);
        if (payment != null && !payment.isEmpty()) {
            BigDecimal payTotal = BigDecimal.ZERO;
            String[] keys = {"wechat", "alipay", "balance", "other"};
            for (String key : keys) {
                payTotal = payTotal.add(groupAmount(payment.get(key)));
            }
            if (payTotal.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("【近 ").append(SNAPSHOT_DAYS).append(" 天支付方式分布】（按收款金额）\n");
                appendPaymentLine(sb, "微信", payment.get("wechat"), payTotal);
                appendPaymentLine(sb, "支付宝", payment.get("alipay"), payTotal);
                appendPaymentLine(sb, "现金/银行卡/余额/货到付款", payment.get("balance"), payTotal);
                appendPaymentLine(sb, "其他方式", payment.get("other"), payTotal);
            }
        }

        return sb.length() == 0 ? null : sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private BigDecimal groupAmount(Object group) {
        if (group instanceof Map) {
            return toBigDecimal(((Map<String, Object>) group).get("amount"));
        }
        return BigDecimal.ZERO;
    }

    @SuppressWarnings("unchecked")
    private void appendPaymentLine(StringBuilder sb, String label, Object group, BigDecimal total) {
        int count = 0;
        BigDecimal amount = BigDecimal.ZERO;
        if (group instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) group;
            Object c = m.get("count");
            if (c instanceof Number) {
                count = ((Number) c).intValue();
            }
            amount = toBigDecimal(m.get("amount"));
        }
        BigDecimal percent = amount.multiply(new BigDecimal("100"))
                .divide(total, 1, BigDecimal.ROUND_HALF_UP);
        sb.append("- ").append(label).append("：").append(count).append(" 笔，¥")
                .append(ToolFormats.money(amount)).append("（占 ").append(percent.toPlainString()).append("%）\n");
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** 快照缓存条目 */
    private static final class CacheEntry {
        private final String text;
        private final long createdAt;

        private CacheEntry(String text, long createdAt) {
            this.text = text;
            this.createdAt = createdAt;
        }
    }
}
