package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.entity.Dish;
import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
import com.reggie.service.DishService;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 套餐菜品关联管理
 *
 * @author reggie
 * @since 2026-07-14
 */
@RestController
@RequestMapping("/setmeal_dish")
@Slf4j
@Tag(name = "套餐菜品关联管理", description = "套餐与菜品关联关系的独立CRUD接口")
public class SetmealDishController {

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private DishService dishService;

    /**
     * 新增套餐菜品关联
     * @param setmealDish 关联信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增套餐菜品关联", description = "向套餐中添加一个菜品关联")
    @Parameter(name = "setmealDish", description = "关联信息（套餐ID、菜品ID、份数、排序等）", required = true)
    public R<SetmealDish> save(@RequestBody SetmealDish setmealDish) {
        log.info("新增套餐菜品关联：setmealId={}, dishId={}, copies={}",
            setmealDish.getSetmealId(), setmealDish.getDishId(), setmealDish.getCopies());
        // 租户校验：确保套餐和菜品都属于当前租户
        if (!validateSetmealTenant(setmealDish.getSetmealId())) {
            return R.error("套餐不存在");
        }
        if (!validateDishTenant(setmealDish.getDishId())) {
            return R.error("菜品不存在");
        }
        setmealDishService.save(setmealDish);
        return R.success(setmealDish);
    }

    /**
     * 批量新增套餐菜品关联
     * @param setmealDishList 关联列表
     * @return 操作结果
     */
    @PostMapping("/batch")
    @Operation(summary = "批量新增套餐菜品关联", description = "向套餐中添加多个菜品关联")
    @Parameter(name = "setmealDishList", description = "关联信息列表", required = true)
    public R<String> saveBatch(@RequestBody List<SetmealDish> setmealDishList) {
        log.info("批量新增套餐菜品关联：count={}", setmealDishList.size());
        if (setmealDishList.isEmpty()) {
            return R.success("批量添加成功");
        }
        // 校验套餐归属
        Long setmealId = setmealDishList.get(0).getSetmealId();
        if (!validateSetmealTenant(setmealId)) {
            return R.error("套餐不存在");
        }
        setmealDishService.saveBatch(setmealDishList);
        return R.success("批量添加成功");
    }

    /**
     * 根据ID查询关联详情
     * @param id 关联ID
     * @return 关联详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询关联详情", description = "根据ID查询单个套餐菜品关联信息")
    @Parameter(name = "id", description = "关联ID", required = true)
    public R<SetmealDish> get(@PathVariable Long id) {
        SetmealDish setmealDish = setmealDishService.getById(id);
        if (setmealDish == null) {
            return R.error("关联不存在");
        }
        // 租户校验：验证套餐归属
        if (!validateSetmealTenant(setmealDish.getSetmealId())) {
            return R.error("关联不存在");
        }
        return R.success(setmealDish);
    }

    /**
     * 修改套餐菜品关联
     * @param setmealDish 关联信息（需包含ID）
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改套餐菜品关联", description = "根据ID更新关联信息（份数、排序等）")
    @Parameter(name = "setmealDish", description = "关联信息（必须包含ID）", required = true)
    public R<String> update(@RequestBody SetmealDish setmealDish) {
        log.info("修改套餐菜品关联：id={}, copies={}, sort={}",
            setmealDish.getId(), setmealDish.getCopies(), setmealDish.getSort());
        // 租户校验
        SetmealDish existing = setmealDishService.getById(setmealDish.getId());
        if (existing == null) {
            return R.error("关联不存在");
        }
        if (!validateSetmealTenant(existing.getSetmealId())) {
            return R.error("关联不存在");
        }
        setmealDishService.updateById(setmealDish);
        return R.success("修改关联成功");
    }

    /**
     * 删除套餐菜品关联
     * @param id 关联ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除套餐菜品关联", description = "根据ID删除单个关联记录")
    @Parameter(name = "id", description = "关联ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        SetmealDish existing = setmealDishService.getById(id);
        if (existing != null && !validateSetmealTenant(existing.getSetmealId())) {
            return R.error("关联不存在");
        }
        setmealDishService.removeById(id);
        return R.success("删除关联成功");
    }

    /**
     * 批量删除套餐菜品关联
     * @param ids 关联ID列表（逗号分隔）
     * @return 操作结果
     */
    @DeleteMapping
    @Operation(summary = "批量删除套餐菜品关联", description = "根据ID列表批量删除关联记录")
    @Parameter(name = "ids", description = "关联ID列表，逗号分隔（如 1,2,3）", required = true)
    public R<String> deleteBatch(@RequestParam String ids) {
        String[] idArr = ids.split(",");
        List<Long> idList = new ArrayList<>();
        for (String idStr : idArr) {
            idStr = idStr.trim();
            if (!idStr.isEmpty()) {
                idList.add(Long.parseLong(idStr));
            }
        }
        // 批量校验租户归属
        if (!idList.isEmpty()) {
            Long currentTenantId = BaseContext.getCurrentTenantId();
            List<SetmealDish> records = setmealDishService.listByIds(idList);
            List<Long> unauthorizedIds = records.stream()
                .filter(r -> !currentTenantId.equals(getSetmealTenantId(r.getSetmealId())))
                .map(SetmealDish::getId)
                .collect(Collectors.toList());
            if (!unauthorizedIds.isEmpty()) {
                return R.error("以下关联不属于当前租户，无法删除：ID=" + unauthorizedIds);
            }
        }
        // 批量删除（修复之前的循环删除问题）
        if (!idList.isEmpty()) {
            setmealDishService.removeByIds(idList);
        }
        return R.success("删除成功");
    }

    /**
     * 查询套餐下的所有菜品关联
     * @param setmealId 套餐ID
     * @return 关联列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询套餐菜品关联列表", description = "根据套餐ID查询该套餐包含的所有菜品关联，按排序升序")
    @Parameter(name = "setmealId", description = "套餐ID", required = true)
    public R<List<SetmealDish>> list(@RequestParam Long setmealId) {
        // 租户校验
        if (!validateSetmealTenant(setmealId)) {
            return R.error("套餐不存在");
        }
        List<SetmealDish> list = setmealDishService.listBySetmealId(setmealId);
        return R.success(list);
    }

    /**
     * 验证套餐是否属于当前租户
     * @param setmealId 套餐ID
     * @return true=属于当前租户，false=不属于
     */
    private boolean validateSetmealTenant(Long setmealId) {
        if (setmealId == null) return false;
        Setmeal setmeal = setmealService.getById(setmealId);
        if (setmeal == null) return false;
        Long currentTenantId = BaseContext.getCurrentTenantId();
        return currentTenantId != null && currentTenantId.equals(setmeal.getTenantId());
    }

    /**
     * 获取套餐的租户ID（用于批量校验）
     */
    private Long getSetmealTenantId(Long setmealId) {
        Setmeal setmeal = setmealService.getById(setmealId);
        return setmeal != null ? setmeal.getTenantId() : null;
    }

    /**
     * 验证菜品是否属于当前租户
     * @param dishId 菜品ID
     * @return true=属于当前租户，false=不属于
     */
    private boolean validateDishTenant(Long dishId) {
        if (dishId == null) return false;
        Dish dish = dishService.getById(dishId);
        if (dish == null) return false;
        Long currentTenantId = BaseContext.getCurrentTenantId();
        return currentTenantId != null && currentTenantId.equals(dish.getTenantId());
    }
}
