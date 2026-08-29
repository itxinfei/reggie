package com.reggie.module.urgency.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.urgency.dto.UrgencyFrequencyVO;
import com.reggie.module.urgency.dto.UrgencyRecordVO;
import com.reggie.module.urgency.dto.UrgencyStatsVO;
import com.reggie.module.urgency.mapper.UrgencyMapper;
import com.reggie.module.urgency.model.UrgencyOrder;
import com.reggie.module.urgency.model.UrgencyRecord;
import com.reggie.module.urgency.service.UrgencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 催单服务实现
 * 提供催单记录持久化、频率控制、催单统计等能力
 *
 * @author reggie
 * @since 2026-08-23
 */
@Slf4j
@Service
public class UrgencyServiceImpl implements UrgencyService {

    @Autowired
    private UrgencyMapper urgencyMapper;

    /** 每人每天最大催单次数 */
    private static final int MAX_URGENCY_PER_DAY = 3;

    /** 记录列表查询上限 */
    private static final int RECORD_QUERY_LIMIT = 50;

    /** Mock 订单数据 */
    private static final List<UrgencyOrder> MOCK_ORDERS = new ArrayList<>();

    static {
        LocalDateTime now = LocalDateTime.now();

        UrgencyOrder order1 = new UrgencyOrder();
        order1.setId(1001L);
        order1.setOrderId(2001L);
        order1.setOrderNo("ORD202608230001");
        order1.setTableNo("08");
        order1.setCustomerName("张先生");
        order1.setDishNames("红烧肉,清炒时蔬,紫菜蛋花汤");
        order1.setStatus("COOKING");
        order1.setCreateTime(now.minusMinutes(12));
        order1.setEstimatedFinishTime(now.plusMinutes(3));
        order1.setProgressPercent(45);
        order1.setTenantId(1L);
        MOCK_ORDERS.add(order1);

        UrgencyOrder order2 = new UrgencyOrder();
        order2.setId(1002L);
        order2.setOrderId(2002L);
        order2.setOrderNo("ORD202608230002");
        order2.setTableNo("03");
        order2.setCustomerName("李女士");
        order2.setDishNames("麻辣香锅,蒜蓉西兰花");
        order2.setStatus("COOKING");
        order2.setCreateTime(now.minusMinutes(20));
        order2.setEstimatedFinishTime(now.plusMinutes(5));
        order2.setProgressPercent(70);
        order2.setTenantId(1L);
        MOCK_ORDERS.add(order2);

        UrgencyOrder order3 = new UrgencyOrder();
        order3.setId(1003L);
        order3.setOrderId(2003L);
        order3.setOrderNo("ORD202608230003");
        order3.setTableNo("12");
        order3.setCustomerName("王先生");
        order3.setDishNames("宫保鸡丁,番茄炒蛋,米饭x2");
        order3.setStatus("WAITING_CALL");
        order3.setCreateTime(now.minusMinutes(15));
        order3.setEstimatedFinishTime(now.minusMinutes(2));
        order3.setProgressPercent(95);
        order3.setTenantId(1L);
        MOCK_ORDERS.add(order3);

        UrgencyOrder order4 = new UrgencyOrder();
        order4.setId(1004L);
        order4.setOrderId(2004L);
        order4.setOrderNo("ORD202608230004");
        order4.setTableNo("05");
        order4.setCustomerName("赵女士");
        order4.setDishNames("烤鱼,拍黄瓜,冬瓜排骨汤");
        order4.setStatus("COOKING");
        order4.setCreateTime(now.minusMinutes(5));
        order4.setEstimatedFinishTime(now.plusMinutes(10));
        order4.setProgressPercent(30);
        order4.setTenantId(1L);
        MOCK_ORDERS.add(order4);

        UrgencyOrder order5 = new UrgencyOrder();
        order5.setId(1005L);
        order5.setOrderId(2005L);
        order5.setOrderNo("ORD202608230005");
        order5.setTableNo("01");
        order5.setCustomerName("陈女士");
        order5.setDishNames("水煮鱼,凉拌木耳");
        order5.setStatus("COMPLETED");
        order5.setCreateTime(now.minusMinutes(30));
        order5.setEstimatedFinishTime(now.minusMinutes(8));
        order5.setProgressPercent(100);
        order5.setTenantId(1L);
        MOCK_ORDERS.add(order5);

        UrgencyOrder order6 = new UrgencyOrder();
        order6.setId(1006L);
        order6.setOrderId(2006L);
        order6.setOrderNo("ORD202608230006");
        order6.setTableNo("07");
        order6.setCustomerName("刘先生");
        order6.setDishNames("牛肉面,卤蛋,酸辣土豆丝");
        order6.setStatus("COOKING");
        order6.setCreateTime(now.minusMinutes(18));
        order6.setEstimatedFinishTime(now.plusMinutes(1));
        order6.setProgressPercent(80);
        order6.setTenantId(1L);
        MOCK_ORDERS.add(order6);

        UrgencyOrder order7 = new UrgencyOrder();
        order7.setId(1007L);
        order7.setOrderId(2007L);
        order7.setOrderNo("ORD202608230007");
        order7.setTableNo("10");
        order7.setCustomerName("杨女士");
        order7.setDishNames("红烧排骨,炒河粉");
        order7.setStatus("WAITING_CALL");
        order7.setCreateTime(now.minusMinutes(25));
        order7.setEstimatedFinishTime(now.minusMinutes(5));
        order7.setProgressPercent(98);
        order7.setTenantId(1L);
        MOCK_ORDERS.add(order7);

        UrgencyOrder order8 = new UrgencyOrder();
        order8.setId(1008L);
        order8.setOrderId(2008L);
        order8.setOrderNo("ORD202608230008");
        order8.setTableNo("02");
        order8.setCustomerName("孙先生");
        order8.setDishNames("剁椒鱼头,清蒸鲈鱼,白灼生菜");
        order8.setStatus("COOKING");
        order8.setCreateTime(now.minusMinutes(22));
        order8.setEstimatedFinishTime(now.plusMinutes(2));
        order8.setProgressPercent(85);
        order8.setTenantId(1L);
        MOCK_ORDERS.add(order8);
    }

