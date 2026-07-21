package com.reggie.controller;

import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 菜品口味管理
 *
 * @author reggie
 * @since 2026-07-14
 */
@RestController
@RequestMapping("/dish_flavor")
@Slf4j
@Tag(name = "菜品口味管理", description = "菜品口味独立CRUD接口")
public class DishFlavorController {

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private DishService dishService;

    /**
     * 新增口味
     * @param dishFlavor 口味信息
     * @return 操作结果
     */
    @PostMapping
    @Operation(summary = "新增口味", description = "为菜品新增一个口味规格")
    @Parameter(name = "dishFlavor", description = "口味信息（名称、值、菜品ID）", required = true)
    public R<DishFlavor> save(@RequestBody DishFlavor dishFlavor) {
        log.info("新增口味：name={}, value={}, dishId={}",
            dishFlavor.getName(), dishFlavor.getValue(), dishFlavor.getDishId());
        // 租户校验：确保菜品属于当前租户
        if (!validateDishTenant(dishFlavor.getDishId())) {
            return R.error("菜品不存在");
        }
        dishFlavorService.save(dishFlavor);
        return R.success(dishFlavor);
    }

    /**
     * 根据ID查询口味详情
     * @param id 口味ID
     * @return 口味详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询口味详情", description = "根据ID查询单个口味信息")
    @Parameter(name = "id", description = "口味ID", required = true)
    public R<DishFlavor> get(@PathVariable Long id) {
        DishFlavor dishFlavor = dishFlavorService.getById(id);
        if (dishFlavor == null) {
            return R.error("口味不存在");
        }
        // 租户校验
        if (!validateDishTenant(dishFlavor.getDishId())) {
            return R.error("口味不存在");
        }
        return R.success(dishFlavor);
    }

    /**
     * 修改口味
     * @param dishFlavor 口味信息（需包含ID）
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改口味", description = "根据ID更新口味信息（名称、值）")
    @Parameter(name = "dishFlavor", description = "口味信息（必须包含ID）", required = true)
    public R<String> update(@RequestBody DishFlavor dishFlavor) {
        log.info("修改口味：id={}, name={}, value={}",
            dishFlavor.getId(), dishFlavor.getName(), dishFlavor.getValue());
        // 租户校验
        if (dishFlavor.getDishId() != null && !validateDishTenant(dishFlavor.getDishId())) {
            return R.error("口味不存在");
        }
        dishFlavorService.updateById(dishFlavor);
        return R.success("修改口味成功");
    }

    /**
     * 删除口味
     * @param id 口味ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除口味", description = "根据ID删除单个口味")
    @Parameter(name = "id", description = "口味ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        DishFlavor flavor = dishFlavorService.getById(id);
        if (flavor != null && !validateDishTenant(flavor.getDishId())) {
            return R.error("口味不存在");
        }
        dishFlavorService.removeById(id);
        return R.success("删除口味成功");
    }

    /**
     * 查询菜品下的所有口味
     * @param dishId 菜品ID
     * @return 口味列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询菜品口味列表", description = "根据菜品ID查询该菜品下所有口味，按ID升序排列")
    @Parameter(name = "dishId", description = "菜品ID", required = true)
    public R<List<DishFlavor>> list(@RequestParam Long dishId) {
        // 租户校验：确保菜品属于当前租户
        if (!validateDishTenant(dishId)) {
            return R.error("菜品不存在");
        }
        List<DishFlavor> list = dishFlavorService.listByDishId(dishId);
        return R.success(list);
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
