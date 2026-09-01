package com.reggie.module.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.common.utils.PageUtils;
import com.reggie.module.platform.model.DishPlatformMapping;
import com.reggie.module.platform.service.DishPlatformMappingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.util.*;

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
            @RequestParam(required = false) String platformType,
            @RequestParam(required = false) Integer status) {
        Page<DishPlatformMapping> pageParam = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<DishPlatformMapping> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DishPlatformMapping::getIsDeleted, 0);
        if (dishId != null) {
            wrapper.eq(DishPlatformMapping::getDishId, dishId);
        }
        if (StringUtils.hasText(platformType)) {
            wrapper.eq(DishPlatformMapping::getPlatformType, platformType);
        }
        if (status != null) {
            wrapper.eq(DishPlatformMapping::getStatus, status);
        }
        wrapper.orderByDesc(DishPlatformMapping::getUpdateTime);
        IPage<DishPlatformMapping> result = mappingService.page(pageParam, wrapper);
        return R.success(result);
    }

    @Operation(summary = "菜品平台映射统计（总数/已上架/已下架/覆盖平台数）")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        long total = mappingService.count();
        long onlineCount = mappingService.count(
                new LambdaQueryWrapper<DishPlatformMapping>().eq(DishPlatformMapping::getStatus, 1));
        long offlineCount = mappingService.count(
                new LambdaQueryWrapper<DishPlatformMapping>().eq(DishPlatformMapping::getStatus, 0));
        long platformCount = mappingService.list()
                .stream()
                .filter(m -> StringUtils.hasText(m.getPlatformType()))
                .map(DishPlatformMapping::getPlatformType)
                .distinct()
                .count();
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("onlineCount", onlineCount);
        result.put("offlineCount", offlineCount);
        result.put("platformCount", platformCount);
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
