package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.entity.Region;
import com.reggie.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 行政区划管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/region")
@Tag(name = "行政区划管理", description = "省市区行政区划数据的CRUD接口")
public class RegionController {

    @Autowired
    private RegionService regionService;

    /**
     * 获取完整的省市区树形结构
     * 前端级联选择器使用，也用于后台管理树形展示
     */
    @GetMapping("/tree")
    @Operation(summary = "获取行政区划树形结构", description = "返回完整的省市区三级树形数据，用于前端级联选择器")
    public R<List<Region>> tree() {
        List<Region> tree = regionService.getRegionTree();
        return R.success(tree);
    }

    /**
     * 根据父级ID查询子级列表
     * @param parentId 父级ID，0或不传则返回所有省份
     */
    @GetMapping("/children")
    @Operation(summary = "查询子级行政区划", description = "根据父级ID查询下级行政区划列表")
    public R<List<Region>> children(
            @Parameter(name = "parentId", description = "父级ID，0或不传则返回所有省份", example = "0")
            @RequestParam(defaultValue = "0") Long parentId) {
        List<Region> list = regionService.getChildren(parentId);
        return R.success(list);
    }

    /**
     * 分页查询（后台管理使用）
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询行政区划", description = "后台管理分页查询行政区划数据")
    public R<Page<Region>> page(
            @Parameter(name = "page", description = "页码", required = true, example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(name = "name", description = "地区名称（模糊查询）")
            @RequestParam(required = false) String name,
            @Parameter(name = "level", description = "行政区划级别（1省 2市 3区）")
            @RequestParam(required = false) Integer level) {
        Page<Region> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null && !name.isEmpty(), Region::getName, name);
        wrapper.eq(level != null, Region::getLevel, level);
        wrapper.orderByAsc(Region::getLevel).orderByAsc(Region::getSort);
        regionService.page(pageInfo, wrapper);
        return R.success(pageInfo);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询行政区划详情", description = "根据ID查询行政区划信息")
    public R<Region> getById(
            @Parameter(name = "id", description = "行政区划ID", required = true)
            @PathVariable Long id) {
        Region region = regionService.getById(id);
        if (region != null) {
            return R.success(region);
        }
        return R.error("地区不存在");
    }

    /**
     * 新增地区
     */
    @PostMapping
    @Operation(summary = "新增行政区划", description = "新增省市区行政区划数据")
    public R<String> save(
            @Parameter(name = "region", description = "行政区划信息", required = true)
            @RequestBody Region region) {
        log.info("新增地区：{}", region.getName());
        regionService.save(region);
        return R.success("新增成功");
    }

    /**
     * 修改地区
     */
    @PutMapping
    @Operation(summary = "修改行政区划", description = "修改行政区划信息")
    public R<String> update(
            @Parameter(name = "region", description = "行政区划信息", required = true)
            @RequestBody Region region) {
        log.info("修改地区：{}", region.getName());
        regionService.updateById(region);
        return R.success("修改成功");
    }

    /**
     * 删除地区
     * 如果有子节点，不允许删除
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除行政区划", description = "删除行政区划，如果有子节点则不允许删除")
    public R<String> delete(
            @Parameter(name = "id", description = "行政区划ID", required = true)
            @PathVariable Long id) {
        // 检查是否有子节点
        LambdaQueryWrapper<Region> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Region::getParentId, id);
        long count = regionService.count(wrapper);
        if (count > 0) {
            return R.error("该地区下存在子级地区，请先删除子级");
        }
        regionService.removeById(id);
        return R.success("删除成功");
    }

    /**
     * 获取筛选下拉选项（地区名称列表）
     * <p>从数据库动态查询所有地区名称，供前端下拉框使用</p>
     */
    @GetMapping("/options")
    @Operation(summary = "筛选选项", description = "获取所有地区名称，供搜索条件下拉框使用")
    public R<Map<String, List<String>>> options() {
        LambdaQueryWrapper<Region> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(Region::getName);
        List<Region> list = regionService.list(qw);

        Set<String> nameSet = new HashSet<>();
        for (Region r : list) {
            if (r.getName() != null && !r.getName().isEmpty()) { nameSet.add(r.getName()); }
        }
        Map<String, List<String>> result = new HashMap<>();
        result.put("names", new ArrayList<>(nameSet));
        return R.success(result);
    }
}
