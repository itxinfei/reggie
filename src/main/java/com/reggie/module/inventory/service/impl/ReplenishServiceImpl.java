package com.reggie.module.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.reggie.common.BaseContext;
import com.reggie.common.ObjectMapperHolder;
import com.reggie.enums.StockRecordType;
import com.reggie.module.inventory.model.Material;
import com.reggie.module.inventory.model.StockRecord;
import com.reggie.module.inventory.service.MaterialService;
import com.reggie.module.inventory.service.ReplenishService;
import com.reggie.module.inventory.service.StockRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.reggie.module.inventory.model.Material.STATUS_NORMAL;

/**
 * 智能补货服务实现
 * <p>
 * 核心算法：加权日均消耗 + 安全库存 + 补货周期 - 当前库存
 * - 加权日均消耗：近7天 weight=1.5，其余 weight=1.0
 * - 安全库存：日均消耗 × 2（应急2天）
 * - 建议采购量：日均消耗 × 补货周期 + 安全库存 - 当前库存
 * </p>
 * <p>
 * Redis 缓存：Key=inventory:replenish:suggest:{tenantId}，TTL=10分钟
 * </p>
 *
 * @author reggie
 * @since 2026-08-23
 */
@Slf4j
@Service
public class ReplenishServiceImpl implements ReplenishService {

    /** Redis 缓存 Key 前缀 */
    private static final String REDIS_KEY_PREFIX = "inventory:replenish:suggest:";

    /** 缓存 TTL：10 分钟 */
    private static final long CACHE_TTL_SECONDS = 600L;

    /** 安全库存倍数：日均消耗 × 2 天 */
    private static final BigDecimal SAFETY_STOCK_DAYS = new BigDecimal("2");

    /** 近7天权重 */
    private static final BigDecimal WEIGHT_RECENT = new BigDecimal("1.5");

    /** 其余天权重 */
    private static final BigDecimal WEIGHT_NORMAL = new BigDecimal("1.0");

    /** 统计天数默认值 */
    private static final int DEFAULT_DAYS = 30;

    /** 补货周期默认值 */
    private static final int DEFAULT_REPLENISH_CYCLE = 14;

    @Autowired
    private MaterialService materialService;

    @Autowired
    private StockRecordService stockRecordService;

