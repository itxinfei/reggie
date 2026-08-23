package com.reggie.module.retention.service.impl;

import com.reggie.common.R;
import com.reggie.module.retention.model.RetentionMember;
import com.reggie.module.retention.service.RetentionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会员留存服务实现（Mock 数据）
 *
 * @author reggie
 * @since 2026-08-23
 */
@Service
@Slf4j
public class RetentionServiceImpl implements RetentionService {

    private static final List<RetentionMember> MOCK_MEMBERS = new ArrayList<>();

    static {
        LocalDate now = LocalDate.now();
        populateMockData(now);
    }

    private static void populateMockData(LocalDate now) {
        addMember(1L, "张三", "13800000001", "GOLD", 8520, now.minusDays(3), 45, new BigDecimal("12580.00"), "ACTIVE", "高频", 1L);
        addMember(2L, "李四", "13800000002", "GOLD", 7200, now.minusDays(7), 38, new BigDecimal("9850.00"), "ACTIVE", "高频", 1L);
        addMember(3L, "王五", "13800000003", "GOLD", 6800, now.minusDays(15), 32, new BigDecimal("8720.00"), "ACTIVE", "新客", 1L);
        addMember(4L, "赵六", "13800000004", "GOLD", 5500, now.minusDays(50), 28, new BigDecimal("6540.00"), "DORMANT", "沉默", 1L);
        addMember(5L, "钱七", "13800000005", "GOLD", 4900, now.minusDays(60), 25, new BigDecimal("5200.00"), "CHURNED", "流失", 1L);

        addMember(6L, "孙八", "13800000006", "SILVER", 4200, now.minusDays(5), 22, new BigDecimal("4380.00"), "ACTIVE", "新客", 1L);
        addMember(7L, "周九", "13800000007", "SILVER", 3800, now.minusDays(10), 18, new BigDecimal("3650.00"), "ACTIVE", "高频", 1L);
        addMember(8L, "吴十", "13800000008", "SILVER", 3500, now.minusDays(20), 15, new BigDecimal("3200.00"), "ACTIVE", "沉默", 1L);
        addMember(9L, "郑一", "13800000009", "SILVER", 3100, now.minusDays(35), 12, new BigDecimal("2850.00"), "DORMANT", "沉默", 1L);
        addMember(10L, "王二", "13800000010", "SILVER", 2700, now.minusDays(45), 10, new BigDecimal("2400.00"), "CHURNED", "流失", 1L);
        addMember(11L, "李三", "13800000011", "SILVER", 2500, now.minusDays(55), 8, new BigDecimal("2100.00"), "CHURNED", "流失", 1L);

        addMember(12L, "陈四", "13800000012", "NORMAL", 2100, now.minusDays(2), 8, new BigDecimal("1850.00"), "ACTIVE", "新客", 1L);
        addMember(13L, "林五", "13800000013", "NORMAL", 1800, now.minusDays(6), 6, new BigDecimal("1520.00"), "ACTIVE", "新客", 1L);
        addMember(14L, "黄六", "13800000014", "NORMAL", 1500, now.minusDays(12), 5, new BigDecimal("1280.00"), "ACTIVE", "沉默", 1L);
        addMember(15L, "刘七", "13800000015", "NORMAL", 1200, now.minusDays(25), 4, new BigDecimal("980.00"), "ACTIVE", "沉默", 1L);
        addMember(16L, "杨八", "13800000016", "NORMAL", 900, now.minusDays(40), 3, new BigDecimal("720.00"), "DORMANT", "沉默", 1L);
        addMember(17L, "朱九", "13800000017", "NORMAL", 600, now.minusDays(55), 2, new BigDecimal("450.00"), "CHURNED", "流失", 1L);
        addMember(18L, "秦十", "13800000018", "NORMAL", 400, now.minusDays(70), 1, new BigDecimal("320.00"), "CHURNED", "流失", 1L);

        addMember(19L, "许一", "13800000019", "GOLD", 9100, now.minusDays(1), 50, new BigDecimal("15200.00"), "ACTIVE", "高频", 1L);
        addMember(20L, "何二", "13800000020", "SILVER", 3300, now.minusDays(8), 14, new BigDecimal("3080.00"), "ACTIVE", "新客", 1L);
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

    @Override
    public Map<String, Object> getRetentionOverview(Long tenantId) {
        List<RetentionMember> members = filterByTenant(tenantId);
        Map<String, Object> overview = new HashMap<>();

        long goldCount = members.stream().filter(m -> "GOLD".equals(m.getLevel())).count();
        long silverCount = members.stream().filter(m -> "SILVER".equals(m.getLevel())).count();
        long normalCount = members.stream().filter(m -> "NORMAL".equals(m.getLevel())).count();
        long activeCount = members.stream().filter(m -> "ACTIVE".equals(m.getStatus())).count();
        long dormantCount = members.stream().filter(m -> "DORMANT".equals(m.getStatus())).count();
        long churnedCount = members.stream().filter(m -> "CHURNED".equals(m.getStatus())).count();

        BigDecimal avgPoints = BigDecimal.ZERO;
        if (!members.isEmpty()) {
            int totalPoints = members.stream().mapToInt(RetentionMember::getPoints).sum();
            avgPoints = BigDecimal.valueOf(totalPoints).divide(BigDecimal.valueOf(members.size()), 2, RoundingMode.HALF_UP);
        }

        BigDecimal totalSpent = members.stream()
                .map(RetentionMember::getTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

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
        return overview;
    }

    @Override
    public List<Map<String, Object>> getMemberList(Long tenantId, String level, String status) {
        List<RetentionMember> members = filterByTenant(tenantId);

        if (level != null && !level.isEmpty()) {
            final String filterLevel = level;
            members = members.stream()
                    .filter(m -> filterLevel.equals(m.getLevel()))
                    .collect(Collectors.toList());
        }

        if (status != null && !status.isEmpty()) {
            final String filterStatus = status;
            members = members.stream()
                    .filter(m -> filterStatus.equals(m.getStatus()))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (RetentionMember m : members) {
            result.add(memberToMap(m));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getPointsRanking(Long tenantId) {
        List<RetentionMember> members = filterByTenant(tenantId);

        List<RetentionMember> sorted = members.stream()
                .sorted(Comparator.comparingInt(RetentionMember::getPoints).reversed())
                .collect(Collectors.toList());

        int limit = Math.min(20, sorted.size());
        List<RetentionMember> top = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            top.add(sorted.get(i));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (RetentionMember m : top) {
            Map<String, Object> map = memberToMap(m);
            map.put("rank", rank++);
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getChurnWarning(Long tenantId) {
        List<RetentionMember> members = filterByTenant(tenantId);
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> result = new ArrayList<>();
        for (RetentionMember m : members) {
            if (m.getLastOrderDate() != null) {
                Period period = Period.between(m.getLastOrderDate(), today);
                int daysSince = period.getDays();
                if (daysSince > 30) {
                    Map<String, Object> map = memberToMap(m);
                    map.put("daysSinceLastOrder", daysSince);
                    result.add(map);
                }
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getSmartRecommend(Long tenantId) {
        List<RetentionMember> members = filterByTenant(tenantId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (RetentionMember m : members) {
            if (shouldRecommend(m)) {
                Map<String, Object> rec = memberToMap(m);
                String recommendType = getRecommendCouponType(m);
                String recommendReason = getRecommendReason(m);
                rec.put("couponType", recommendType);
                rec.put("reason", recommendReason);
                result.add(rec);
            }
        }
        return result;
    }

    @Override
    public R<Void> sendCoupon(Long memberId) {
        log.info("Sending coupon to member: {}", memberId);
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
        log.info("Batch sending coupon to {} members", memberIds.size());
        R<Void> r = new R<>();
        r.setCode(1);
        r.setMsg("Coupons sent to " + memberIds.size() + " members");
        return r;
    }

    // ==================== Private Helper Methods ====================

    private List<RetentionMember> filterByTenant(Long tenantId) {
        if (tenantId == null) {
            return new ArrayList<>(MOCK_MEMBERS);
        }
        final Long targetTenant = tenantId;
        return MOCK_MEMBERS.stream()
                .filter(m -> targetTenant.equals(m.getTenantId()))
                .collect(Collectors.toList());
    }

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