    // ==================== 催单记录持久化与频率控制 ====================

    /**
     * 发起催单操作，含频率控制
     * 每人每天最多催单 MAX_URGENCY_PER_DAY 次，超出则返回错误
     *
     * @param orderId  订单ID
     * @param memberId 会员ID
     * @param orderNo  订单号
     * @return 催单结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Map<String, Object>> triggerUrgency(Long orderId, Long memberId, String orderNo) {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 发起催单: orderId={}, memberId={}, orderNo={}, tenantId={}", orderId, memberId, orderNo, tenantId);

        if (orderId == null) {
            return R.error("订单ID不能为空");
        }
        if (memberId == null) {
            return R.error("会员ID不能为空");
        }
        if (orderNo == null || orderNo.trim().isEmpty()) {
            return R.error("订单号不能为空");
        }

        // 频率控制检查
        LocalDate today = LocalDate.now();
        Integer todayCount = urgencyMapper.countTodayByMember(memberId, tenantId, today);

        if (todayCount != null && todayCount >= MAX_URGENCY_PER_DAY) {
            log.warn("[催单] 频率限制: memberId={} 今日已催单{}次, 上限{}", memberId, todayCount, MAX_URGENCY_PER_DAY);
            Map<String, Object> data = new HashMap<>();
            data.put("canUrgency", false);
            data.put("todayCount", todayCount);
            data.put("maxAllowed", MAX_URGENCY_PER_DAY);
            return R.error("催单过于频繁，今日剩余次数不足（今日最多" + MAX_URGENCY_PER_DAY + "次）");
        }

        // 查询该订单是否已有催单记录
        LambdaQueryWrapper<UrgencyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UrgencyRecord::getOrderId, orderId)
                .eq(UrgencyRecord::getTenantId, tenantId);
        UrgencyRecord existingRecord = urgencyMapper.selectOne(wrapper);

        if (existingRecord != null && "SENT".equals(existingRecord.getStatus())) {
            // 已有未处理记录，次数+1
            // 防御性 null 检查：times 可能在数据库中为 null（历史数据或外部导入）
            Integer prevTimes = existingRecord.getTimes();
            existingRecord.setTimes((prevTimes != null ? prevTimes : 0) + 1);
            existingRecord.setUpdateTime(LocalDateTime.now());
            urgencyMapper.updateById(existingRecord);
            log.info("[催单] 更新催单记录: id={}, times={}", existingRecord.getId(), existingRecord.getTimes());
        } else {
            // 新建催单记录
            UrgencyRecord record = new UrgencyRecord();
            record.setOrderId(orderId);
            record.setMemberId(memberId);
            record.setOrderNo(orderNo);
            record.setTimes(1);
            record.setStatus("SENT");
            record.setTenantId(tenantId);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            urgencyMapper.insert(record);
            log.info("[催单] 新增催单记录: id={}, orderId={}, memberId={}", record.getId(), orderId, memberId);
        }

        // 返回催单结果
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("orderId", orderId);
        result.put("memberId", memberId);
        result.put("orderNo", orderNo);
        result.put("todayCount", todayCount + 1);
        result.put("maxAllowed", MAX_URGENCY_PER_DAY);
        result.put("remainCount", MAX_URGENCY_PER_DAY - (todayCount + 1));
        return R.success(result);
    }

    /**
     * 查询催单记录列表
     *
     * @param memberId 会员ID
     * @return 催单记录列表
     */
    @Override
    public R<Map<String, Object>> getUrgencyRecords(Long memberId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 查询催单记录: memberId={}, tenantId={}", memberId, tenantId);

        if (memberId == null) {
            return R.error("会员ID不能为空");
        }

        List<UrgencyRecord> records = urgencyMapper.listByMemberId(memberId, tenantId, RECORD_QUERY_LIMIT);

        List<Map<String, Object>> recordList = new ArrayList<>();
        for (UrgencyRecord record : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", record.getId());
            item.put("orderId", record.getOrderId());
            item.put("orderNo", record.getOrderNo());
            item.put("times", record.getTimes());
            item.put("status", record.getStatus());
            item.put("createTime", record.getCreateTime());
            recordList.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", recordList);
        result.put("total", recordList.size());
        return R.success(result);
    }

    /**
     * 获取催单统计数据
     *
     * @return 催单统计数据
     */
    @Override
    public R<Map<String, Object>> getUrgencyStats() {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 获取催单统计: tenantId={}", tenantId);

        List<Map<String, Object>> statsList = urgencyMapper.getUrgencyStats(tenantId);

        Integer totalUrgency = 0;
        Integer todayUrgency = 0;
        Integer weekUrgency = 0;
        if (statsList != null && !statsList.isEmpty()) {
            Map<String, Object> stats = statsList.get(0);
            totalUrgency = getIntValue(stats, "totalUrgency");
            todayUrgency = getIntValue(stats, "todayUrgency");
            weekUrgency = getIntValue(stats, "weekUrgency");
        }

        // 计算平均响应时间
        BigDecimal avgResponse = urgencyMapper.avgResponseTime(tenantId);
        Double avgResponseTime = (avgResponse != null) ? avgResponse.doubleValue() : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalUrgency", totalUrgency);
        result.put("todayUrgency", todayUrgency);
        result.put("weekUrgency", weekUrgency);
        result.put("avgResponseTime", avgResponseTime);
        return R.success(result);
    }

    /**
     * 频率检查
     *
     * @param memberId 会员ID
     * @return 频率控制信息
     */
    @Override
    public R<Map<String, Object>> checkFrequency(Long memberId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 检查频率: memberId={}, tenantId={}", memberId, tenantId);

        if (memberId == null) {
            return R.error("会员ID不能为空");
        }

        LocalDate today = LocalDate.now();
        Integer todayCount = urgencyMapper.countTodayByMember(memberId, tenantId, today);
        if (todayCount == null) {
            todayCount = 0;
        }

        boolean canUrgency = todayCount < MAX_URGENCY_PER_DAY;
        int remainCount = Math.max(0, MAX_URGENCY_PER_DAY - todayCount);

        Map<String, Object> result = new HashMap<>();
        result.put("memberId", memberId);
        result.put("todayCount", todayCount);
        result.put("maxAllowed", MAX_URGENCY_PER_DAY);
        result.put("canUrgency", canUrgency);
        result.put("remainCount", remainCount);
        return R.success(result);
    }

    // ==================== 以下保留原有 Mock 业务逻辑 ====================

    @Override
    public Map<String, Object> getUrgencyOverview(Long tenantId) {
        Map<String, Object> overview = new HashMap<>();

        List<UrgencyOrder> orders = filterByTenant(tenantId);
        long cookingCount = orders.stream().filter(o -> "COOKING".equals(o.getStatus())).count();
        long waitingCallCount = orders.stream().filter(o -> "WAITING_CALL".equals(o.getStatus())).count();
        long completedCount = orders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).count();

        long overdueCount = orders.stream()
                .filter(o -> !"COMPLETED".equals(o.getStatus()))
                .filter(o -> Duration.between(o.getCreateTime(), LocalDateTime.now()).toMinutes() >= 15)
                .count();

        long urgentCount = orders.stream()
                .filter(o -> "COOKING".equals(o.getStatus()))
                .filter(o -> Duration.between(o.getCreateTime(), LocalDateTime.now()).toMinutes() >= 10)
                .count();

        List<UrgencyOrder> pendingOrders = new ArrayList<>();
        for (UrgencyOrder order : orders) {
            if (!"COMPLETED".equals(order.getStatus())) {
                pendingOrders.add(order);
            }
        }

        long avgMinutes = 0;
        long maxMinutes = 0;
        if (!pendingOrders.isEmpty()) {
            long totalMinutes = 0;
            for (UrgencyOrder order : pendingOrders) {
                long minutes = Duration.between(order.getCreateTime(), LocalDateTime.now()).toMinutes();
                totalMinutes += minutes;
                if (minutes > maxMinutes) {
                    maxMinutes = minutes;
                }
            }
            avgMinutes = totalMinutes / pendingOrders.size();
        }

        overview.put("totalOrders", orders.size());
        overview.put("cookingCount", cookingCount);
        overview.put("waitingCallCount", waitingCallCount);
        overview.put("completedCount", completedCount);
        overview.put("urgentCount", urgentCount);
        overview.put("overdueCount", overdueCount);
        overview.put("avgWaitMinutes", avgMinutes);
        overview.put("maxWaitMinutes", maxMinutes);

        return overview;
    }

