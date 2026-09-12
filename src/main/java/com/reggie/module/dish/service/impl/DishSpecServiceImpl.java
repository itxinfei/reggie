package com.reggie.module.dish.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.dish.model.DishSpecGroup;
import com.reggie.module.dish.model.DishSpecOption;
import com.reggie.module.dish.model.DishSpecRelation;
import com.reggie.module.dish.mapper.DishSpecGroupMapper;
import com.reggie.module.dish.mapper.DishSpecOptionMapper;
import com.reggie.module.dish.mapper.DishSpecRelationMapper;
import com.reggie.module.dish.service.DishSpecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 菜品规格服务实现
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class DishSpecServiceImpl extends ServiceImpl<DishSpecGroupMapper, DishSpecGroup> implements DishSpecService {

    @Autowired
    private DishSpecGroupMapper specGroupMapper;

    @Autowired
    private DishSpecOptionMapper specOptionMapper;

    @Autowired
    private DishSpecRelationMapper specRelationMapper;

    // ==================== 规格组管理 ====================

    /**
     * 获取 spec groups。
     * @param tenantId 参数 tenantId
     * @param status 参数 status
     * @return 返回结果
     */
    @Override
    public List<DishSpecGroup> getSpecGroups(Long tenantId, Integer status) {
        LambdaQueryWrapper<DishSpecGroup> qw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            qw.eq(DishSpecGroup::getTenantId, tenantId);
        }
        if (status != null) {
            qw.eq(DishSpecGroup::getStatus, status);
        }
        qw.orderByAsc(DishSpecGroup::getSortOrder);
        return specGroupMapper.selectList(qw);
    }

    /**
     * 获取 spec group by id。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    public DishSpecGroup getSpecGroupById(Long id) {
        return specGroupMapper.selectById(id);
    }

    /**
     * 保存 or update spec group。
     * @param group 参数 group
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateSpecGroup(DishSpecGroup group) {
        if (group.getId() == null) {
            group.setCreateTime(LocalDateTime.now());
            group.setUpdateTime(LocalDateTime.now());
            return specGroupMapper.insert(group) > 0;
        } else {
            group.setUpdateTime(LocalDateTime.now());
            return specGroupMapper.updateById(group) > 0;
        }
    }

    /**
     * 删除 spec group。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSpecGroup(Long id) {
        // 删除关联的选项
        LambdaQueryWrapper<DishSpecOption> optionQw = new LambdaQueryWrapper<>();
        optionQw.eq(DishSpecOption::getGroupId, id);
        specOptionMapper.delete(optionQw);

        // 删除关联关系
        LambdaQueryWrapper<DishSpecRelation> relationQw = new LambdaQueryWrapper<>();
        relationQw.eq(DishSpecRelation::getGroupId, id);
        specRelationMapper.delete(relationQw);

        return specGroupMapper.deleteById(id) > 0;
    }

    // ==================== 规格选项管理 ====================

    /**
     * 获取 spec options。
     * @param groupId 参数 groupId
     * @param tenantId 参数 tenantId
     * @param status 参数 status
     * @return 返回结果
     */
    @Override
    public List<DishSpecOption> getSpecOptions(Long groupId, Long tenantId, Integer status) {
        LambdaQueryWrapper<DishSpecOption> qw = new LambdaQueryWrapper<>();
        if (groupId != null) {
            qw.eq(DishSpecOption::getGroupId, groupId);
        }
        if (tenantId != null) {
            qw.eq(DishSpecOption::getTenantId, tenantId);
        }
        if (status != null) {
            qw.eq(DishSpecOption::getStatus, status);
        }
        qw.orderByAsc(DishSpecOption::getSortOrder);
        return specOptionMapper.selectList(qw);
    }

    /**
     * 保存 or update spec option。
     * @param option 参数 option
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateSpecOption(DishSpecOption option) {
        if (option.getId() == null) {
            option.setCreateTime(LocalDateTime.now());
            option.setUpdateTime(LocalDateTime.now());
            return specOptionMapper.insert(option) > 0;
        } else {
            option.setUpdateTime(LocalDateTime.now());
            return specOptionMapper.updateById(option) > 0;
        }
    }

    /**
     * 删除 spec option。
     * @param id 参数 id
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSpecOption(Long id) {
        return specOptionMapper.deleteById(id) > 0;
    }

    /**
     * 批量处理 save spec options。
     * @param options 参数 options
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchSaveSpecOptions(List<DishSpecOption> options) {
        if (options == null || options.isEmpty()) {
            return true;
        }
        for (DishSpecOption option : options) {
            saveOrUpdateSpecOption(option);
        }
        return true;
    }

    // ==================== 菜品规格关联 ====================

    /**
     * 获取 dish spec groups。
     * @param dishId 参数 dishId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public List<Map<String, Object>> getDishSpecGroups(Long dishId, Long tenantId) {
        // 1. 查询菜品关联的规格组
        LambdaQueryWrapper<DishSpecRelation> relationQw = new LambdaQueryWrapper<>();
        relationQw.eq(DishSpecRelation::getDishId, dishId);
        if (tenantId != null) {
            relationQw.eq(DishSpecRelation::getTenantId, tenantId);
        }
        relationQw.orderByAsc(DishSpecRelation::getSortOrder);
        List<DishSpecRelation> relations = specRelationMapper.selectList(relationQw);

        if (relations.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询规格组详情
        List<Long> groupIds = relations.stream()
                .map(DishSpecRelation::getGroupId)
                .collect(Collectors.toList());

        LambdaQueryWrapper<DishSpecGroup> groupQw = new LambdaQueryWrapper<>();
        groupQw.in(DishSpecGroup::getId, groupIds);
        groupQw.eq(DishSpecGroup::getStatus, 1);
        List<DishSpecGroup> groups = specGroupMapper.selectList(groupQw);

        // 3. 批量查询所有规格组的选项，避免 N+1 查询
        LambdaQueryWrapper<DishSpecOption> optionQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            optionQw.eq(DishSpecOption::getTenantId, tenantId);
        }
        optionQw.eq(DishSpecOption::getStatus, 1);
        optionQw.orderByAsc(DishSpecOption::getSortOrder);
        List<DishSpecOption> allOptions = specOptionMapper.selectList(optionQw);
        Map<Long, List<DishSpecOption>> optionsByGroup = allOptions.stream()
                .collect(Collectors.groupingBy(DishSpecOption::getGroupId));

        // 4. 组装结果
        List<Map<String, Object>> result = new ArrayList<>();
        for (DishSpecGroup group : groups) {
            Map<String, Object> groupMap = new HashMap<>();
            groupMap.put("id", group.getId());
            groupMap.put("name", group.getName());
            groupMap.put("type", group.getType());
            groupMap.put("required", group.getRequired());
            groupMap.put("maxSelect", group.getMaxSelect());

            List<DishSpecOption> options = optionsByGroup.get(group.getId());
            groupMap.put("options", options != null ? options : new ArrayList<>());

            result.add(groupMap);
        }

        return result;
    }

    /**
     * 设置 dish spec groups。
     * @param dishId 参数 dishId
     * @param groupIds 参数 groupIds
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setDishSpecGroups(Long dishId, List<Long> groupIds, Long tenantId) {
        // 1. 删除原有关联
        deleteDishSpecRelations(dishId);

        // 2. 保存新关联（批量构建后批量插入）
        if (groupIds != null && !groupIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            Long currentId = com.reggie.common.BaseContext.getCurrentId();
            List<DishSpecRelation> relations = new ArrayList<>();
            for (int i = 0; i < groupIds.size(); i++) {
                DishSpecRelation relation = new DishSpecRelation();
                relation.setDishId(dishId);
                relation.setGroupId(groupIds.get(i));
                relation.setSortOrder(i);
                relation.setTenantId(tenantId);
                relation.setCreateTime(now);
                relation.setCreateUser(currentId);
                relations.add(relation);
            }
            specRelationMapper.insertBatch(relations);
        }

        return true;
    }

    /**
     * 删除 dish spec relations。
     * @param dishId 参数 dishId
     * @return 返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDishSpecRelations(Long dishId) {
        LambdaQueryWrapper<DishSpecRelation> qw = new LambdaQueryWrapper<>();
        qw.eq(DishSpecRelation::getDishId, dishId);
        return specRelationMapper.delete(qw) >= 0;
    }

    // ==================== 规格价格计算 ====================

    /**
     * 计算 spec price。
     * @param dishId 参数 dishId
     * @param basePrice 参数 basePrice
     * @param optionIds 参数 optionIds
     * @return 返回结果
     */
    @Override
    public BigDecimal calculateSpecPrice(Long dishId, BigDecimal basePrice, List<Long> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            return basePrice;
        }

        BigDecimal totalPrice = basePrice;

        // 批量查询选项的价格调整，避免 N+1 查询
        List<DishSpecOption> options = specOptionMapper.selectBatchIds(optionIds);
        if (options == null || options.isEmpty()) {
            return basePrice;
        }

        for (DishSpecOption option : options) {
            if (option.getPriceAdjust() != null) {
                totalPrice = totalPrice.add(option.getPriceAdjust());
            }
        }

        // 确保价格不为负数
        if (totalPrice.compareTo(BigDecimal.ZERO) < 0) {
            totalPrice = BigDecimal.ZERO;
        }

        return totalPrice;
    }

    /**
     * 获取 dish spec detail。
     * @param dishId 参数 dishId
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getDishSpecDetail(Long dishId, Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        // 获取菜品关联的规格组
        List<Map<String, Object>> specGroups = getDishSpecGroups(dishId, tenantId);
        result.put("specGroups", specGroups);

        return result;
    }

    // ==================== 统计分析 ====================

    /**
     * 获取 spec statistics。
     * @param tenantId 参数 tenantId
     * @return 返回结果
     */
    @Override
    public Map<String, Object> getSpecStatistics(Long tenantId) {
        Map<String, Object> result = new HashMap<>();

        LambdaQueryWrapper<DishSpecGroup> groupQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            groupQw.eq(DishSpecGroup::getTenantId, tenantId);
        }
        int totalGroups = specGroupMapper.selectCount(groupQw).intValue();

        LambdaQueryWrapper<DishSpecOption> optionQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            optionQw.eq(DishSpecOption::getTenantId, tenantId);
        }
        int totalOptions = specOptionMapper.selectCount(optionQw).intValue();

        LambdaQueryWrapper<DishSpecRelation> relationQw = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            relationQw.eq(DishSpecRelation::getTenantId, tenantId);
        }
        int totalRelations = specRelationMapper.selectCount(relationQw).intValue();

        // 统计使用规格的菜品数量
        Set<Long> uniqueDishes = new HashSet<>();
        List<DishSpecRelation> relations = specRelationMapper.selectList(relationQw);
        for (DishSpecRelation relation : relations) {
            uniqueDishes.add(relation.getDishId());
        }

        result.put("totalGroups", totalGroups);
        result.put("totalOptions", totalOptions);
        result.put("totalRelations", totalRelations);
        result.put("dishesWithSpec", uniqueDishes.size());

        return result;
    }
}




