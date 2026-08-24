package com.reggie.module.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.platform.model.DishPlatformMapping;
import com.reggie.module.platform.service.DishPlatformMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 商品平台映射管理控制器
 *
 * @author reggie
 * @since 2026-08-24
 */
@Slf4j
@Tag(name = "商品平台映射管理")
@RestController
@RequestMapping("/admin/platform/mapping")
@RequiredArgsConstructor
public class DishPlatformMappingController {

    private final DishPlatformMappingService mappingService;

    @Operation(summary = "分页查询映射列表")
    @GetMapping("/page")
    public R<IPage<DishPlatformMapping>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long dishId,
            @RequestParam(required = false) String platformType) {
        Page<DishPlatformMapping> pageParam = new Page<>(page, pageSize);
        // LambdaQueryWrapper 不能在 Page 构造后直接用，通过 service 层封装
        IPage<DishPlatformMapping> result = mappingService.page(pageParam);
        return R.success(result);
    }

    @Operation(summary = "按菜品ID查询映射")
    @GetMapping("/dish/{dishId}")
    public R<List<DishPlatformMapping>> listByDishId(@PathVariable @NotNull Long dishId) {
        return R.success(mappingService.listByDishId(dishId));
    }

    @Operation(summary = "按平台类型查询映射")
    @GetMapping("/platform/{platformType}")
    public R<List<DishPlatformMapping>> listByPlatformType(@PathVariable String platformType) {
        return R.success(mappingService.listByPlatformType(platformType));
    }

    @Operation(summary = "新增映射")
    @PostMapping
    public R<DishPlatformMapping> add(@RequestBody DishPlatformMapping mapping) {
        mapping.setIsDeleted(0);
        mapping.setStatus(1);
        mappingService.save(mapping);
        return R.success(mapping);
    }

    @Operation(summary = "更新映射")
    @PutMapping
    public R<Boolean> update(@RequestBody DishPlatformMapping mapping) {
        return R.success(mappingService.updateById(mapping));
    }

    @Operation(summary = "删除映射")
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.success(mappingService.removeById(id));
    }
}
