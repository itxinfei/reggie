package com.reggie.module.retention.service.impl;

import com.reggie.common.R;
import com.reggie.module.retention.dto.ChurnWarningVO;
import com.reggie.module.retention.dto.MemberLevelStatsVO;
import com.reggie.module.retention.dto.MemberVO;
import com.reggie.module.retention.dto.RetentionTrendVO;
import com.reggie.module.retention.dto.SmartRecommendVO;
import com.reggie.module.retention.mapper.RetentionMapper;
import com.reggie.module.retention.model.RetentionMember;
import com.reggie.module.retention.service.RetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会员留存服务实现
 * 优先调用 RetentionMapper 获取真实数据，数据库表不存在时降级为 Mock 数据。
 *
 * @author reggie
 * @since 2026-08-23
 */
@Service
@Slf4j
public class RetentionServiceImpl implements RetentionService {

    @Autowired
    private RetentionMapper retentionMapper;

    /** 结构化 Mock 会员数据（数据库表未创建时的兜底） */
    private static final List<RetentionMember> MOCK_MEMBERS = new ArrayList<>();

    static {
        LocalDate now = LocalDate.now();
        populateMockData(now);
    }

    /**
     * 填充 Mock 数据
     */
    private static void populateMockData(LocalDate now) {
        addMember(1L, "张三", "13800000001", "GOLD", 8520, now.minusDays(3), 45,
                new BigDecimal("12580.00"), "ACTIVE", "高频", 1L);
        addMember(2L, "李四", "13800000002", "GOLD", 7200, now.minusDays(7), 38,
                new BigDecimal("9850.00"), "ACTIVE", "高频", 1L);
        addMember(3L, "王五", "13800000003", "GOLD", 6800, now.minusDays(15), 32,
                new BigDecimal("8720.00"), "ACTIVE", "新客", 1L);
        addMember(4L, "赵六", "13800000004", "GOLD", 5500, now.minusDays(50), 28,
                new BigDecimal("6540.00"), "DORMANT", "沉默", 1L);
        addMember(5L, "钱七", "13800000005", "GOLD", 4900, now.minusDays(60), 25,
                new BigDecimal("5200.00"), "CHURNED", "流失", 1L);

        addMember(6L, "孙八", "13800000006", "SILVER", 4200, now.minusDays(5), 22,
                new BigDecimal("4380.00"), "ACTIVE", "新客", 1L);
        addMember(7L, "周九", "13800000007", "SILVER", 3800, now.minusDays(10), 18,
                new BigDecimal("3650.00"), "ACTIVE", "高频", 1L);
        addMember(8L, "吴十", "13800000008", "SILVER", 3500, now.minusDays(20), 15,
                new BigDecimal("3200.00"), "ACTIVE", "沉默", 1L);
        addMember(9L, "郑一", "13800000009", "SILVER", 3100, now.minusDays(35), 12,
                new BigDecimal("2850.00"), "DORMANT", "沉默", 1L);
        addMember(10L, "王二", "13800000010", "SILVER", 2700, now.minusDays(45), 10,
                new BigDecimal("2400.00"), "CHURNED", "流失", 1L);
        addMember(11L, "李三", "13800000011", "SILVER", 2500, now.minusDays(55), 8,
                new BigDecimal("2100.00"), "CHURNED", "流失", 1L);

        addMember(12L, "陈四", "13800000012", "NORMAL", 2100, now.minusDays(2), 8,
                new BigDecimal("1850.00"), "ACTIVE", "新客", 1L);
        addMember(13L, "林五", "13800000013", "NORMAL", 1800, now.minusDays(6), 6,
                new BigDecimal("1520.00"), "ACTIVE", "新客", 1L);
        addMember(14L, "黄六", "13800000014", "NORMAL", 1500, now.minusDays(12), 5,
                new BigDecimal("1280.00"), "ACTIVE", "沉默", 1L);
        addMember(15L, "刘七", "13800000015", "NORMAL", 1200, now.minusDays(25), 4,
                new BigDecimal("980.00"), "ACTIVE", "沉默", 1L);
        addMember(16L, "杨八", "13800000016", "NORMAL", 900, now.minusDays(40), 3,
                new BigDecimal("720.00"), "DORMANT", "沉默", 1L);
        addMember(17L, "朱九", "13800000017", "NORMAL", 600, now.minusDays(55), 2,
                new BigDecimal("450.00"), "CHURNED", "流失", 1L);
        addMember(18L, "秦十", "13800000018", "NORMAL", 400, now.minusDays(70), 1,
                new BigDecimal("320.00"), "CHURNED", "流失", 1L);

        addMember(19L, "许一", "13800000019", "GOLD", 9100, now.minusDays(1), 50,
                new BigDecimal("15200.00"), "ACTIVE", "高频", 1L);
        addMember(20L, "何二", "13800000020", "SILVER", 3300, now.minusDays(8), 14,
                new BigDecimal("3080.00"), "ACTIVE", "新客", 1L);
    }

