package com.reggie.module.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.delivery.mapper.DeliveryRangeRuleMapper;
import com.reggie.module.delivery.model.DeliveryRangeRule;
import com.reggie.module.delivery.model.DeliveryFeeStep;
import com.reggie.module.delivery.mapper.DeliveryFeeStepMapper;
import com.reggie.module.delivery.service.DeliveryEnhancedService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送增强服务实现
 *
 * @author reggie
 * @since 2026-08-11
 */
@Slf4j
@Service
public class DeliveryEnhancedServiceImpl extends ServiceImpl<DeliveryRangeRuleMapper, DeliveryRangeRule> 
        implements DeliveryEnhancedService {

    @Autowired
    private DeliveryRangeRuleMapper rangeRuleMapper;

    @Autowired
    private DeliveryFeeStepMapper feeStepMapper;

    // ==================== 配送范围管理 ====================

    @Override
    public List<DeliveryRangeRule> getRangeRules(Long tenantId) {
        LambdaQueryWrapper<DeliveryRangeRule> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(DeliveryRangeRule::getTenantId, tenantId);
        }
        qw.eq(DeliveryRangeRule::getStatus, 1);
        qw.orderByAsc(DeliveryRangeRule::getSortOrder);
        return rangeRuleMapper.selectList(qw);
    }

    @Override
    public DeliveryRangeRule getRangeRuleById(Long id) {
        return rangeRuleMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateRangeRule(DeliveryRangeRule rule) {
        if (rule.getId() == null) {
            rule.setCreateTime(LocalDateTime.now());
            rule.setUpdateTime(LocalDateTime.now());
            return rangeRuleMapper.insert(rule) > 0;
        } else {
            rule.setUpdateTime(LocalDateTime.now());
            return rangeRuleMapper.updateById(rule) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRangeRule(Long id) {
        return rangeRuleMapper.deleteById(id) > 0;
    }

    // ==================== 配送费阶梯管理 ====================

    @Override
    public List<DeliveryFeeStep> getFeeSteps(Long ruleId, Long tenantId) {
        LambdaQueryWrapper<DeliveryFeeStep> qw = new LambdaQueryWrapper<>();
        if (ruleId != null) {
            qw.eq(DeliveryFeeStep::getRuleId, ruleId);
        }
        if (tenantId != null) {
            qw.eq(DeliveryFeeStep::getTenantId, tenantId);
        }
        qw.orderByAsc(DeliveryFeeStep::getSortOrder);
        return feeStepMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateFeeStep(DeliveryFeeStep step) {
        if (step.getId() == null) {
            step.setCreateTime(LocalDateTime.now());
            step.setUpdateTime(LocalDateTime.now());
            return feeStepMapper.insert(step) > 0;
        } else {
            step.setUpdateTime(LocalDateTime.now());
            return feeStepMapper.updateById(step) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFeeStep(Long id) {
        return feeStepMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveFeeSteps(List<DeliveryFeeStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return true;
        }
        for (DeliveryFeeStep step : steps) {
            saveOrUpdateFeeStep(step);
        }
        return true;
    }

    // ==================== 配送范围校验 ====================

    @Override
    public boolean isInRange(Long ruleId, BigDecimal longitude, BigDecimal latitude) {
        DeliveryRangeRule rule = rangeRuleMapper.selectById(ruleId);
        if (rule == null || rule.getStatus() != 1) {
            return false;
        }

        if (rule.getRangeType() == DeliveryRangeRule.TYPE_CIRCLE) {
            // 圆形范围校验
            return isPointInCircle(longitude, latitude, 
                    rule.getCenterLongitude(), rule.getCenterLatitude(), rule.getRadius());
        } else if (rule.getRangeType() == DeliveryRangeRule.TYPE_POLYGON) {
            // 多边形范围校验
            return isPointInPolygon(longitude, latitude, rule.getPolygonPoints());
        }

        return false;
    }

    @Override
    public Long findMatchingRule(BigDecimal longitude, BigDecimal latitude, Long tenantId) {
        List<DeliveryRangeRule> rules = getRangeRules(tenantId);
        for (DeliveryRangeRule rule : rules) {
            if (isInRange(rule.getId(), longitude, latitude)) {
                return rule.getId();
            }
        }
        return null;
    }

    // ==================== 配送费计算 ====================

    @Override
    public BigDecimal calculateDeliveryFee(Long ruleId, BigDecimal distance, BigDecimal orderAmount) {
        DeliveryRangeRule rule = rangeRuleMapper.selectById(ruleId);
        if (rule == null) {
            return BigDecimal.ZERO;
        }

        // 检查是否满足免费配送条件
        if (rule.getFreeThreshold() != null && orderAmount.compareTo(rule.getFreeThreshold()) >= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal fee = BigDecimal.ZERO;

        if (rule.getFeeType() == 1) {
            // 固定配送费
            fee = rule.getBaseFee() != null ? rule.getBaseFee() : BigDecimal.ZERO;
        } else if (rule.getFeeType() == 2) {
            // 距离阶梯配送费
            fee = calculateStepFee(ruleId, distance);
        } else if (rule.getFeeType() == 3) {
            // 基础费 + 距离费
            BigDecimal baseFee = rule.getBaseFee() != null ? rule.getBaseFee() : BigDecimal.ZERO;
            BigDecimal distanceFee = BigDecimal.ZERO;
            if (rule.getFeePerKm() != null && distance.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal distanceKm = distance.divide(new BigDecimal("1000"), 4, RoundingMode.HALF_UP);
                distanceFee = distanceKm.multiply(rule.getFeePerKm());
            }
            fee = baseFee.add(distanceFee);
        }

        // 限制最低和最高配送费
        if (rule.getMinFee() != null && fee.compareTo(rule.getMinFee()) < 0) {
            fee = rule.getMinFee();
        }
        if (rule.getMaxFee() != null && fee.compareTo(rule.getMaxFee()) > 0) {
            fee = rule.getMaxFee();
        }

        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public Map<String, Object> calculateFee(BigDecimal longitude, BigDecimal latitude, BigDecimal distance,
                                            BigDecimal orderAmount, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 查找匹配的配送范围规则
        Long ruleId = findMatchingRule(longitude, latitude, tenantId);
        if (ruleId == null) {
            result.put("inRange", false);
            result.put("fee", BigDecimal.ZERO);
            result.put("message", "地址不在配送范围内");
            return result;
        }

        DeliveryRangeRule rule = rangeRuleMapper.selectById(ruleId);
        BigDecimal fee = calculateDeliveryFee(ruleId, distance, orderAmount);

        result.put("inRange", true);
        result.put("ruleId", ruleId);
        result.put("ruleName", rule.getRuleName());
        result.put("distance", distance);
        result.put("fee", fee);
        result.put("freeThreshold", rule.getFreeThreshold());
        result.put("isFree", fee.compareTo(BigDecimal.ZERO) == 0);

        return result;
    }

    @Override
    public BigDecimal calculateDistance(BigDecimal lon1, BigDecimal lat1, BigDecimal lon2, BigDecimal lat2) {
        // 使用 Haversine 公式计算两点间距离
        double earthRadius = 6371000; // 地球半径（米）

        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue())) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadius * c;

        return new BigDecimal(distance).setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 统计分析 ====================

    @Override
    public Map<String, Object> getDeliveryStatistics(Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<DeliveryRangeRule> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(DeliveryRangeRule::getTenantId, tenantId);
        }
        List<DeliveryRangeRule> rules = rangeRuleMapper.selectList(qw);

        int totalRules = rules.size();
        int activeRules = 0;
        int circleRules = 0;
        int polygonRules = 0;

        for (DeliveryRangeRule rule : rules) {
            if (rule.getStatus() == 1) {
                activeRules++;
            }
            if (rule.getRangeType() == DeliveryRangeRule.TYPE_CIRCLE) {
                circleRules++;
            } else if (rule.getRangeType() == DeliveryRangeRule.TYPE_POLYGON) {
                polygonRules++;
            }
        }

        result.put("totalRules", totalRules);
        result.put("activeRules", activeRules);
        result.put("circleRules", circleRules);
        result.put("polygonRules", polygonRules);

        return result;
    }

    @Override
    public Map<String, Object> getRangeCoverage(Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        List<DeliveryRangeRule> rules = getRangeRules(tenantId);
        List<Map<String, Object>> coverageList = new ArrayList<>();

        for (DeliveryRangeRule rule : rules) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", rule.getId());
            item.put("ruleName", rule.getRuleName());
            item.put("rangeType", rule.getRangeType());
            item.put("rangeTypeName", rule.getRangeType() == 1 ? "圆形" : "多边形");

            if (rule.getRangeType() == DeliveryRangeRule.TYPE_CIRCLE && rule.getRadius() != null) {
                // 计算圆形面积（平方米）
                double area = Math.PI * rule.getRadius().doubleValue() * rule.getRadius().doubleValue();
                item.put("area", new BigDecimal(area).setScale(2, RoundingMode.HALF_UP));
                item.put("radius", rule.getRadius());
            }

            coverageList.add(item);
        }

        result.put("rules", coverageList);
        result.put("totalRules", rules.size());

        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 判断点是否在圆形范围内
     */
    private boolean isPointInCircle(BigDecimal pointLon, BigDecimal pointLat,
                                     BigDecimal centerLon, BigDecimal centerLat, BigDecimal radius) {
        BigDecimal distance = calculateDistance(pointLon, pointLat, centerLon, centerLat);
        return distance.compareTo(radius) <= 0;
    }

    /**
     * 判断点是否在多边形范围内（射线法）
     */
    private boolean isPointInPolygon(BigDecimal pointLon, BigDecimal pointLat, String polygonPointsJson) {
        if (polygonPointsJson == null || polygonPointsJson.isEmpty()) {
            return false;
        }

        // 解析多边形坐标点
        // 格式：[[lon1,lat1],[lon2,lat2],...]
        try {
            String[] points = polygonPointsJson.replace("],[", "|")
                    .replace("[[", "").replace("]]", "").split("\\|");

            int n = points.length;
            if (n < 3) {
                return false;
            }

            boolean inside = false;
            double testX = pointLon.doubleValue();
            double testY = pointLat.doubleValue();

            for (int i = 0, j = n - 1; i < n; j = i++) {
                String[] pointI = points[i].split(",");
                String[] pointJ = points[j].split(",");

                double xi = Double.parseDouble(pointI[0]);
                double yi = Double.parseDouble(pointI[1]);
                double xj = Double.parseDouble(pointJ[0]);
                double yj = Double.parseDouble(pointJ[1]);

                if (((yi > testY) != (yj > testY)) &&
                    (testX < (xj - xi) * (testY - yi) / (yj - yi) + xi)) {
                    inside = !inside;
                }
            }

            return inside;
        } catch (Exception e) {
            log.error("解析多边形坐标失败", e);
            return false;
        }
    }

    /**
     * 计算阶梯配送费
     */
    private BigDecimal calculateStepFee(Long ruleId, BigDecimal distance) {
        List<DeliveryFeeStep> steps = getFeeSteps(ruleId, null);
        if (steps.isEmpty()) {
            return BigDecimal.ZERO;
        }

        for (DeliveryFeeStep step : steps) {
            if (step.getStartDistance() != null && step.getEndDistance() != null) {
                if (distance.compareTo(step.getStartDistance()) >= 0 &&
                    distance.compareTo(step.getEndDistance()) <= 0) {
                    BigDecimal fee = step.getFee();
                    // 计算超出部分的费用
                    if (step.getIncrementDistance() != null && step.getIncrementFee() != null) {
                        BigDecimal extraDistance = distance.subtract(step.getStartDistance());
                        if (extraDistance.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal increments = extraDistance.divide(step.getIncrementDistance(), 0, RoundingMode.CEILING);
                            fee = fee.add(increments.multiply(step.getIncrementFee()));
                        }
                    }
                    return fee;
                }
            }
        }

        // 如果没有匹配的阶梯，使用最后一个阶梯的费用
        DeliveryFeeStep lastStep = steps.get(steps.size() - 1);
        return lastStep.getFee() != null ? lastStep.getFee() : BigDecimal.ZERO;
    }
}

