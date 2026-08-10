package com.reggie.module.store.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.entity.*;
import com.reggie.module.store.mapper.*;
import com.reggie.module.store.model.*;
import com.reggie.module.store.service.StoreSyncService;
import com.reggie.service.*;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 门店数据同步服务实现
 * 处理总部向分店的一键同步功能
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
/**
 * StoreSync service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class StoreSyncServiceImpl implements StoreSyncService {

    /** 同步日志Mapper */
    @Autowired
    private StoreSyncLogMapper syncLogMapper;
    /** 门店信息Mapper */
    @Autowired
    private StoreInfoMapper storeInfoMapper;

    /** 菜品服务 */
    @Autowired
    private DishService dishService;
    /** 分类服务 */
    @Autowired
    private CategoryService categoryService;
    /** 套餐服务 */
    @Autowired
    private SetmealService setmealService;
    /** 菜品口味服务 */
    @Autowired
    private DishFlavorService dishFlavorService;
    /** 套餐菜品关联服务 */
    @Autowired
    private SetmealDishService setmealDishService;

    @Override
    public Map<String, Object> syncDishes(Long sourceTenantId, Long targetTenantId,
                                           List<Long> dishIds, Long operatorId) {
        Map<String, Object> result = new LinkedHashMap<>();
        StoreSyncLog syncLog = createSyncLog(sourceTenantId, targetTenantId,
                StoreSyncLog.SYNC_TYPE_DISH, operatorId);

        int synced = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try {
            // 获取源门店菜品
            LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Dish::getTenantId, sourceTenantId)
                   .eq(Dish::getIsDeleted, 0);
            if (dishIds != null && !dishIds.isEmpty()) {
                wrapper.in(Dish::getId, dishIds); // 选择性同步
            }
            List<Dish> sourceDishes = dishService.list(wrapper);

            for (Dish dish : sourceDishes) {
                try {
                    // 复制菜品到目标门店
                    Dish newDish = new Dish();
                    newDish.setName(dish.getName());
                    newDish.setCategoryId(dish.getCategoryId());
                    newDish.setPrice(dish.getPrice());
                    newDish.setCode(dish.getCode());
                    newDish.setImage(dish.getImage());
                    newDish.setDescription(dish.getDescription());
                    newDish.setStatus(dish.getStatus());
                    newDish.setSort(dish.getSort());
                    newDish.setTenantId(targetTenantId);
                    dishService.save(newDish);

                    // 同步菜品口味
                    LambdaQueryWrapper<DishFlavor> flavorWrapper = new LambdaQueryWrapper<>();
                    flavorWrapper.eq(DishFlavor::getDishId, dish.getId());
                    List<DishFlavor> flavors = dishFlavorService.list(flavorWrapper);
                    for (DishFlavor flavor : flavors) {
                        DishFlavor newFlavor = new DishFlavor();
                        newFlavor.setDishId(newDish.getId());
                        newFlavor.setName(flavor.getName());
                        newFlavor.setValue(flavor.getValue());
                        dishFlavorService.save(newFlavor);
                    }

                    synced++;
                } catch (Exception e) {
                    failed++;
                    errors.add("菜品[" + dish.getName() + "]: " + e.getMessage());
                }
            }

            result.put("synced", synced);
            result.put("failed", failed);
            result.put("errors", errors);

            updateSyncLog(syncLog, StoreSyncLog.STATUS_SUCCESS, synced, failed, errors);
        } catch (Exception e) {
            updateSyncLog(syncLog, StoreSyncLog.STATUS_FAILED, synced, failed,
                    Collections.singletonList(e.getMessage()));
            result.put("synced", 0);
            result.put("failed", 1);
            result.put("errors", Collections.singletonList(e.getMessage()));
        }

        return result;
    }

    @Override
    public Map<String, Object> syncCategories(Long sourceTenantId, Long targetTenantId, Long operatorId) {
        Map<String, Object> result = new LinkedHashMap<>();
        StoreSyncLog syncLog = createSyncLog(sourceTenantId, targetTenantId,
                StoreSyncLog.SYNC_TYPE_CATEGORY, operatorId);

        int synced = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try {
            LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Category::getTenantId, sourceTenantId)
                   .eq(Category::getIsDeleted, 0);
            List<Category> categories = categoryService.list(wrapper);

            for (Category cat : categories) {
                try {
                    Category newCat = new Category();
                    newCat.setType(cat.getType());
                    newCat.setName(cat.getName());
                    newCat.setSort(cat.getSort());
                    newCat.setTenantId(targetTenantId);
                    categoryService.save(newCat);
                    synced++;
                } catch (Exception e) {
                    failed++;
                    errors.add("分类[" + cat.getName() + "]: " + e.getMessage());
                }
            }

            result.put("synced", synced);
            result.put("failed", failed);
            updateSyncLog(syncLog, StoreSyncLog.STATUS_SUCCESS, synced, failed, errors);
        } catch (Exception e) {
            updateSyncLog(syncLog, StoreSyncLog.STATUS_FAILED, 0, 1,
                    Collections.singletonList(e.getMessage()));
        }

        return result;
    }

    @Override
    public Map<String, Object> syncSetmeals(Long sourceTenantId, Long targetTenantId,
                                             List<Long> setmealIds, Long operatorId) {
        Map<String, Object> result = new LinkedHashMap<>();
        StoreSyncLog syncLog = createSyncLog(sourceTenantId, targetTenantId,
                StoreSyncLog.SYNC_TYPE_SETMEAL, operatorId);

        int synced = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try {
            LambdaQueryWrapper<Setmeal> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Setmeal::getTenantId, sourceTenantId)
                   .eq(Setmeal::getIsDeleted, 0);
            if (setmealIds != null && !setmealIds.isEmpty()) {
                wrapper.in(Setmeal::getId, setmealIds);
            }
            List<Setmeal> setmeals = setmealService.list(wrapper);

            for (Setmeal sm : setmeals) {
                try {
                    Setmeal newSm = new Setmeal();
                    newSm.setCategoryId(sm.getCategoryId());
                    newSm.setName(sm.getName());
                    newSm.setPrice(sm.getPrice());
                    newSm.setStatus(sm.getStatus());
                    newSm.setCode(sm.getCode());
                    newSm.setDescription(sm.getDescription());
                    newSm.setImage(sm.getImage());
                    newSm.setTenantId(targetTenantId);
                    setmealService.save(newSm);

                    LambdaQueryWrapper<SetmealDish> sdWrapper = new LambdaQueryWrapper<>();
                    sdWrapper.eq(SetmealDish::getSetmealId, sm.getId());
                    List<SetmealDish> sdList = setmealDishService.list(sdWrapper);
                    for (SetmealDish sd : sdList) {
                        SetmealDish newSd = new SetmealDish();
                        newSd.setSetmealId(newSm.getId());
                        newSd.setDishId(sd.getDishId());
                        newSd.setName(sd.getName());
                        newSd.setPrice(sd.getPrice());
                        newSd.setCopies(sd.getCopies());
                        newSd.setSort(sd.getSort());
                        newSd.setTenantId(targetTenantId);
                        setmealDishService.save(newSd);
                    }

                    synced++;
                } catch (Exception e) {
                    failed++;
                    errors.add("套餐[" + sm.getName() + "]: " + e.getMessage());
                }
            }

            result.put("synced", synced);
            result.put("failed", failed);
            updateSyncLog(syncLog, StoreSyncLog.STATUS_SUCCESS, synced, failed, errors);
        } catch (Exception e) {
            updateSyncLog(syncLog, StoreSyncLog.STATUS_FAILED, 0, 1,
                    Collections.singletonList(e.getMessage()));
        }

        return result;
    }

    @Override
    public Map<String, Object> syncCoupons(Long sourceTenantId, Long targetTenantId, Long operatorId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("synced", 0);
        result.put("failed", 0);
        result.put("message", "优惠券同步功能开发中");

        StoreSyncLog syncLog = createSyncLog(sourceTenantId, targetTenantId,
                StoreSyncLog.SYNC_TYPE_COUPON, operatorId);
        updateSyncLog(syncLog, StoreSyncLog.STATUS_SUCCESS, 0, 0, Collections.emptyList());

        return result;
    }

    @Override
    public List<Map<String, Object>> getSyncLogs(Long sourceTenantId, int page, int pageSize) {
        Page<StoreSyncLog> pageObj = new Page<>(page, pageSize);
        LambdaQueryWrapper<StoreSyncLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreSyncLog::getSourceTenantId, sourceTenantId)
               .orderByDesc(StoreSyncLog::getStartTime);

        Page<StoreSyncLog> result = syncLogMapper.selectPage(pageObj, wrapper);
        return result.getRecords().stream().map(l -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", l.getId());
            map.put("sourceTenantId", l.getSourceTenantId());
            map.put("targetTenantId", l.getTargetTenantId());
            map.put("syncType", l.getSyncType());
            map.put("syncStatus", l.getSyncStatus());
            map.put("syncCount", l.getSyncCount());
            map.put("failCount", l.getFailCount());
            map.put("startTime", l.getStartTime());
            map.put("endTime", l.getEndTime());
            return map;
        }).collect(Collectors.toList());
    }

    private StoreSyncLog createSyncLog(Long sourceTenantId, Long targetTenantId,
                                        Integer syncType, Long operatorId) {
        StoreSyncLog syncLog = new StoreSyncLog();
        syncLog.setSourceTenantId(sourceTenantId);
        syncLog.setTargetTenantId(targetTenantId);
        syncLog.setSyncType(syncType);
        syncLog.setSyncMode(StoreSyncLog.SYNC_MODE_FULL);
        syncLog.setSyncStatus(StoreSyncLog.STATUS_IN_PROGRESS);
        syncLog.setOperatorId(operatorId);
        syncLog.setStartTime(LocalDateTime.now());
        syncLogMapper.insert(syncLog);
        return syncLog;
    }

    private void updateSyncLog(StoreSyncLog syncLog, Integer status, int syncCount,
                                int failCount, List<String> errors) {
        syncLog.setSyncStatus(status);
        syncLog.setSyncCount(syncCount);
        syncLog.setFailCount(failCount);
        syncLog.setEndTime(LocalDateTime.now());
        if (!errors.isEmpty()) {
            syncLog.setErrorDetail(StrUtil.join("; ", errors));
        }
        syncLogMapper.updateById(syncLog);
    }
}