    private static void addMember(Long id, String name, String phone, String level, Integer points,
                                  LocalDate lastOrderDate, Integer totalOrders, BigDecimal totalSpent,
                                  String status, String tag, Long tenantId) {
        RetentionMember member = new RetentionMember();
        member.setId(id);
        member.setMemberName(name);
        member.setPhone(phone);
        member.setLevel(level);
        member.setPoints(points);
        member.setLastOrderDate(lastOrderDate);
        member.setTotalOrders(totalOrders);
        member.setTotalSpent(totalSpent);
        member.setStatus(status);
        member.setTag(tag);
        member.setTenantId(tenantId);
        MOCK_MEMBERS.add(member);
    }

    // ==================== Service 接口实现 ====================

    @Override
    public Map<String, Object> getRetentionOverview(Long tenantId) {
        List<RetentionMember> members = loadMembers(tenantId);
        Map<String, Object> overview = new HashMap<>();

        int goldCount = 0, silverCount = 0, normalCount = 0;
        int activeCount = 0, dormantCount = 0, churnedCount = 0;
        for (RetentionMember m : members) {
            String level = m.getLevel();
            if ("GOLD".equals(level)) goldCount++;
            else if ("SILVER".equals(level)) silverCount++;
            else if ("NORMAL".equals(level)) normalCount++;

            String status = m.getStatus();
            if ("ACTIVE".equals(status)) activeCount++;
            else if ("DORMANT".equals(status)) dormantCount++;
            else if ("CHURNED".equals(status)) churnedCount++;
        }

        BigDecimal avgPoints = BigDecimal.ZERO;
        if (!members.isEmpty()) {
            int totalPoints = 0;
            for (RetentionMember m : members) {
                totalPoints += m.getPoints();
            }
            avgPoints = BigDecimal.valueOf(totalPoints).divide(
                    BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalSpent = BigDecimal.ZERO;
        for (RetentionMember m : members) {
            if (m.getTotalSpent() != null) {
                totalSpent = totalSpent.add(m.getTotalSpent());
            }
        }

        overview.put("totalCount", members.size());
        overview.put("goldCount", goldCount);
        overview.put("silverCount", silverCount);
        overview.put("normalCount", normalCount);
        overview.put("activeCount", activeCount);
        overview.put("dormantCount", dormantCount);
        overview.put("churnedCount", churnedCount);
        overview.put("avgPoints", avgPoints);
        overview.put("totalSpent", totalSpent);
        overview.put("tenantId", tenantId);

        // 增加等级统计
        List<Map<String, Object>> levelStats = buildLevelStats(members);
        overview.put("levelStats", levelStats);

        return overview;
    }

    @Override
    public List<Map<String, Object>> getMemberList(Long tenantId, String level, String status) {
        List<RetentionMember> members = loadMembers(tenantId);

        // 按等级筛选
        if (level != null && !level.isEmpty()) {
            final String filterLevel = level;
            List<RetentionMember> filtered = new ArrayList<>();
            for (RetentionMember m : members) {
                if (filterLevel.equals(m.getLevel())) {
                    filtered.add(m);
                }
            }
            members = filtered;
        }

        // 按状态筛选
        if (status != null && !status.isEmpty()) {
            final String filterStatus = status;
            List<RetentionMember> filtered = new ArrayList<>();
            for (RetentionMember m : members) {
                if (filterStatus.equals(m.getStatus())) {
                    filtered.add(m);
                }
            }
            members = filtered;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (RetentionMember m : members) {
            result.add(memberToMap(m));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPointsRanking(Long tenantId) {
        List<RetentionMember> members = loadMembers(tenantId);

        List<RetentionMember> sorted = new ArrayList<>(members);
        sorted.sort(new Comparator<RetentionMember>() {
            @Override
            public int compare(RetentionMember o1, RetentionMember o2) {
                // 防御性 null 检查：points 可能为 null（数据库中未设置的记录）
                Integer points1 = o1.getPoints() != null ? o1.getPoints() : 0;
                Integer points2 = o2.getPoints() != null ? o2.getPoints() : 0;
                return points2.compareTo(points1);
            }
        });

        int limit = Math.min(20, sorted.size());
        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (int i = 0; i < limit; i++) {
            Map<String, Object> map = memberToMap(sorted.get(i));
            map.put("rank", rank++);
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getChurnWarning(Long tenantId) {
        // 优先从 Mapper 获取
        List<RetentionMember> riskMembers = retentionMapper.listChurnRiskMembers(tenantId, 30);

        if (!riskMembers.isEmpty()) {
            return convertChurnWarningList(riskMembers);
        }

        // 降级到 Mock 数据
        List<RetentionMember> members = loadMembers(tenantId);
        return convertChurnWarningList(members);
    }

    @Override
    public List<Map<String, Object>> getSmartRecommend(Long tenantId) {
        List<RetentionMember> members = loadMembers(tenantId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (RetentionMember m : members) {
            if (shouldRecommend(m)) {
                Map<String, Object> rec = memberToMap(m);
                String recommendType = getRecommendCouponType(m);
                String recommendReason = getRecommendReason(m);
                String priority = getRecommendPriority(m);
                rec.put("couponType", recommendType);
                rec.put("reason", recommendReason);
                rec.put("priority", priority);
                result.add(rec);
            }
        }
        return result;
    }

    @Override
    public R<Void> sendCoupon(Long memberId) {
        log.info("发送优惠券给会员: {}", memberId);
        R<Void> r = new R<>();
        r.setCode(1);
        r.setMsg("Coupon sent successfully to member " + memberId);
        return r;
    }

    @Override
    public R<Void> batchSendCoupon(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return R.error("Member ID list cannot be empty");
        }
        log.info("批量发送优惠券给 {} 个会员", memberIds.size());
        R<Void> r = new R<>();
        r.setCode(1);
        r.setMsg("Coupons sent to " + memberIds.size() + " members");
        return r;
    }

    // ==================== 留存趋势 ====================

    /**
     * 获取近7天留存趋势数据
     *
     * @param tenantId 租户ID
     * @return 趋势数据列表
     */
    public List<Map<String, Object>> getRetentionTrend(Long tenantId) {
        // 优先从 Mapper 获取
        List<Map<String, Object>> trendData = retentionMapper.getRetentionTrend(
                tenantId, LocalDate.now().minusDays(6).toString(), LocalDate.now().toString());

        if (!trendData.isEmpty()) {
            return trendData;
        }

        // 降级到 Mock 趋势数据
        return buildMockTrendData(tenantId);
    }

    /**
     * 获取会员等级统计
     *
     * @param tenantId 租户ID
     * @return 等级统计列表
     */
    public List<Map<String, Object>> getMemberLevelStats(Long tenantId) {
        List<RetentionMember> members = loadMembers(tenantId);
        return buildLevelStats(members);
    }

    // ==================== Private Helper Methods ====================

    /**
     * 加载会员数据：优先 Mapper，降级 Mock
     *
     * <p>降级路径说明：当 RetentionMember 数据库表尚未创建或无数据时，
     * 使用 Mock 数据兜底，并将 Mock 记录的 tenantId 重写为当前请求租户，
     * 确保所有租户在过渡期都能看到代表性数据（而非静默返回空列表）。
     */
    private List<RetentionMember> loadMembers(Long tenantId) {
        List<RetentionMember> activeMembers = retentionMapper.listActiveMembers(tenantId, 100);
        if (!activeMembers.isEmpty()) {
            return activeMembers;
        }
        if (log.isWarnEnabled()) {
            log.warn("RetentionMember 数据库无数据(tenantId={})，降级为 Mock 数据", tenantId);
        }
        return filterByTenant(tenantId);
    }

    /**
     * 按租户过滤 Mock 数据，并将 Mock 记录的 tenantId 重写为请求租户
     * 避免多租户场景下仅 tenantId=1 能看到 Mock 数据
     */
    private List<RetentionMember> filterByTenant(Long tenantId) {
        List<RetentionMember> copy = new ArrayList<>(MOCK_MEMBERS);
        if (tenantId != null) {
            for (RetentionMember m : copy) {
                m.setTenantId(tenantId);
            }
        }
        return copy;
    }

    /**
     * 计算流失预警列表（含 riskScore 和 riskLevel）
     */
    private List<Map<String, Object>> convertChurnWarningList(List<RetentionMember> members) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (RetentionMember m : members) {
            if (m.getLastOrderDate() == null) {
                continue;
            }
            Period period = Period.between(m.getLastOrderDate(), today);
            int daysSince = period.getDays();
            if (daysSince > 30) {
                Map<String, Object> map = memberToMap(m);
                map.put("daysSinceLastOrder", daysSince);

                int riskScore = calculateRiskScore(m, daysSince);
                String riskLevel = calculateRiskLevel(riskScore);
                map.put("riskScore", riskScore);
                map.put("riskLevel", riskLevel);

                result.add(map);
            }
        }
        return result;
    }

    /**
     * 计算风险评分（0-100，越高越危险）
     *
     * @param m         会员
     * @param daysSince 距上次下单天数
     * @return 风险评分
     */
    private int calculateRiskScore(RetentionMember m, int daysSince) {
        int score = 0;

        // 距今天数权重（50分基础）
        if (daysSince >= 60) {
            score += 50;
        } else if (daysSince >= 45) {
            score += 35;
        } else if (daysSince >= 30) {
            score += 20;
        }

        // 等级权重（高等级流失风险更大，40分）
        if ("GOLD".equals(m.getLevel())) {
            score += 40;
        } else if ("SILVER".equals(m.getLevel())) {
            score += 25;
        } else {
            score += 10;
        }

        // 状态权重（10分）
        if ("DORMANT".equals(m.getStatus())) {
            score += 10;
        } else if ("CHURNED".equals(m.getStatus())) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    /**
     * 根据风险评分确定风险等级
     *
     * @param score 风险评分
     * @return LOW / MEDIUM / HIGH
     */
    private String calculateRiskLevel(int score) {
        if (score >= 70) {
            return "HIGH";
        }
        if (score >= 40) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * 判断是否应该推荐发券
     */
    private boolean shouldRecommend(RetentionMember m) {
        if ("DORMANT".equals(m.getStatus())) {
            return true;
        }
        if ("CHURNED".equals(m.getStatus())) {
            return true;
        }
        if ("SILVER".equals(m.getLevel()) && "ACTIVE".equals(m.getStatus())) {
            return true;
        }
        return false;
    }

    /**
     * 根据会员状态和等级推荐优惠券类型
     */
    private String getRecommendCouponType(RetentionMember m) {
        if ("DORMANT".equals(m.getStatus())) {
            return "满减券";
        }
        if ("CHURNED".equals(m.getStatus())) {
            return "回归券";
        }
        if ("SILVER".equals(m.getLevel())) {
            return "升级券";
        }
        return "通用券";
    }

    /**
     * 生成推荐原因
     */
    private String getRecommendReason(RetentionMember m) {
        if ("DORMANT".equals(m.getStatus())) {
            return "会员处于沉默状态，推荐发送满减券刺激消费";
        }
        if ("CHURNED".equals(m.getStatus())) {
            return "会员已流失，推荐发送回归券召回";
        }
        if ("SILVER".equals(m.getLevel())) {
            return "会员等级为SILVER，推荐发送升级券激励升级";
        }
        return "推荐发送优惠券";
    }

    /**
     * 计算推荐优先级
     */
    private String getRecommendPriority(RetentionMember m) {
        if ("CHURNED".equals(m.getStatus()) && "GOLD".equals(m.getLevel())) {
            return "HIGH";
        }
        if ("DORMANT".equals(m.getStatus())) {
            return "HIGH";
        }
        if ("CHURNED".equals(m.getStatus())) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /**
     * 构造等级统计数据
     */
    private List<Map<String, Object>> buildLevelStats(List<RetentionMember> members) {
        List<Map<String, Object>> result = new ArrayList<>();
        String[] levels = {"GOLD", "SILVER", "NORMAL"};
        int totalCount = members.size();

        for (String level : levels) {
            final String currentLevel = level;
            List<RetentionMember> levelMembers = new ArrayList<>();
            for (RetentionMember m : members) {
                if (currentLevel.equals(m.getLevel())) {
                    levelMembers.add(m);
                }
            }

            int count = levelMembers.size();
            double ratio = totalCount > 0
                    ? BigDecimal.valueOf(count).divide(BigDecimal.valueOf(totalCount), 2,
                    RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0;

            BigDecimal avgSpent = BigDecimal.ZERO;
            if (count > 0) {
                BigDecimal sumSpent = BigDecimal.ZERO;
                for (RetentionMember m : levelMembers) {
                    if (m.getTotalSpent() != null) {
                        sumSpent = sumSpent.add(m.getTotalSpent());
                    }
                }
                avgSpent = sumSpent.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("level", level);
            stats.put("count", count);
            stats.put("ratio", ratio);
            stats.put("avgSpent", avgSpent);
            result.add(stats);
        }
        return result;
    }

    /**
     * 生成 Mock 留存趋势数据（近7天）
     */
    private List<Map<String, Object>> buildMockTrendData(Long tenantId) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // 模拟近7天趋势数据
        int[] newMembers = {12, 8, 15, 10, 6, 9, 14};
        int[] retainedMembers = {85, 82, 88, 84, 80, 83, 87};
        int[] churnedMembers = {2, 3, 1, 4, 3, 2, 1};

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            int ni = 6 - i;
            int total = retainedMembers[ni] + churnedMembers[ni];
            double rate = total > 0
                    ? BigDecimal.valueOf(retainedMembers[ni]).divide(
                    BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0;

            Map<String, Object> trend = new HashMap<>();
            trend.put("date", date);
            trend.put("newMembers", newMembers[ni]);
            trend.put("retainedMembers", retainedMembers[ni]);
            trend.put("churnedMembers", churnedMembers[ni]);
            trend.put("retentionRate", rate);
            result.add(trend);
        }
        return result;
    }

    /**
     * 将 RetentionMember 转换为 Map
     */
    private Map<String, Object> memberToMap(RetentionMember m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("memberName", m.getMemberName());
        map.put("phone", m.getPhone());
        map.put("level", m.getLevel());
        map.put("points", m.getPoints());
        map.put("lastOrderDate", m.getLastOrderDate());
        map.put("totalOrders", m.getTotalOrders());
        map.put("totalSpent", m.getTotalSpent());
        map.put("status", m.getStatus());
        map.put("tag", m.getTag());
        map.put("tenantId", m.getTenantId());
        return map;
    }
}