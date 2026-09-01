package com.reggie.module.urgency.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.module.auth.model.Employee;
import com.reggie.module.auth.service.EmployeeService;
import com.reggie.module.dining.model.DiningTable;
import com.reggie.module.dining.service.DiningTableService;
import com.reggie.module.notification.service.NotificationService;
import com.reggie.module.order.mapper.OrderDetailMapper;
import com.reggie.module.order.mapper.OrderMapper;
import com.reggie.module.order.model.OrderDetail;
import com.reggie.module.order.model.Orders;
import com.reggie.module.urgency.mapper.UrgencyMapper;
import com.reggie.module.urgency.model.UrgencyRecord;
import com.reggie.module.urgency.service.UrgencyService;
import com.reggie.module.sys.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 催单服务实现
 * 看板数据源为真实订单表（orders）与催单记录表（urgency_record），不再使用 Mock 数据。
 *
 * @author reggie
 * @since 2026-08-23
 */
@Slf4j
@Service
public class UrgencyServiceImpl implements UrgencyService {

    @Autowired
    private UrgencyMapper urgencyMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private DiningTableService diningTableService;

    /** 系统配置服务（读取漏单预警阈值等租户配置） */
    @Autowired
    private SystemConfigService systemConfigService;

    /** 通知服务（店长短信/APP推送告警） */
    @Autowired
    private NotificationService notificationService;

    /** 员工服务（查询店长手机号） */
    @Autowired
    private EmployeeService employeeService;

    /** Redis（告警去重状态，fail-closed：不可用时跳过主动通知） */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /** 每人每天最大催单次数 */
    private static final int MAX_URGENCY_PER_DAY = 3;

    /** 记录列表查询上限 */
    private static final int RECORD_QUERY_LIMIT = 50;

    /** 超时阈值（分钟）：等待超过该值判定为超时订单 */
    private static final int OVERDUE_THRESHOLD_MINUTES = 15;

    /** 紧急阈值（分钟）：等待超过该值判定为紧急订单 */
    private static final int URGENT_THRESHOLD_MINUTES = 10;

    /** 未接单黄金时长（分钟）默认值：超过该值进入预警 */
    private static final int UNACCEPTED_GOLDEN_DEFAULT_MINUTES = 3;
    /** 未接单告警时长（分钟）默认值：超过该值升级告警 */
    private static final int UNACCEPTED_ALARM_DEFAULT_MINUTES = 10;
    /** 未接单漏单时长（分钟）默认值：超过该值（3 倍黄金时长）自动转漏单并升级告警 */
    private static final int UNACCEPTED_MISSED_DEFAULT_MINUTES = 9;

    /** 告警去重集合 Key 前缀（Redis Set，成员=订单ID） */
    private static final String ALERT_GOLDEN_KEY_PREFIX = "urgency:alert:golden:";
    private static final String ALERT_MISSED_KEY_PREFIX = "urgency:alert:missed:";
    /** 告警去重集合 TTL（12 小时，跨天订单无需长期保留） */
    private static final long ALERT_SET_TTL_SECONDS = 12 * 3600L;

    /** 进行中订单状态（待接单/派送中） */
    private static final List<Integer> PENDING_STATUSES = Arrays.asList(
            Orders.STATUS_ORDERED, Orders.STATUS_DELIVERING);

    /** 已完成的订单状态 */
    private static final List<Integer> DONE_STATUSES = Collections.singletonList(Orders.STATUS_COMPLETED);

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
        result.put("todayCount", (todayCount != null ? todayCount : 0) + 1);
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

    // ==================== 催菜看板（真实订单数据） ====================