    @Override
    public List<Map<String, Object>> getUrgencyList(Long tenantId, String status) {
        List<Map<String, Object>> result = new ArrayList<>();

        List<UrgencyOrder> orders = filterByTenant(tenantId);

        if (status != null && !status.isEmpty()) {
            List<UrgencyOrder> filtered = new ArrayList<>();
            for (UrgencyOrder order : orders) {
                if (status.equals(order.getStatus())) {
                    filtered.add(order);
                }
            }
            orders = filtered;
        }

        orders = sortByWaitTimeDesc(orders);

        LocalDateTime now = LocalDateTime.now();
        for (UrgencyOrder order : orders) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", order.getId());
            item.put("orderId", order.getOrderId());
            item.put("orderNo", order.getOrderNo());
            item.put("tableNo", order.getTableNo());
            item.put("customerName", order.getCustomerName());
            item.put("dishNames", order.getDishNames());
            item.put("status", order.getStatus());
            item.put("statusDesc", getStatusDesc(order.getStatus()));
            item.put("createTime", order.getCreateTime());
            item.put("estimatedFinishTime", order.getEstimatedFinishTime());
            item.put("progressPercent", order.getProgressPercent());
            item.put("waitMinutes", Duration.between(order.getCreateTime(), now).toMinutes());
            result.add(item);
        }

