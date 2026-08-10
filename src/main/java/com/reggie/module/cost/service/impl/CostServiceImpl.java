package com.reggie.module.cost.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.entity.DishCost;
import com.reggie.entity.CostRecord;
import com.reggie.entity.LaborCost;
import com.reggie.entity.OtherCost;
import com.reggie.mapper.DishCostMapper;
import com.reggie.module.cost.service.CostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成本核算服务实现
 *
 * @author reggie
 * @since 2026-08-10
 */
@Slf4j
/**
 * Cost service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class CostServiceImpl extends ServiceImpl<DishCostMapper, DishCost> implements CostService {

    @Autowired
    private DishCostMapper dishCostMapper;

    @Autowired
    private com.reggie.mapper.CostRecordMapper costRecordMapper;

    @Autowired
    private com.reggie.mapper.LaborCostMapper laborCostMapper;

    @Autowired
    private com.reggie.mapper.OtherCostMapper otherCostMapper;

    // ==================== 菜品成本管理 ====================

    @Override
    public List<DishCost> getDishCostList(Long tenantId) {
        LambdaQueryWrapper<DishCost> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(DishCost::getTenantId, tenantId);
        }
        qw.orderByDesc(DishCost::getUpdateTime);
        return dishCostMapper.selectList(qw);
    }

    @Override
    public DishCost getDishCostByDishId(Long dishId, Long tenantId) {
        LambdaQueryWrapper<DishCost> qw = new LambdaQueryWrapper<>();
        qw.eq(DishCost::getDishId, dishId);
        if (tenantId != null) {
            qw.eq(DishCost::getTenantId, tenantId);
        }
        return dishCostMapper.selectOne(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateDishCost(DishCost dishCost) {
        // 计算总成本
        BigDecimal materialCost = dishCost.getMaterialCost() != null ? dishCost.getMaterialCost() : BigDecimal.ZERO;
        BigDecimal laborCost = dishCost.getLaborCost() != null ? dishCost.getLaborCost() : BigDecimal.ZERO;
        BigDecimal otherCost = dishCost.getOtherCost() != null ? dishCost.getOtherCost() : BigDecimal.ZERO;
        dishCost.setTotalCost(materialCost.add(laborCost).add(otherCost));

        // 计算毛利率
        if (dishCost.getSalePrice() != null && dishCost.getSalePrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal profit = dishCost.getSalePrice().subtract(dishCost.getTotalCost());
            BigDecimal profitRate = profit.divide(dishCost.getSalePrice(), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            dishCost.setProfitRate(profitRate);
        }

        // 保存或更新
        if (dishCost.getId() == null) {
            dishCost.setCreateTime(LocalDateTime.now());
            dishCost.setUpdateTime(LocalDateTime.now());
            return dishCostMapper.insert(dishCost) > 0;
        } else {
            dishCost.setUpdateTime(LocalDateTime.now());
            return dishCostMapper.updateById(dishCost) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDishCost(Long id) {
        return dishCostMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateDishCost(List<DishCost> dishCosts) {
        if (dishCosts == null || dishCosts.isEmpty()) {
            return true;
        }
        for (DishCost dishCost : dishCosts) {
            saveOrUpdateDishCost(dishCost);
        }
        return true;
    }

    // ==================== 成本记录管理 ====================

    @Override
    public List<CostRecord> getCostRecordList(Integer costType, LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        LambdaQueryWrapper<CostRecord> qw = new LambdaQueryWrapper<>();
        if (costType != null) {
            qw.eq(CostRecord::getCostType, costType);
        }
        if (startDate != null) {
            qw.ge(CostRecord::getCostDate, startDate);
        }
        if (endDate != null) {
            qw.le(CostRecord::getCostDate, endDate);
        }
        if (tenantId != null) {
            qw.eq(CostRecord::getTenantId, tenantId);
        }
        qw.orderByDesc(CostRecord::getCostDate);
        return costRecordMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveCostRecord(CostRecord costRecord) {
        costRecord.setCreateTime(LocalDateTime.now());
        return costRecordMapper.insert(costRecord) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCostRecord(Long id) {
        return costRecordMapper.deleteById(id) > 0;
    }

    // ==================== 人工成本管理 ====================

    @Override
    public List<LaborCost> getLaborCostList(LocalDate costMonth, Long tenantId) {
        LambdaQueryWrapper<LaborCost> qw = new LambdaQueryWrapper<>();
        if (costMonth != null) {
            qw.eq(LaborCost::getCostMonth, costMonth);
        }
        if (tenantId != null) {
            qw.eq(LaborCost::getTenantId, tenantId);
        }
        qw.orderByDesc(LaborCost::getCostMonth);
        return laborCostMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateLaborCost(LaborCost laborCost) {
        // 计算总成本
        BigDecimal salary = laborCost.getSalary() != null ? laborCost.getSalary() : BigDecimal.ZERO;
        BigDecimal socialInsurance = laborCost.getSocialInsurance() != null ? laborCost.getSocialInsurance() : BigDecimal.ZERO;
        BigDecimal housingFund = laborCost.getHousingFund() != null ? laborCost.getHousingFund() : BigDecimal.ZERO;
        BigDecimal otherBenefits = laborCost.getOtherBenefits() != null ? laborCost.getOtherBenefits() : BigDecimal.ZERO;
        laborCost.setTotalCost(salary.add(socialInsurance).add(housingFund).add(otherBenefits));

        if (laborCost.getId() == null) {
            laborCost.setCreateTime(LocalDateTime.now());
            laborCost.setUpdateTime(LocalDateTime.now());
            return laborCostMapper.insert(laborCost) > 0;
        } else {
            laborCost.setUpdateTime(LocalDateTime.now());
            return laborCostMapper.updateById(laborCost) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteLaborCost(Long id) {
        return laborCostMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveLaborCost(List<LaborCost> laborCosts) {
        if (laborCosts == null || laborCosts.isEmpty()) {
            return true;
        }
        for (LaborCost laborCost : laborCosts) {
            saveOrUpdateLaborCost(laborCost);
        }
        return true;
    }

    // ==================== 其他成本管理 ====================

    @Override
    public List<OtherCost> getOtherCostList(Integer costType, LocalDateTime startDate, LocalDateTime endDate, Long tenantId) {
        LambdaQueryWrapper<OtherCost> qw = new LambdaQueryWrapper<>();
        if (costType != null) {
            qw.eq(OtherCost::getCostType, costType);
        }
        if (startDate != null) {
            qw.ge(OtherCost::getCostDate, startDate);
        }
        if (endDate != null) {
            qw.le(OtherCost::getCostDate, endDate);
        }
        if (tenantId != null) {
            qw.eq(OtherCost::getTenantId, tenantId);
        }
        qw.orderByDesc(OtherCost::getCostDate);
        return otherCostMapper.selectList(qw);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateOtherCost(OtherCost otherCost) {
        if (otherCost.getId() == null) {
            otherCost.setCreateTime(LocalDateTime.now());
            otherCost.setUpdateTime(LocalDateTime.now());
            return otherCostMapper.insert(otherCost) > 0;
        } else {
            otherCost.setUpdateTime(LocalDateTime.now());
            return otherCostMapper.updateById(otherCost) > 0;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOtherCost(Long id) {
        return otherCostMapper.deleteById(id) > 0;
    }

    // ==================== 成本统计分析 ====================

    @Override
    public Map<String, Object> getCostSummary(LocalDate startDate, LocalDate endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 查询成本记录
        LambdaQueryWrapper<CostRecord> costQw = new LambdaQueryWrapper<>();
        costQw.ge(CostRecord::getCostDate, startDate.atStartOfDay());
        costQw.le(CostRecord::getCostDate, endDate.atTime(23, 59, 59));
        if (tenantId != null) {
            costQw.eq(CostRecord::getTenantId, tenantId);
        }
        List<CostRecord> costRecords = costRecordMapper.selectList(costQw);

        // 2. 按类型汇总
        BigDecimal materialCostTotal = BigDecimal.ZERO;
        BigDecimal laborCostTotal = BigDecimal.ZERO;
        BigDecimal otherCostTotal = BigDecimal.ZERO;

        for (CostRecord record : costRecords) {
            if (record.getCostType() == 1) {
                materialCostTotal = materialCostTotal.add(record.getAmount());
            } else if (record.getCostType() == 2) {
                laborCostTotal = laborCostTotal.add(record.getAmount());
            } else if (record.getCostType() == 3) {
                otherCostTotal = otherCostTotal.add(record.getAmount());
            }
        }

        // 3. 查询人工成本
        LambdaQueryWrapper<LaborCost> laborQw = new LambdaQueryWrapper<>();
        laborQw.ge(LaborCost::getCostMonth, startDate);
        laborQw.le(LaborCost::getCostMonth, endDate);
        if (tenantId != null) {
            laborQw.eq(LaborCost::getTenantId, tenantId);
        }
        List<LaborCost> laborCosts = laborCostMapper.selectList(laborQw);
        for (LaborCost laborCost : laborCosts) {
            laborCostTotal = laborCostTotal.add(laborCost.getTotalCost() != null ? laborCost.getTotalCost() : BigDecimal.ZERO);
        }

        // 4. 查询其他成本
        LambdaQueryWrapper<OtherCost> otherQw = new LambdaQueryWrapper<>();
        otherQw.ge(OtherCost::getCostDate, startDate.atStartOfDay());
        otherQw.le(OtherCost::getCostDate, endDate.atTime(23, 59, 59));
        if (tenantId != null) {
            otherQw.eq(OtherCost::getTenantId, tenantId);
        }
        List<OtherCost> otherCosts = otherCostMapper.selectList(otherQw);
        for (OtherCost otherCost : otherCosts) {
            otherCostTotal = otherCostTotal.add(otherCost.getAmount() != null ? otherCost.getAmount() : BigDecimal.ZERO);
        }

        // 5. 计算总成本
        BigDecimal totalCost = materialCostTotal.add(laborCostTotal).add(otherCostTotal);

        result.put("materialCost", materialCostTotal);
        result.put("laborCost", laborCostTotal);
        result.put("otherCost", otherCostTotal);
        result.put("totalCost", totalCost);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        return result;
    }

    @Override
    public Map<String, Object> getCostTrend(LocalDate startDate, LocalDate endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();
        List<String> dates = new ArrayList<>();
        List<BigDecimal> materialCosts = new ArrayList<>();
        List<BigDecimal> laborCosts = new ArrayList<>();
        List<BigDecimal> otherCosts = new ArrayList<>();
        List<BigDecimal> totalCosts = new ArrayList<>();

        // 按天统计
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            dates.add(current.toString());

            // 查询当天成本记录
            LambdaQueryWrapper<CostRecord> costQw = new LambdaQueryWrapper<>();
            costQw.ge(CostRecord::getCostDate, current.atStartOfDay());
            costQw.le(CostRecord::getCostDate, current.atTime(23, 59, 59));
            if (tenantId != null) {
                costQw.eq(CostRecord::getTenantId, tenantId);
            }
            List<CostRecord> costRecords = costRecordMapper.selectList(costQw);

            BigDecimal dayMaterialCost = BigDecimal.ZERO;
            BigDecimal dayLaborCost = BigDecimal.ZERO;
            BigDecimal dayOtherCost = BigDecimal.ZERO;

            for (CostRecord record : costRecords) {
                if (record.getCostType() == 1) {
                    dayMaterialCost = dayMaterialCost.add(record.getAmount());
                } else if (record.getCostType() == 2) {
                    dayLaborCost = dayLaborCost.add(record.getAmount());
                } else if (record.getCostType() == 3) {
                    dayOtherCost = dayOtherCost.add(record.getAmount());
                }
            }

            materialCosts.add(dayMaterialCost);
            laborCosts.add(dayLaborCost);
            otherCosts.add(dayOtherCost);
            totalCosts.add(dayMaterialCost.add(dayLaborCost).add(dayOtherCost));

            current = current.plusDays(1);
        }

        result.put("dates", dates);
        result.put("materialCosts", materialCosts);
        result.put("laborCosts", laborCosts);
        result.put("otherCosts", otherCosts);
        result.put("totalCosts", totalCosts);

        return result;
    }

    @Override
    public Map<String, Object> getCostStructure(LocalDate startDate, LocalDate endDate, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 获取成本汇总
        Map<String, Object> summary = getCostSummary(startDate, endDate, tenantId);
        BigDecimal materialCost = (BigDecimal) summary.get("materialCost");
        BigDecimal laborCost = (BigDecimal) summary.get("laborCost");
        BigDecimal otherCost = (BigDecimal) summary.get("otherCost");
        BigDecimal totalCost = (BigDecimal) summary.get("totalCost");

        // 计算占比
        List<Map<String, Object>> structure = new ArrayList<>();
        if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> materialItem = new HashMap<>();
            materialItem.put("name", "食材成本");
            materialItem.put("value", materialCost);
            materialItem.put("rate", materialCost.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")));
            structure.add(materialItem);

            Map<String, Object> laborItem = new HashMap<>();
            laborItem.put("name", "人工成本");
            laborItem.put("value", laborCost);
            laborItem.put("rate", laborCost.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")));
            structure.add(laborItem);

            Map<String, Object> otherItem = new HashMap<>();
            otherItem.put("name", "其他成本");
            otherItem.put("value", otherCost);
            otherItem.put("rate", otherCost.divide(totalCost, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100")));
            structure.add(otherItem);
        }

        result.put("structure", structure);
        result.put("totalCost", totalCost);

        return result;
    }

    @Override
    public List<Map<String, Object>> getDishCostRanking(int limit, Long tenantId) {
        LambdaQueryWrapper<DishCost> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(DishCost::getTenantId, tenantId);
        }
        qw.orderByDesc(DishCost::getTotalCost);
        qw.last("LIMIT " + limit);
        List<DishCost> dishCosts = dishCostMapper.selectList(qw);

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (DishCost dishCost : dishCosts) {
            Map<String, Object> item = new HashMap<>();
            item.put("dishId", dishCost.getDishId());
            item.put("dishName", dishCost.getDishName());
            item.put("materialCost", dishCost.getMaterialCost());
            item.put("laborCost", dishCost.getLaborCost());
            item.put("otherCost", dishCost.getOtherCost());
            item.put("totalCost", dishCost.getTotalCost());
            item.put("salePrice", dishCost.getSalePrice());
            item.put("profitRate", dishCost.getProfitRate());
            ranking.add(item);
        }

        return ranking;
    }

    @Override
    public BigDecimal calculateProfitRate(Long dishId, Long tenantId) {
        DishCost dishCost = getDishCostByDishId(dishId, tenantId);
        if (dishCost == null || dishCost.getSalePrice() == null || dishCost.getSalePrice().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal profit = dishCost.getSalePrice().subtract(dishCost.getTotalCost() != null ? dishCost.getTotalCost() : BigDecimal.ZERO);
        return profit.divide(dishCost.getSalePrice(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }

    @Override
    public List<Map<String, Object>> getCostAlert(BigDecimal threshold, Long tenantId) {
        LambdaQueryWrapper<DishCost> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(DishCost::getTenantId, tenantId);
        }
        qw.lt(DishCost::getProfitRate, threshold);
        qw.orderByAsc(DishCost::getProfitRate);
        List<DishCost> dishCosts = dishCostMapper.selectList(qw);

        List<Map<String, Object>> alerts = new ArrayList<>();
        for (DishCost dishCost : dishCosts) {
            Map<String, Object> item = new HashMap<>();
            item.put("dishId", dishCost.getDishId());
            item.put("dishName", dishCost.getDishName());
            item.put("totalCost", dishCost.getTotalCost());
            item.put("salePrice", dishCost.getSalePrice());
            item.put("profitRate", dishCost.getProfitRate());
            item.put("alertLevel", getAlertLevel(dishCost.getProfitRate()));
            alerts.add(item);
        }

        return alerts;
    }

    /**
     * 获取预警级别
     */
    private String getAlertLevel(BigDecimal profitRate) {
        if (profitRate == null) {
            return "未知";
        }
        if (profitRate.compareTo(new BigDecimal("0")) < 0) {
            return "严重";
        } else if (profitRate.compareTo(new BigDecimal("10")) < 0) {
            return "警告";
        } else if (profitRate.compareTo(new BigDecimal("20")) < 0) {
            return "注意";
        } else {
            return "正常";
        }
    }
}