    /**
     * 获取催单概览统计（基于今日真实订单）
     * 统计今日进行中订单的紧急数、超时数、平均/最长等待时间
     *
     * @param tenantId 租户ID
     * @return 概览数据
     */
    @Override
    public Map<String, Object> getUrgencyOverview(Long tenantId) {
        Map<String, Object> overview = new HashMap<>();
        List<Orders> pendingOrders = listTodayOrders(tenantId, PENDING_STATUSES);
        List<Orders> todayOrders = listTodayOrders(tenantId, mergeStatuses());

        LocalDateTime now = LocalDateTime.now();
        long urgentCount = 0;
        long overdueCount = 0;
        long totalWaitMinutes = 0;
        long maxWaitMinutes = 0;

        for (Orders order : pendingOrders) {
            long waitMinutes = waitMinutes(order, now);
            totalWaitMinutes += waitMinutes;
            if (waitMinutes > maxWaitMinutes) {
                maxWaitMinutes = waitMinutes;
            }
            if (waitMinutes >= URGENT_THRESHOLD_MINUTES) {
                urgentCount++;
            }
            if (waitMinutes >= OVERDUE_THRESHOLD_MINUTES) {
                overdueCount++;
            }
        }

        long avgWaitMinutes = pendingOrders.isEmpty() ? 0 : totalWaitMinutes / pendingOrders.size();

        overview.put("urgentCount", urgentCount);
        overview.put("overdueCount", overdueCount);
        overview.put("avgWaitMin", avgWaitMinutes);
        overview.put("maxWaitMin", maxWaitMinutes);
        overview.put("totalOrders", todayOrders.size());
        overview.put("cookingCount", pendingOrders.size());
        overview.put("completedCount", todayOrders.size() - pendingOrders.size());
        return overview;
    }

    /**
     * 获取未接单实时监控看板（商家主动漏单预警）
     * <p>仅查询当前租户"待接单"(STATUS_ORDERED)订单（不含已接单配送中），
     * 按等待时长分级（正常/预警/告警）返回，供接单大屏轮询展示与语音播报。
     * 阈值取自系统配置(order.unaccepted.golden/alarm.minutes)，缺省用类常量。</p>
     *
     * @param tenantId 租户ID
     * @return 监控数据（stats 统计 + list 订单列表 + thresholds 阈值）
     */
    @Override
    public Map<String, Object> getUnacceptedMonitor(Long tenantId) {
        int goldenMinutes = parseIntConfig("order.unaccepted.golden.minutes", UNACCEPTED_GOLDEN_DEFAULT_MINUTES);
        int alarmMinutes = parseIntConfig("order.unaccepted.alarm.minutes", UNACCEPTED_ALARM_DEFAULT_MINUTES);
        int missedMinutes = parseIntConfig("order.unaccepted.missed.minutes",
                Math.max(UNACCEPTED_MISSED_DEFAULT_MINUTES, goldenMinutes * 3));

        // 仅查"待接单"状态（不含已接单配送中），避免把配送中订单误判为漏单
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getStatus, Orders.STATUS_ORDERED)
               .eq(Orders::getTenantId, tenantId)
               .orderByAsc(Orders::getOrderTime);
        List<Orders> orders = orderMapper.selectList(wrapper);

        LocalDateTime now = LocalDateTime.now();
        Map<Long, String> tableNames = loadTableNames(orders);
        Map<Long, String> dishNamesMap = loadDishNames(orders, tenantId);

