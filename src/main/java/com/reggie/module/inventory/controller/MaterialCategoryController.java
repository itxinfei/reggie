package com.reggie.module.inventory.controller;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.common.R;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.module.inventory.model.MaterialCategory;
import com.reggie.module.inventory.service.MaterialCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import java.util.List;

/**
 * 食材分类管理控制器
 * 提供食材分类的增删改查接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@RequireEmployee
@RestController
@RequestMapping("/api/inventory/material-category")
@Tag(name = "食材分类管理")
public class MaterialCategoryController {

    @Autowired
    private MaterialCategoryService materialCategoryService;

    /**
     * 分页查询食材分类列表
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 分类名称（可选，模糊查询）
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询", description = "分页查询食材分类列表，支持按名称搜索，按排序字段升序排列")
    @Parameter(name = "page", description = "页码", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "分类名称（可选，模糊查询）")
    public R<Page<MaterialCategory>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize,
                                          @RequestParam(required = false) String name) {
        Page<MaterialCategory> pageInfo = PageUtils.of(page, pageSize);
        LambdaQueryWrapper<MaterialCategory> qw = new LambdaQueryWrapper<>();
        // 修改点：添加按名称模糊搜索支持，修复前端 name 参数被后端静默丢弃的 Bug
        qw.like(name != null && !name.isEmpty(), MaterialCategory::getName, name);
        qw.orderByAsc(MaterialCategory::getSort);
        materialCategoryService.page(pageInfo, qw);
        return R.success(pageInfo);
    }

    /**
     * 新增食材分类
     * <p>租户安全：强制设置 tenantId。</p>
     */
    @PostMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "新增分类", description = "创建新的食材分类")
    public R<String> save(@RequestBody MaterialCategory category) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        category.setTenantId(tenantId);
        materialCategoryService.save(category);
        return R.success("新增分类成功");
    }

    /**
     * 修改食材分类
     * <p>租户安全：先校验归属，再更新业务字段。</p>
     */
    @PutMapping
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "修改分类", description = "更新食材分类信息")
    public R<String> update(@RequestBody MaterialCategory category) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        MaterialCategory exist = materialCategoryService.getById(category.getId());
        if (exist == null) {
            throw new CustomException("食材分类不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("食材分类不属于当前租户");
        }
        exist.setName(category.getName());
        exist.setSort(category.getSort());
        materialCategoryService.updateById(exist);
        return R.success("修改分类成功");
    }

    @DeleteMapping("/{id}")
    @RateLimit(maxRequestsPerSecond = 10)
    @Operation(summary = "删除分类", description = "根据ID删除食材分类（先校验租户归属）")
    @Parameter(name = "id", description = "分类ID", required = true)
    public R<String> delete(@PathVariable Long id) {
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId == null) {
            throw new CustomException("租户上下文不存在");
        }
        MaterialCategory exist = materialCategoryService.getById(id);
        if (exist == null) {
            throw new CustomException("食材分类不存在");
        }
        if (!tenantId.equals(exist.getTenantId())) {
            throw new CustomException("食材分类不属于当前租户");
        }
        materialCategoryService.removeById(id);
        return R.success("删除分类成功");
    }

    /**
     * 根据ID查询食材分类
     * @param id 分类ID
     * @return 分类详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据id查询", description = "根据ID查询食材分类详情")
    @Parameter(name = "id", description = "分类ID", required = true)
    public R<MaterialCategory> get(@PathVariable Long id) {
        MaterialCategory category = materialCategoryService.getById(id);
        if (category == null) {
            return R.error("分类不存在");
        }
        return R.success(category);
    }

    /**
     * 查询所有食材分类
     * @return 分类列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询所有", description = "查询所有食材分类列表，按排序字段升序排列")
    public R<List<MaterialCategory>> list() {
        LambdaQueryWrapper<MaterialCategory> qw = new LambdaQueryWrapper<>();
        qw.orderByAsc(MaterialCategory::getSort);
        return R.success(materialCategoryService.list(qw));
    }
}

