package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.entity.Region;
import com.reggie.service.RegionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 行政区划管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
@RestController
@RequestMapping("/region")
public class RegionController {

    @Autowired
    private RegionService regionService;

    /**
     * 获取完整的省市区树形结构
     * 前端级联选择器使用，也用于后台管理树形展示
     */
    @GetMapping("/tree")
    public R<List<Region>> tree() {
        List<Region> tree = regionService.getRegionTree();
        return R.success(tree);
    }

    /**
     * 根据父级ID查询子级列表
     * @param parentId 父级ID，0或不传则返回所有省份
     */
    @GetMapping("/children")
    public R<List<Region>> children(@RequestParam(defaultValue = "0") Long parentId) {
        List<Region> list = regionService.getChildren(parentId);
        return R.success(list);
    }

    /**
     * 分页查询（后台管理使用）
     */
    @GetMapping("/page")
    public R<Page<Region>> page(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int pageSize,
                                 @RequestParam(required = false) String name,
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
    public R<Region> getById(@PathVariable Long id) {
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
    public R<String> save(@RequestBody Region region) {
        log.info("新增地区：{}", region.getName());
        regionService.save(region);
        return R.success("新增成功");
    }

    /**
     * 修改地区
     */
    @PutMapping
    public R<String> update(@RequestBody Region region) {
        log.info("修改地区：{}", region.getName());
        regionService.updateById(region);
        return R.success("修改成功");
    }

    /**
     * 删除地区
     * 如果有子节点，不允许删除
     */
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
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
}