        long normalCount = 0;
        long warningCount = 0;
        long alarmCount = 0;
        long missedCount = 0;
        long totalWaitMinutes = 0;
        long maxWaitMinutes = 0;
        List<Map<String, Object>> list = new ArrayList<>();
        for (Orders order : orders) {
            long wait = waitMinutes(order, now);
            totalWaitMinutes += wait;
            if (wait > maxWaitMinutes) {
                maxWaitMinutes = wait;
            }
            String level = resolveLevel(wait, goldenMinutes, alarmMinutes, missedMinutes);
            if ("MISSED".equals(level)) {
                missedCount++;
            } else if ("ALARM".equals(level)) {
                alarmCount++;
            } else if ("WARNING".equals(level)) {
                warningCount++;
            } else {
                normalCount++;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("id", order.getId());
            item.put("orderId", order.getId());
            item.put("orderNo", order.getNumber());
            item.put("orderTime", order.getOrderTime() != null ? order.getOrderTime() : order.getCreateTime());
            item.put("waitMinutes", wait);
            item.put("level", level);
            item.put("isOverdue", wait >= alarmMinutes);
            item.put("isMissed", "MISSED".equals(level));
            item.put("customerName", order.getUserName());
            item.put("phone", order.getPhone());
            item.put("amount", order.getAmount());
            item.put("source", order.getSource());
            item.put("tableNo", order.getTableId() == null ? "--" : tableNames.getOrDefault(order.getTableId(), "--"));
            item.put("dishNames", dishNamesMap.getOrDefault(order.getId(), ""));
            list.add(item);
        }
        long avgWaitMinutes = orders.isEmpty() ? 0 : totalWaitMinutes / orders.size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", (long) orders.size());
        stats.put("normal", normalCount);
        stats.put("warning", warningCount);
        stats.put("alarm", alarmCount);
        stats.put("missed", missedCount);
        stats.put("avgWaitMin", avgWaitMinutes);
        stats.put("maxWaitMin", maxWaitMinutes);

        Map<String, Object> thresholds = new HashMap<>();
        thresholds.put("goldenMinutes", goldenMinutes);
        thresholds.put("alarmMinutes", alarmMinutes);
        thresholds.put("missedMinutes", missedMinutes);

        Map<String, Object> result = new HashMap<>();
        result.put("stats", stats);
        result.put("list", list);
        result.put("thresholds", thresholds);
        result.put("hasAlarm", (alarmCount + missedCount) > 0);
        result.put("hasMissed", missedCount > 0);
        return result;
    }

    /**
     * 定时扫描待接单订单并主动告警（漏单预警核心）
     * <p>对超黄金时长仍未接单的订单通知店长（短信/通知记录），对超漏单阈值的订单升级漏单告警。
     * Redis Set 去重保证每订单只通知一次；Redis 不可用时跳过本轮主动通知（fail-closed）。</p>
     *
     * @param tenantId 租户ID
     * @return 本轮新触发的通知数
     */
    @Override
    public int scanUnacceptedAndAlert(Long tenantId) {
        if (redisTemplate == null) {
            log.warn("[漏单预警] Redis不可用，跳过主动通知: tenantId={}", tenantId);
            return 0;
        }
        int goldenMinutes = parseIntConfig("order.unaccepted.golden.minutes", UNACCEPTED_GOLDEN_DEFAULT_MINUTES);
        int alarmMinutes = parseIntConfig("order.unaccepted.alarm.minutes", UNACCEPTED_ALARM_DEFAULT_MINUTES);
        int missedMinutes = parseIntConfig("order.unaccepted.missed.minutes",
                Math.max(UNACCEPTED_MISSED_DEFAULT_MINUTES, goldenMinutes * 3));

        // 仅查"待接单"状态，避免把配送中订单误判为漏单
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getStatus, Orders.STATUS_ORDERED)
               .eq(Orders::getTenantId, tenantId);
        List<Orders> orders = orderMapper.selectList(wrapper);
        if (orders.isEmpty()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        Set<Long> pendingIds = new HashSet<>();
        int alerted = 0;
        for (Orders order : orders) {
            pendingIds.add(order.getId());
            long wait = waitMinutes(order, now);
            String level = resolveLevel(wait, goldenMinutes, alarmMinutes, missedMinutes);
            if ("MISSED".equals(level)) {
                // 漏单升级：首次触发才通知
                if (firstAlert("missed", tenantId, order.getId())) {
                    notifyManagers(tenantId, "【漏单告警】订单 " + tail(order.getNumber())
                            + " 已等待 " + wait + " 分钟仍未接单，已升级为漏单，请立即处理！");
                    log.warn("[漏单预警] 漏单升级告警: tenantId={}, orderId={}, orderNo={}, waitMinutes={}",
                            tenantId, order.getId(), order.getNumber(), wait);
                    alerted++;
                }
            } else if (wait >= goldenMinutes) {
                // 超黄金时长未接单：首次触发才通知
                if (firstAlert("golden", tenantId, order.getId())) {
                    notifyManagers(tenantId, "您有订单待接单超时：订单 " + tail(order.getNumber())
                            + " 已等待 " + wait + " 分钟，请及时接单");
                    log.warn("[漏单预警] 超时未接单通知: tenantId={}, orderId={}, orderNo={}, waitMinutes={}",
                            tenantId, order.getId(), order.getNumber(), wait);
                    alerted++;
                }
            }
        }
        // 清理：已不在待接单状态的订单，从两个告警去重集合中移除，避免集合无限增长
        cleanupAlertSets(tenantId, pendingIds);
        return alerted;
    }

