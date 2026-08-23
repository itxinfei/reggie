package com.reggie.module.urgency.service.impl;

import com.reggie.common.R;
import com.reggie.module.urgency.model.UrgencyOrder;
import com.reggie.module.urgency.service.UrgencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 催单服务实现
 * 当前使用 Mock 数据填充，待后续接入真实订单数据源
 *
 * @author reggie
 * @since 2026-08-23
 */
@Slf4j
@Service
public class UrgencyServiceImpl implements UrgencyService {

    /** Mock 订单数据 */
    private static final List<UrgencyOrder> MOCK_ORDERS = new ArrayList<>();

    static {
        LocalDateTime now = LocalDateTime.now();

        // 订单1：制作中，已催单
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

        // 订单2：制作中，超时可催
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

        // 订单3：等待叫号，超时可催
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

        // 订单4：制作中，正常进行
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

        // 订单5：已完成
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

        // 订单6：制作中，已催单
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

        // 订单7：等待叫号
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

        // 订单8：制作中，超时可催
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

    @Override
    public Map<String, Object> getUrgencyOverview(Long tenantId) {
        Map<String, Object> overview = new HashMap<>();

        List<UrgencyOrder> orders = filterByTenant(tenantId);
        long cookingCount = orders.stream().filter(o -> "COOKING".equals(o.getStatus())).count();
        long waitingCallCount = orders.stream().filter(o -> "WAITING_CALL".equals(o.getStatus())).count();
        long completedCount = orders.stream().filter(o -> "COMPLETED".equals(o.getStatus())).count();

        // 超时可催：超过15分钟且未完成
        long overdueCount = orders.stream()
                .filter(o -> !"COMPLETED".equals(o.getStatus()))
                .filter(o -> Duration.between(o.getCreateTime(), LocalDateTime.now()).toMinutes() >= 15)
                .count();

        // 催单中订单（制作中且已超过10分钟）
        long urgentCount = orders.stream()
                .filter(o -> "COOKING".equals(o.getStatus()))
                .filter(o -> Duration.between(o.getCreateTime(), LocalDateTime.now()).toMinutes() >= 10)
                .count();

        // 平均等待时间（仅未完成的订单）
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

        // 按等待时间降序排列
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

        // 模拟催单操作
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

                // 预估剩余时间
                if (order.getEstimatedFinishTime() != null) {
                    long remainSeconds = Duration.between(now, order.getEstimatedFinishTime()).getSeconds();
                    detail.put("estimatedRemainSeconds", remainSeconds > 0 ? remainSeconds : 0);
                }

                // 是否超时可催
                long waitMinutes = Duration.between(order.getCreateTime(), now).toMinutes();
                detail.put("isOverdue", waitMinutes >= 15);

                return detail;
            }
        }

        return detail;
    }

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
     * 获取叫号排队列表（Mock 数据）
     */
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

    /**
     * 获取催单统计汇总（Mock 数据）
     */
    @Override
    public Map<String, Object> getUrgencySummary(Long tenantId) {
        Map<String, Object> summary = new HashMap<>();

        summary.put("todayTotalCalls", 25);
        summary.put("completionRate", 92);
        summary.put("avgResponseMinutes", 3);

        return summary;
    }

    /**
     * 状态描述
     */
    private String getStatusDesc(String status) {
        if (status == null) {
            return "";
        }
        switch (status) {
            case "COOKING":
                return "制作中";
            case "WAITING_CALL":
                return "等待叫号";
            case "COMPLETED":
                return "已完成";
            default:
                return "未知状态";
        }
    }
}
