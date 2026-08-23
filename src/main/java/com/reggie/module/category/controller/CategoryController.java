package com.reggie.module.category.controller;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.module.category.model.Category;
import com.reggie.module.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 分类管理
 * <p>修改点（7月14日）：移除冗余的手动租户过滤(MP拦截器自动处理)；save清除id防注入；新增统计接口</p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/category")
@Slf4j
@Tag(name = "分类管理", description = "菜品及套餐分类接口")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增分类
     * <p>修改点：清除前端传入的id，防止注入；名称trim处理</p>
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @PostMapping
    @Operation(summary = "新增分类", description = "创建新的菜品或套餐分类，排序号不填则自动分配")
    public R<String> save(@Valid @RequestBody Category category) {
        // 修改点：清除前端可能传入的id，由数据库自增
        category.setId(null);
        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }
        log.info("新增分类：name={}, type={}, sort={}", category.getName(), category.getType(), category.getSort());
        categoryService.save(category);
        return R.success("新增分类成功");
    }

    /**
     * 分页查询
     * <p>修改点：移除冗余手动租户过滤，MyBatis-Plus TenantLineInnerInterceptor 已自动处理</p>
     */
    @RequireEmployee
        @GetMapping("/page")
    @Operation(summary = "分类分页查询", description = "分页查询分类列表，支持按类型、名称筛选，按排序字段升序排列")
    @Parameter(description = "Page")
    public R<Page<Category>> page(@RequestParam(defaultValue = "1") @Min(1) int page, @RequestParam(defaultValue = "10") @Min(1) @Max(100) int pageSize,
                                  @Parameter(description = "Type")
                                  @RequestParam(required = false) String type,
                                  @Parameter(description = "Name")
                                  @RequestParam(required = false) String name) {
        Page<Category> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(type != null && !type.isEmpty(), Category::getType, type);
        queryWrapper.like(name != null && !name.isEmpty(), Category::getName, name);
        queryWrapper.orderByAsc(Category::getSort);

        categoryService.page(pageInfo, queryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 根据id删除分类
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @DeleteMapping("/{id}")
    @Operation(summary = "删除分类", description = "根据ID删除分类，删除前校验是否关联了菜品或套餐")
    public R<String> delete(@PathVariable Long id) {
        log.info("删除分类，id={}", id);
        categoryService.remove(id);
        return R.success("分类删除成功");
    }

    /**
     * 批量删除分类
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @DeleteMapping
    @Operation(summary = "批量删除分类", description = "根据逗号分隔的ID批量删除分类，删除前逐条校验关联性")
    public R<String> deleteBatch(@RequestParam String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return R.error("请选择要删除的分类");
        }
        String[] split = ids.split(",");
        List<Long> idList = new ArrayList<>();
        for (String s : split) {
            String trim = s.trim();
            if (!trim.isEmpty()) {
                idList.add(Long.parseLong(trim));
            }
        }
        if (idList.isEmpty()) {
            return R.error("请选择要删除的分类");
        }
        log.info("批量删除分类，ids={}", idList);
        categoryService.remove(idList);
        return R.success("分类删除成功");
    }

    /**
     * 修改分类信息
     * <p>修改点：Service层已校验存在性、禁止改type、名称唯一性、排序冲突处理</p>
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @PutMapping
    @Operation(summary = "修改分类", description = "根据ID更新分类名称或排序，不允许修改分类类型")
    public R<String> update(@Valid @RequestBody Category category) {
        log.info("修改分类：id={}, name={}, sort={}", category.getId(), category.getName(), category.getSort());
        // 安全：使用白名单字段更新，防止Mass Assignment攻击
        LambdaUpdateWrapper<Category> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Category::getId, category.getId());
        if (category.getName() != null) {
            updateWrapper.set(Category::getName, category.getName().trim());
        }
        if (category.getSort() != null) {
            updateWrapper.set(Category::getSort, category.getSort());
        }
        categoryService.update(updateWrapper);
        return R.success("分类修改成功");
    }

    /**
     * 查询分类详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询分类详情", description = "根据ID查询分类信息")
    public R<Category> get(@PathVariable Long id) {
        Category category = categoryService.getById(id);
        if (category == null) {
            return R.error("分类不存在");
        }
        return R.success(category);
    }

    /**
     * 根据条件查询分类列表
     * <p>修改点：移除冗余手动租户过滤；增加id同名安全判断</p>
     */
    @GetMapping("/list")
    @Operation(summary = "查询分类列表", description = "按类型筛选，按排序升序+更新时间降序排列")
    public R<List<Category>> list(Category category) {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        if (category != null) {
            if (category.getType() != null) {
                queryWrapper.eq(Category::getType, category.getType());
            }
            if (category.getName() != null && !category.getName().trim().isEmpty()) {
                queryWrapper.like(Category::getName, category.getName().trim());
            }
        }
        queryWrapper.orderByAsc(Category::getSort).orderByDesc(Category::getUpdateTime);

        List<Category> list = categoryService.list(queryWrapper);
        return R.success(list);
    }

    /**
     * 获取筛选下拉选项
     * <p>修改点：移除冗余手动租户过滤</p>
     */
    @GetMapping("/options")
    @Operation(summary = "筛选选项", description = "获取所有分类名称供下拉框使用")
    public R<Map<String, List<String>>> options() {
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(Category::getSort);
        List<Category> list = categoryService.list(queryWrapper);

        Set<String> nameSet = new HashSet<>();
        for (Category cat : list) {
            if (cat.getName() != null && !cat.getName().isEmpty()) {
                nameSet.add(cat.getName());
            }
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("names", new ArrayList<>(nameSet));
        return R.success(result);
    }

    /**
     * 获取分类统计数据（新增）
     * <p>修改点：替代前端分页后手动统计，直接从DB查询准确数据</p>
     *
     * @return 包含 totalCategories / foodCategories / comboCategories / todayNew 的 Map
     */
    @RequireEmployee
        @GetMapping("/stats")
    @Operation(summary = "分类统计", description = "获取分类总数、菜品/套餐分类数、今日新增数")
    public R<Map<String, Object>> stats() {
        // 总分类数
        long totalCategories = categoryService.count();

        // 菜品分类数
        long foodCategories = categoryService.count(
                new LambdaQueryWrapper<Category>().eq(Category::getType, 1));

        // 套餐分类数
        long comboCategories = categoryService.count(
                new LambdaQueryWrapper<Category>().eq(Category::getType, 2));

        // 今日新增（按createTime当天范围）
        java.time.LocalDate today = java.time.LocalDate.now();
        long todayNew = categoryService.count(
                new LambdaQueryWrapper<Category>()
                        .ge(Category::getCreateTime, today.atStartOfDay())
                        .le(Category::getCreateTime, today.plusDays(1).atStartOfDay()));

        Map<String, Object> result = new HashMap<>();
        result.put("totalCategories", totalCategories);
        result.put("foodCategories", foodCategories);
        result.put("comboCategories", comboCategories);
        result.put("todayNew", todayNew);
        return R.success(result);
    }
}




