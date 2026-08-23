package com.reggie.module.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.R;
import com.reggie.module.inventory.dto.DishMaterialBatchDTO;
import com.reggie.module.inventory.dto.DishMaterialSaveDTO;
import com.reggie.module.inventory.model.DishMaterial;
import com.reggie.module.inventory.service.DishMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品食材关联（BOM）Controller
 *
 * @author reggie
 * @since 2026-08-22
 */
@Slf4j
@RestController
@RequestMapping("/api/inventory/dish-material")
@Tag(name = "菜品食材关联", description = "菜品食材关联（BOM）")
@RequireEmployee
public class DishMaterialController {

    @Autowired
    private DishMaterialService dishMaterialService;

    @GetMapping("/listByDish/{dishId}")
    @Operation(summary = "查询某菜品全部食材配方")
    public R<List<DishMaterial>> listByDish(@PathVariable Long dishId) {
        List<DishMaterial> list = dishMaterialService.listByDishId(dishId);
        return R.success(list);
    }

    @PostMapping
    @Operation(summary = "保存食材关联")
    public R<String> save(@Validated @RequestBody DishMaterialSaveDTO dto) {
        dishMaterialService.saveMaterial(dto);
        return R.success("保存成功");
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新食材关联")
    public R<String> update(@PathVariable Long id, @Validated @RequestBody DishMaterialSaveDTO dto) {
        dishMaterialService.updateMaterial(id, dto);
        return R.success("更新成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除食材关联")
    public R<String> delete(@PathVariable Long id) {
        dishMaterialService.deleteMaterial(id);
        return R.success("删除成功");
    }

    @PostMapping("/batchSave")
    @Operation(summary = "批量保存某菜品的食材配方（先删后插）")
    public R<String> batchSave(@Validated @RequestBody DishMaterialBatchDTO batchDTO) {
        dishMaterialService.batchSave(batchDTO);
        return R.success("保存成功");
    }
}