        return result;
    }

    @Override
    public R<Void> callNext(Long orderId) {
        log.info("[催单] 发起催单操作: orderId={}", orderId);

        if (orderId == null) {
            return R.error("订单ID不能为空");
        }

        // 对指定订单发起催单操作
        log.info("[催单] 催单操作成功: orderId={}", orderId);
        return R.success(null);
    }

    @Override
    public Map<String, Object> getUrgencyDetail(Long orderId, Long tenantId) {
        Map<String, Object> detail = new HashMap<>();

        if (orderId == null) {
            return detail;
        }

        List<UrgencyOrder> orders = filterByTenant(tenantId);
        for (UrgencyOrder order : orders) {
            if (orderId.equals(order.getOrderId())) {
                detail.put("id", order.getId());
                detail.put("orderId", order.getOrderId());
                detail.put("orderNo", order.getOrderNo());
                detail.put("tableNo", order.getTableNo());
                detail.put("customerName", order.getCustomerName());
                detail.put("dishNames", order.getDishNames());
                detail.put("status", order.getStatus());
                detail.put("statusDesc", getStatusDesc(order.getStatus()));
                detail.put("createTime", order.getCreateTime());
                detail.put("estimatedFinishTime", order.getEstimatedFinishTime());
                detail.put("progressPercent", order.getProgressPercent());

                LocalDateTime now = LocalDateTime.now();
                detail.put("waitMinutes", Duration.between(order.getCreateTime(), now).toMinutes());

                if (order.getEstimatedFinishTime() != null) {
                    long remainSeconds = Duration.between(now, order.getEstimatedFinishTime()).getSeconds();
                    detail.put("estimatedRemainSeconds", remainSeconds > 0 ? remainSeconds : 0);
                }

                long waitMinutes = Duration.between(order.getCreateTime(), now).toMinutes();
                detail.put("isOverdue", waitMinutes >= 15);

                return detail;
            }
        }

        return detail;
    }

    @Override
    public Map<String, Object> getQueueList(Long tenantId) {
        Map<String, Object> queue = new HashMap<>();

        queue.put("currentTableNo", "08");
        queue.put("waitCount", 3);

        List<Map<String, Object>> queueList = new ArrayList<>();

        Map<String, Object> q1 = new HashMap<>();
        q1.put("tableNo", "09");
        q1.put("status", "COOKING");
        q1.put("statusDesc", "制作中");
        queueList.add(q1);

        Map<String, Object> q2 = new HashMap<>();
        q2.put("tableNo", "11");
        q2.put("status", "WAITING_CALL");
        q2.put("statusDesc", "待叫号");
        queueList.add(q2);

        Map<String, Object> q3 = new HashMap<>();
        q3.put("tableNo", "13");
        q3.put("status", "COOKING");
        q3.put("statusDesc", "制作中");
        queueList.add(q3);

        queue.put("queueList", queueList);
        return queue;
    }

    @Override
    public Map<String, Object> getUrgencySummary(Long tenantId) {
        Map<String, Object> summary = new HashMap<>();

        summary.put("todayTotalCalls", 25);
        summary.put("completionRate", 92);
        summary.put("avgResponseMinutes", 3);

        return summary;
    }

    // ==================== 私有方法 ====================

    /**
     * 按租户过滤订单
     */
    private List<UrgencyOrder> filterByTenant(Long tenantId) {
        List<UrgencyOrder> filtered = new ArrayList<>();
        for (UrgencyOrder order : MOCK_ORDERS) {
            if (tenantId == null || tenantId.equals(order.getTenantId())) {
                filtered.add(order);
            }
        }
        return filtered;
    }

    /**
     * 按等待时间降序排列
     */
    private List<UrgencyOrder> sortByWaitTimeDesc(List<UrgencyOrder> orders) {
        List<UrgencyOrder> sorted = new ArrayList<>(orders);
        sorted.sort((a, b) -> {
            long timeA = Duration.between(a.getCreateTime(), LocalDateTime.now()).toMinutes();
            long timeB = Duration.between(b.getCreateTime(), LocalDateTime.now()).toMinutes();
            return Long.compare(timeB, timeA);
        });
        return sorted;
    }

    /**
     * 状态描述
     */
    private String getStatusDesc(String status) {
        if (status == null) {
            return "";
        }
        if ("COOKING".equals(status)) {
            return "制作中";
        } else if ("WAITING_CALL".equals(status)) {
            return "等待叫号";
        } else if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        return "未知状态";
    }

    /**
     * 从 Map 中安全获取 Integer 值
     */
    private Integer getIntValue(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) {
            return 0;
        }
        Object val = map.get(key);
        if (val instanceof Integer) {
            return (Integer) val;
        }
        if (val instanceof Long) {
            return ((Long) val).intValue();
        }
        return Integer.parseInt(val.toString());
    }
}