    /**
     * Redis 模板，可选注入（Redis 不可用时降级为直接计算）
     */
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, Object> getReplenishDashboard(Long tenantId) {
        log.info("[智能补货] 获取补货看板数据，tenantId={}", tenantId);

        List<Map<String, Object>> suggestions = getSmartReplenishSuggest(tenantId, DEFAULT_DAYS, DEFAULT_REPLENISH_CYCLE);

        Map<String, Object> dashboard = new HashMap<String, Object>();

        if (CollectionUtils.isEmpty(suggestions)) {
            dashboard.put("totalSuggestCount", 0);
            dashboard.put("urgentCount", 0);
            dashboard.put("criticalCount", 0);
            dashboard.put("warningCount", 0);
            dashboard.put("totalAmount", BigDecimal.ZERO);
            dashboard.put("top5", new ArrayList<Map<String, Object>>());
            dashboard.put("suggestions", new ArrayList<Map<String, Object>>());
            return dashboard;
        }

        int urgentCount = 0;
        int criticalCount = 0;
        int warningCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Map<String, Object> item : suggestions) {
            String urgency = (String) item.get("urgency");
            int urgencyLevel = parseUrgencyLevel(urgency);

            if (urgencyLevel == 3) {
                urgentCount++;
            } else if (urgencyLevel == 2) {
                criticalCount++;
            } else if (urgencyLevel == 1) {
                warningCount++;
            }

            // 补货总金额 = suggestQty × unitPrice
            BigDecimal suggestQty = getBigDecimal(item, "suggestQty");
            BigDecimal unitPrice = getBigDecimal(item, "unitPrice");
            if (suggestQty != null && unitPrice != null) {
                totalAmount = totalAmount.add(suggestQty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP));
            }
        }

        // TOP5 即将断货（按 estimatedDays 升序取前5）
        List<Map<String, Object>> top5 = getTop5ByEstimatedDays(suggestions);

        dashboard.put("totalSuggestCount", suggestions.size());
        dashboard.put("urgentCount", urgentCount);
        dashboard.put("criticalCount", criticalCount);
        dashboard.put("warningCount", warningCount);
        dashboard.put("totalAmount", totalAmount);
        dashboard.put("top5", top5);
        dashboard.put("suggestions", suggestions);

        return dashboard;
    }

    @Override
    public List<Map<String, Object>> getSmartReplenishSuggest(Long tenantId, int days, int replenishCycle) {
        log.info("[智能补货] 获取补货建议，tenantId={}, days={}, replenishCycle={}", tenantId, days, replenishCycle);

        if (days <= 0) {
            days = DEFAULT_DAYS;
        }
        if (replenishCycle <= 0) {
            replenishCycle = DEFAULT_REPLENISH_CYCLE;
        }

        // 尝试从 Redis 缓存读取
        List<Map<String, Object>> cached = tryGetFromCache(tenantId, days, replenishCycle);
        if (cached != null) {
            log.info("[智能补货] 命中 Redis 缓存，tenantId={}", tenantId);
            return cached;
        }

        // 查询所有启用的食材（租户隔离）
        LambdaQueryWrapper<Material> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(Material::getTenantId, tenantId);
        }
        qw.eq(Material::getStatus, STATUS_NORMAL);
        List<Material> allMaterials = materialService.list(qw);

        if (CollectionUtils.isEmpty(allMaterials)) {
            List<Map<String, Object>> empty = new ArrayList<Map<String, Object>>();
            tryPutToCache(tenantId, days, replenishCycle, empty);
            return empty;
        }

        // 查询近 N 天所有出库记录
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<StockRecord> outQw = new LambdaQueryWrapper<>();
        outQw.eq(StockRecord::getType, StockRecordType.OUT.getValue());
        outQw.ge(StockRecord::getCreatedTime, since);
        if (tenantId != null) {
            outQw.eq(StockRecord::getTenantId, tenantId);
        }
        List<StockRecord> outRecords = stockRecordService.list(outQw);

        // 计算补货建议
        List<Map<String, Object>> suggestList = new ArrayList<Map<String, Object>>();
        BigDecimal replenishCycleDays = new BigDecimal(replenishCycle);

        for (Material m : allMaterials) {
            BigDecimal stockQty = m.getStockQty() != null ? m.getStockQty() : BigDecimal.ZERO;
            BigDecimal dailyUsage = calcWeightedDailyUsage(m.getId(), days, tenantId);

            // 安全库存 = 日均消耗 × 2
            BigDecimal safetyStock = dailyUsage.multiply(SAFETY_STOCK_DAYS).setScale(2, RoundingMode.HALF_UP);

            // 预计可用天数
            int estimatedDays = calcEstimatedDays(stockQty, dailyUsage);

            // 紧急度等级
            String urgency = calcUrgency(estimatedDays);

            // 建议采购量 = 日均消耗 × 补货周期 + 安全库存 - 当前库存
            BigDecimal suggestQty = dailyUsage.multiply(replenishCycleDays)
                    .add(safetyStock)
                    .subtract(stockQty)
                    .setScale(2, RoundingMode.HALF_UP);

            // 只有需要补货的食材才列入建议（suggestQty > 0）
            if (suggestQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Map<String, Object> item = new HashMap<String, Object>();
            item.put("materialId", m.getId());
            item.put("materialName", m.getName());
            item.put("unit", m.getUnit());
            item.put("stockQty", stockQty);
            item.put("dailyUsage", dailyUsage);
            item.put("safetyStock", safetyStock);
            item.put("estimatedDays", estimatedDays);
            item.put("urgency", urgency);
            item.put("suggestQty", suggestQty);
            item.put("unitPrice", m.getUnitPrice());
            item.put("supplierId", m.getSupplierId());
            item.put("supplierName", m.getSupplierName());
            item.put("replenishCycle", replenishCycle);
            suggestList.add(item);
        }

        // 排序：按紧急度等级降序（紧急在前），同等级按 estimatedDays 升序
        sortByUrgency(suggestList);

        // 写入 Redis 缓存
        tryPutToCache(tenantId, days, replenishCycle, suggestList);

        log.info("[智能补货] 补货建议计算完成，tenantId={}, 共{}种食材需补货", tenantId, suggestList.size());
        return suggestList;
    }

    @Override
    public BigDecimal calcWeightedDailyUsage(Long materialId, int days, Long tenantId) {
        if (days <= 0) {
            days = DEFAULT_DAYS;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime since = now.minusDays(days);

        // 查询该食材近 N 天出库记录
        LambdaQueryWrapper<StockRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(StockRecord::getMaterialId, materialId);
        qw.eq(StockRecord::getType, StockRecordType.OUT.getValue());
        qw.ge(StockRecord::getCreatedTime, since);
        qw.le(StockRecord::getCreatedTime, now);
        if (tenantId != null) {
            qw.eq(StockRecord::getTenantId, tenantId);
        }
        List<StockRecord> records = stockRecordService.list(qw);

        if (CollectionUtils.isEmpty(records)) {
            return BigDecimal.ZERO;
        }

        // 按天汇总出库量：key=日期字符串(yyyy-MM-dd), value=当天出库总量
        Map<String, BigDecimal> dailyOutMap = new HashMap<String, BigDecimal>();
        for (StockRecord r : records) {
            LocalDateTime createdTime = r.getCreatedTime();
            if (createdTime == null || r.getQty() == null) {
                continue;
            }
            // 使用日期字符串作为 key（JDK 1.8 兼容）
            String dateKey = formatLocalDate(createdTime.toLocalDate());
            BigDecimal existing = dailyOutMap.get(dateKey);
            if (existing == null) {
                existing = BigDecimal.ZERO;
            }
            dailyOutMap.put(dateKey, existing.add(r.getQty()));
        }

        if (dailyOutMap.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 计算加权日均消耗
        // 近7天 weight=1.5，其余 weight=1.0
        BigDecimal totalWeighted = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : dailyOutMap.entrySet()) {
            String dateKey = entry.getKey();
            BigDecimal dailyOut = entry.getValue();

            // 判断该日期距离今天的天数
            int daysAgo = calcDaysAgo(dateKey, now);
            BigDecimal weight = daysAgo <= 7 ? WEIGHT_RECENT : WEIGHT_NORMAL;

            totalWeighted = totalWeighted.add(dailyOut.multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal weightedDailyUsage = totalWeighted.divide(totalWeight, 4, RoundingMode.HALF_UP);
        log.debug("[智能补货] materialId={}, 加权日均消耗={}, 加权总量={}, 权重总量={}",
                materialId, weightedDailyUsage, totalWeighted, totalWeight);
        return weightedDailyUsage;
    }

    @Override
    public int calcEstimatedDays(BigDecimal stockQty, BigDecimal dailyUsage) {
        if (stockQty == null) {
            stockQty = BigDecimal.ZERO;
        }
        if (dailyUsage == null || dailyUsage.compareTo(BigDecimal.ZERO) <= 0) {
            // 日均消耗为0或空，表示该食材暂无消耗记录，视为无限期可用
            return Integer.MAX_VALUE;
        }
        BigDecimal result = stockQty.divide(dailyUsage, 0, RoundingMode.FLOOR);
        return result.intValue();
    }

    @Override
    public String calcUrgency(int estimatedDays) {
        if (estimatedDays <= 1) {
            return "3"; // 紧急：≤1天
        } else if (estimatedDays <= 3) {
            return "2"; // 紧迫：≤3天
        } else if (estimatedDays <= 7) {
            return "1"; // 关注：≤7天
        }
        return "0"; // 充足：>7天
    }

    // ==================== 私有方法 ====================

    /**
     * 从 Redis 缓存中尝试读取补货建议数据
     * <p>
     * 缓存格式：JSON 数组字符串
     * Key: inventory:replenish:suggest:{tenantId}
     * </p>
     *
     * @return 缓存中的数据列表，未命中或 Redis 不可用时返回 null
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tryGetFromCache(Long tenantId, int days, int replenishCycle) {
        if (redisTemplate == null || tenantId == null) {
            return null;
        }
        String redisKey = REDIS_KEY_PREFIX + tenantId;
        try {
            Object cached = redisTemplate.opsForValue().get(redisKey);
            if (cached == null) {
                return null;
            }
            String json;
            if (cached instanceof String) {
                json = (String) cached;
            } else {
                json = ObjectMapperHolder.getDefault().writeValueAsString(cached);
            }
            com.fasterxml.jackson.databind.ObjectMapper mapper = ObjectMapperHolder.getDefault();
            Object parsed = mapper.readValue(json, Map.class);
            // 如果反序列化后是数组，转为 List<Map>
            if (parsed instanceof List) {
                return (List<Map<String, Object>>) parsed;
            }
            return null;
        } catch (Exception e) {
            log.warn("[智能补货] Redis 读取缓存失败，降级为直接计算：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 将补货建议数据写入 Redis 缓存
     *
     * @param tenantId       租户ID
     * @param days           统计天数
     * @param replenishCycle 补货周期
     * @param data           补货建议数据
     */
    @SuppressWarnings("unchecked")
    private void tryPutToCache(Long tenantId, int days, int replenishCycle, List<Map<String, Object>> data) {
        if (redisTemplate == null || tenantId == null) {
            return;
        }
        String redisKey = REDIS_KEY_PREFIX + tenantId;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = ObjectMapperHolder.getDefault();
            String json = mapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(redisKey, json, CACHE_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            log.debug("[智能补货] 补货建议数据已缓存，redisKey={}, size={}", redisKey, data.size());
        } catch (JsonProcessingException e) {
            log.warn("[智能补货] JSON 序列化失败，跳过缓存：{}", e.getMessage());
        } catch (Exception e) {
            log.warn("[智能补货] Redis 写入缓存失败，降级：{}", e.getMessage());
        }
    }

    /**
     * 按紧急度等级降序、预计可用天数升序对列表排序
     * 使用冒泡排序（JDK 1.8 兼容，且数据量通常不大）
     */
    private void sortByUrgency(List<Map<String, Object>> list) {
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < list.size() - 1 - i; j++) {
                Map<String, Object> a = list.get(j);
                Map<String, Object> b = list.get(j + 1);

                int urgencyA = parseUrgencyLevel((String) a.get("urgency"));
                int urgencyB = parseUrgencyLevel((String) b.get("urgency"));

                boolean shouldSwap = false;
                if (urgencyB > urgencyA) {
                    shouldSwap = true;
                } else if (urgencyB == urgencyA) {
                    // 同等级按 estimatedDays 升序（断货风险大的在前）
                    int daysA = (Integer) a.get("estimatedDays");
                    int daysB = (Integer) b.get("estimatedDays");
                    if (daysB < daysA) {
                        shouldSwap = true;
                    }
                }

                if (shouldSwap) {
                    Map<String, Object> tmp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, tmp);
                }
            }
        }
    }

    /**
     * 按预计可用天数升序取前5（即将断货TOP5）
     */
    private List<Map<String, Object>> getTop5ByEstimatedDays(List<Map<String, Object>> suggestions) {
        if (CollectionUtils.isEmpty(suggestions)) {
            return new ArrayList<Map<String, Object>>();
        }

        // 先按 estimatedDays 升序排序（副本）
        List<Map<String, Object>> sorted = new ArrayList<Map<String, Object>>(suggestions);
        sortByEstimatedDays(sorted);

        int limit = Math.min(5, sorted.size());
        List<Map<String, Object>> top5 = new ArrayList<Map<String, Object>>(limit);
        for (int i = 0; i < limit; i++) {
            top5.add(sorted.get(i));
        }
        return top5;
    }

    /**
     * 按预计可用天数升序排序（冒泡排序）
     */
    private void sortByEstimatedDays(List<Map<String, Object>> list) {
        for (int i = 0; i < list.size(); i++) {
            for (int j = 0; j < list.size() - 1 - i; j++) {
                Map<String, Object> a = list.get(j);
                Map<String, Object> b = list.get(j + 1);

                int daysA = (Integer) a.get("estimatedDays");
                int daysB = (Integer) b.get("estimatedDays");
                if (daysB < daysA) {
                    Map<String, Object> tmp = list.get(j);
                    list.set(j, list.get(j + 1));
                    list.set(j + 1, tmp);
                }
            }
        }
    }

    /**
     * 解析紧急度等级为整数
     */
    private int parseUrgencyLevel(String urgency) {
        if (urgency == null) {
            return 0;
        }
        try {
            return Integer.parseInt(urgency);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 安全获取 Map 中的 BigDecimal 值
     */
    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 格式化 LocalDate 为 yyyy-MM-dd 字符串（JDK 1.8 兼容）
     */
    private String formatLocalDate(java.time.LocalDate date) {
        return String.valueOf(date); // JDK 1.8 的 LocalDate.toString() 已返回 yyyy-MM-dd
    }

    /**
     * 计算日期字符串距离今天的天数
     *
     * @param dateKey 日期字符串 yyyy-MM-dd
     * @param now     当前时间
     * @return 天数差（≥0）
     */
    private int calcDaysAgo(String dateKey, LocalDateTime now) {
        java.time.LocalDate today = now.toLocalDate();
        java.time.LocalDate target = java.time.LocalDate.parse(dateKey);
        long diff = java.time.temporal.ChronoUnit.DAYS.between(target, today);
        return diff >= 0 ? (int) diff : 0;
    }
}