    /**
     * 解析未接单等级：漏单(MISSED) &gt; 告警(ALARM) &gt; 预警(WARNING) &gt; 正常(NORMAL)
     * 默认配置下漏单阈值=3 倍黄金时长，故 ALARM 档仅在漏单阈值高于告警阈值时出现。
     */
    private String resolveLevel(long waitMinutes, int goldenMinutes, int alarmMinutes, int missedMinutes) {
        if (waitMinutes >= missedMinutes) {
            return "MISSED";
        }
        if (waitMinutes >= alarmMinutes) {
            return "ALARM";
        }
        if (waitMinutes >= goldenMinutes) {
            return "WARNING";
        }
        return "NORMAL";
    }

    /**
     * 判断订单是否首次触发对应等级告警（Redis SADD 原子去重，返回 true 表示首次加入）
     */
    private boolean firstAlert(String level, Long tenantId, Long orderId) {
        try {
            String key = ("missed".equals(level) ? ALERT_MISSED_KEY_PREFIX : ALERT_GOLDEN_KEY_PREFIX) + tenantId;
            Long added = redisTemplate.opsForSet().add(key, String.valueOf(orderId));
            if (added != null && added > 0) {
                redisTemplate.expire(key, ALERT_SET_TTL_SECONDS, TimeUnit.SECONDS);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("[漏单预警] 告警去重失败(Redis异常): tenantId={}, orderId={}, level={}, error={}",
                    tenantId, orderId, level, e.getMessage());
            return false;
        }
    }

    /**
     * 清理告警去重集合：移除已不在待接单状态的订单ID，避免集合无限增长
     */
    private void cleanupAlertSets(Long tenantId, Set<Long> pendingIds) {
        if (pendingIds == null || pendingIds.isEmpty()) {
            return;
        }
        try {
            String goldenKey = ALERT_GOLDEN_KEY_PREFIX + tenantId;
            String missedKey = ALERT_MISSED_KEY_PREFIX + tenantId;
            Set<Object> goldenMembers = redisTemplate.opsForSet().members(goldenKey);
            if (goldenMembers != null) {
                for (Object member : goldenMembers) {
                    Long id = parseLongId(member);
                    if (id != null && !pendingIds.contains(id)) {
                        redisTemplate.opsForSet().remove(goldenKey, member);
                    }
                }
            }
            Set<Object> missedMembers = redisTemplate.opsForSet().members(missedKey);
            if (missedMembers != null) {
                for (Object member : missedMembers) {
                    Long id = parseLongId(member);
                    if (id != null && !pendingIds.contains(id)) {
                        redisTemplate.opsForSet().remove(missedKey, member);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[漏单预警] 告警集合清理失败: tenantId={}, error={}", tenantId, e.getMessage());
        }
    }

    /**
     * 将 Redis 集合成员（订单ID字符串）解析为 Long
     */
    private Long parseLongId(Object member) {
        if (member == null) {
            return null;
        }
        try {
            return Long.parseLong(member.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 通知店长（当前租户下状态正常的全部员工，取其手机号发短信并落通知记录）
     * 短信默认 Mock 模式仅打日志，生产需配置阿里云短信凭证并关闭 mock-mode。
     */
    private void notifyManagers(Long tenantId, String content) {
        try {
            List<Employee> managers = employeeService.lambdaQuery()
                    .eq(Employee::getTenantId, tenantId)
                    .eq(Employee::getStatus, 1)
                    .isNotNull(Employee::getPhone)
                    .ne(Employee::getPhone, "")
                    .list();
            if (managers.isEmpty()) {
                log.info("[漏单预警] 未找到可通知的店长: tenantId={}", tenantId);
                return;
            }
            List<String> phones = new ArrayList<>();
            for (Employee m : managers) {
                if (m.getPhone() != null && !m.getPhone().trim().isEmpty()) {
                    phones.add(m.getPhone().trim());
                }
            }
            if (phones.isEmpty()) {
                return;
            }
            notificationService.sendSimpleMessage(1, phones, "未接单告警", content);
            log.info("[漏单预警] 已通知店长: tenantId={}, phones={}", tenantId, phones);
        } catch (Exception e) {
            log.error("[漏单预警] 通知店长失败: tenantId={}, error={}", tenantId, e.getMessage());
        }
    }

    /**
     * 订单号尾部截取（便于语音播报/短信展示）
     */
    private String tail(String orderNo) {
        if (orderNo == null || orderNo.length() <= 6) {
            return orderNo == null ? "--" : orderNo;
        }
        return orderNo.substring(orderNo.length() - 6);
    }

    /**
     * 解析整型系统配置，缺失或非法时返回默认值
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    private int parseIntConfig(String key, int defaultValue) {
        String val = systemConfigService.getConfigOrDefault(key, null);
        if (val == null || val.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("[漏单预警] 非法配置值 key={}, val={}, 使用默认 {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 获取催单列表（基于今日真实订单）
     * 按等待时间降序排列，支持状态筛选
     *
     * @param tenantId 租户ID
     * @param status   状态筛选（COOKING/WAITING_CALL/DONE，空为全部）
     * @return 催单列表
     */
    @Override
    public List<Map<String, Object>> getUrgencyList(Long tenantId, String status) {
        List<Orders> orders;
        if ("DONE".equals(status)) {
            orders = listTodayOrders(tenantId, DONE_STATUSES);
        } else if ("COOKING".equals(status) || "WAITING_CALL".equals(status)) {
            orders = listTodayOrders(tenantId, PENDING_STATUSES);
        } else {
            orders = listTodayOrders(tenantId, mergeStatuses());
        }

        // 等待时间降序排列
        final LocalDateTime now = LocalDateTime.now();
        orders.sort((a, b) -> Long.compare(waitMinutes(b, now), waitMinutes(a, now)));

        // 批量加载桌名与菜品名
        Map<Long, String> tableNames = loadTableNames(orders);
        Map<Long, String> dishNamesMap = loadDishNames(orders, tenantId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Orders order : orders) {
            Map<String, Object> item = new HashMap<>();
            String orderStatus = mapOrderStatus(order.getStatus());
            long waitMinutes = waitMinutes(order, now);
            item.put("id", order.getId());
            item.put("orderId", order.getId());
            item.put("orderNo", order.getNumber());
            item.put("tableNo", order.getTableId() == null ? "--" : tableNames.getOrDefault(order.getTableId(), "--"));
            item.put("customerName", order.getUserName());
            item.put("dishNames", dishNamesMap.getOrDefault(order.getId(), ""));
            item.put("status", orderStatus);
            item.put("statusDesc", getStatusDesc(orderStatus));
            item.put("createTime", order.getOrderTime() != null ? order.getOrderTime() : order.getCreateTime());
            item.put("waitMinutes", waitMinutes);
            item.put("progressPercent", estimateProgress(order.getStatus()));
            item.put("isOverdue", waitMinutes >= OVERDUE_THRESHOLD_MINUTES);
            result.add(item);
        }
        return result;
    }

    /**
     * 员工催单操作：真实写入催单记录（memberId=0 表示员工操作）
     *
     * @param orderId 订单ID
     * @return 操作结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Void> callNext(Long orderId) {
        Long tenantId = BaseContext.getCurrentTenantId();
        log.info("[催单] 员工发起催单: orderId={}, tenantId={}", orderId, tenantId);

        if (orderId == null) {
            return R.error("订单ID不能为空");
        }

        Orders order = orderMapper.selectById(orderId);
        if (order == null || !tenantId.equals(order.getTenantId())) {
            return R.error("订单不存在或无权操作");
        }

        LambdaQueryWrapper<UrgencyRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UrgencyRecord::getOrderId, orderId)
                .eq(UrgencyRecord::getTenantId, tenantId);
        UrgencyRecord existingRecord = urgencyMapper.selectOne(wrapper);

        if (existingRecord != null) {
            Integer prevTimes = existingRecord.getTimes();
            existingRecord.setTimes((prevTimes != null ? prevTimes : 0) + 1);
            existingRecord.setStatus("SENT");
            existingRecord.setUpdateTime(LocalDateTime.now());
            urgencyMapper.updateById(existingRecord);
            log.info("[催单] 员工催单更新记录: id={}, times={}", existingRecord.getId(), existingRecord.getTimes());
        } else {
            UrgencyRecord record = new UrgencyRecord();
            record.setOrderId(orderId);
            record.setMemberId(0L);
            record.setOrderNo(order.getNumber());
            record.setTimes(1);
            record.setStatus("SENT");
            record.setTenantId(tenantId);
            record.setCreateTime(LocalDateTime.now());
            record.setUpdateTime(LocalDateTime.now());
            urgencyMapper.insert(record);
            log.info("[催单] 员工催单新增记录: id={}", record.getId());
        }
        return R.success(null);
    }

    /**
     * 查看催单详情（基于真实订单数据）
     *
     * @param orderId  订单ID
     * @param tenantId 租户ID
     * @return 订单详情含制作进度
     */
    @Override
    public Map<String, Object> getUrgencyDetail(Long orderId, Long tenantId) {
        Map<String, Object> detail = new HashMap<>();
        if (orderId == null) {
            return detail;
        }

        List<Map<String, Object>> list = getUrgencyList(tenantId, null);
        for (Map<String, Object> item : list) {
            if (orderId.equals(item.get("orderId"))) {
                detail.putAll(item);
                detail.remove("id");
                return detail;
            }
        }
        return detail;
    }

    /**
     * 获取叫号排队列表（今日堂食/排队/预订进行中订单）
     *
     * @param tenantId 租户ID
     * @return 排队数据（当前叫号/等待列表）
     */
    @Override
    public Map<String, Object> getQueueList(Long tenantId) {
        Map<String, Object> queue = new HashMap<>();

        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getTenantId, tenantId)
                .ge(Orders::getCreateTime, LocalDate.now().atStartOfDay())
                .in(Orders::getStatus, PENDING_STATUSES)
                .in(Orders::getSource, Arrays.asList("EAT_IN", "QUEUE", "RESERVATION"))
                .orderByAsc(Orders::getCreateTime);
        List<Orders> pendingDineIn = orderMapper.selectList(wrapper);

        Map<Long, String> tableNames = loadTableNames(pendingDineIn);

        List<Map<String, Object>> queueList = new ArrayList<>();
        for (Orders order : pendingDineIn) {
            Map<String, Object> item = new HashMap<>();
            item.put("tableNo", order.getTableId() == null ? "--" : tableNames.getOrDefault(order.getTableId(), "--"));
            item.put("status", "COOKING");
            item.put("statusDesc", "制作中");
            queueList.add(item);
        }

        String currentTableNo = queueList.isEmpty() ? "--" : String.valueOf(queueList.get(0).get("tableNo"));
        queue.put("currentTableNo", currentTableNo);
        queue.put("waitCount", queueList.size());
        queue.put("queueList", queueList);
        return queue;
    }

    /**
     * 获取催单统计汇总（基于今日真实催单记录）
     *
     * @param tenantId 租户ID
     * @return 催单统计（今日总数/完成率/平均响应时间）
     */
    @Override
    public Map<String, Object> getUrgencySummary(Long tenantId) {
        Map<String, Object> summary = new HashMap<>();
        LocalDate today = LocalDate.now();

        Integer todayTotal = urgencyMapper.countToday(tenantId, today);
        Integer todayProcessed = urgencyMapper.countTodayProcessed(tenantId, today);
        BigDecimal avgResponse = urgencyMapper.avgResponseTime(tenantId);

        int todayCount = todayTotal == null ? 0 : todayTotal;
        int processedCount = todayProcessed == null ? 0 : todayProcessed;
        int completionRate = todayCount == 0 ? 0 : (int) Math.round(processedCount * 100.0 / todayCount);
        double avgResponseMin = avgResponse == null ? 0.0 : avgResponse.doubleValue();

        summary.put("todayTotal", todayCount);
        summary.put("completeRate", completionRate);
        summary.put("avgResponseMin", avgResponseMin);
        return summary;
    }

    // ==================== 私有方法 ====================

    /**
     * 合并进行中与已完成状态列表
     */
    private List<Integer> mergeStatuses() {
        List<Integer> statuses = new ArrayList<>(PENDING_STATUSES);
        statuses.addAll(DONE_STATUSES);
        return statuses;
    }

    /**
     * 查询今日（按创建时间）指定状态的订单列表
     */
    private List<Orders> listTodayOrders(Long tenantId, List<Integer> statuses) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getTenantId, tenantId)
                .ge(Orders::getCreateTime, LocalDate.now().atStartOfDay())
                .in(Orders::getStatus, statuses);
        return orderMapper.selectList(wrapper);
    }

    /**
     * 计算订单已等待分钟数（取下单时间，缺失时用创建时间）
     */
    private long waitMinutes(Orders order, LocalDateTime now) {
        LocalDateTime base = order.getOrderTime() != null ? order.getOrderTime() : order.getCreateTime();
        if (base == null) {
            return 0;
        }
        long minutes = Duration.between(base, now).toMinutes();
        return Math.max(0, minutes);
    }

    /**
     * 批量加载桌台名称映射
     */
    private Map<Long, String> loadTableNames(List<Orders> orders) {
        Map<Long, String> tableNames = new HashMap<>();
        if (orders == null || orders.isEmpty()) {
            return tableNames;
        }
        List<Long> tableIds = new ArrayList<>();
        for (Orders order : orders) {
            if (order.getTableId() != null && !tableIds.contains(order.getTableId())) {
                tableIds.add(order.getTableId());
            }
        }
        if (tableIds.isEmpty()) {
            return tableNames;
        }
        List<DiningTable> tables = diningTableService.listByIds(tableIds);
        for (DiningTable table : tables) {
            tableNames.put(table.getId(), table.getName());
        }
        return tableNames;
    }

    /**
     * 批量加载订单菜品名称（按 orderId 聚合，逗号分隔）
     */
    private Map<Long, String> loadDishNames(List<Orders> orders, Long tenantId) {
        Map<Long, String> dishNamesMap = new HashMap<>();
        if (orders == null || orders.isEmpty()) {
            return dishNamesMap;
        }
        List<Long> orderIds = new ArrayList<>();
        for (Orders order : orders) {
            orderIds.add(order.getId());
        }

        LambdaQueryWrapper<OrderDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OrderDetail::getOrderId, orderIds)
                .eq(OrderDetail::getTenantId, tenantId)
                .orderByAsc(OrderDetail::getId);
        List<OrderDetail> details = orderDetailMapper.selectList(wrapper);

        Map<Long, List<String>> grouped = new HashMap<>();
        for (OrderDetail detail : details) {
            List<String> names = grouped.get(detail.getOrderId());
            if (names == null) {
                names = new ArrayList<>();
                grouped.put(detail.getOrderId(), names);
            }
            names.add(detail.getName());
        }
        for (Map.Entry<Long, List<String>> entry : grouped.entrySet()) {
            dishNamesMap.put(entry.getKey(), String.join("、", entry.getValue()));
        }
        return dishNamesMap;
    }

    /**
     * 订单状态映射为看板状态：进行中=COOKING，已完成=DONE
     */
    private String mapOrderStatus(Integer status) {
        if (status != null && status == Orders.STATUS_COMPLETED) {
            return "DONE";
        }
        return "COOKING";
    }

    /**
     * 制作进度估算：待接单=30%，派送中=70%，已完成=100%
     */
    private Integer estimateProgress(Integer status) {
        if (status == null) {
            return 0;
        }
        if (status == Orders.STATUS_COMPLETED) {
            return 100;
        }
        if (status == Orders.STATUS_DELIVERING) {
            return 70;
        }
        return 30;
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
        } else if ("DONE".equals(status)) {
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
