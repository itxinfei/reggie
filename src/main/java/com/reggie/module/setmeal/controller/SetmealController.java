package com.reggie.module.setmeal.controller;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.SetmealDto;
import com.reggie.module.category.model.Category;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.setmeal.model.SetmealDish;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.setmeal.service.SetmealDishService;
import com.reggie.module.setmeal.service.SetmealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 套餐管理
 *
 * @author reggie
 * @since 2026-07-09
 */

@RestController
@RequestMapping("/setmeal")
@Slf4j
@Tag(name = "套餐管理", description = "套餐CRUD及菜品关联接口")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SetmealDishService setmealDishService;

    /**
     * 新增套餐
     * @param setmealDto 套餐信息（包含基本信息及菜品列表）
     * @return 操作结果
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @PostMapping
    @Operation(summary = "新增套餐", description = "创建新的套餐及关联菜品，支持多菜品组合")
    @Parameter(name = "setmealDto", description = "套餐信息DTO（名称、分类、价格、描述、状态、菜品列表）", required = true)
    public R<String> save(@Valid @RequestBody SetmealDto setmealDto){
        log.info("套餐信息：id={}, name={}, categoryId={}, price={}",
            setmealDto.getId(),
            setmealDto.getName(),
            setmealDto.getCategoryId(),
            setmealDto.getPrice());

        setmealService.saveWithDish(setmealDto);

        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 套餐名称（可选，模糊查询）
     * @return 分页结果
     */
    @RequireEmployee
        @GetMapping("/page")
    @Operation(summary = "套餐分页查询", description = "分页查询套餐列表，支持按名称模糊搜索和状态筛选，自动关联分类名称")
    @Parameter(name = "page", description = "页码，从1开始", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "套餐名称（可选，模糊查询）")
    @Parameter(name = "status", description = "售卖状态（可选，'0'=停售 ,'1'=启售）")
    @Parameter(name = "code", description = "套餐编码（可选，模糊查询）")
    public R<Page<SetmealDto>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String name, @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String code){
        //分页构造器对象
        Page<Setmeal> pageInfo = PageUtils.of(page,pageSize);
        Page<SetmealDto> dtoPage = PageUtils.of(page, pageSize);

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据name进行like模糊查询
        queryWrapper.like(name != null,Setmeal::getName,name);
        queryWrapper.eq(status != null && !status.isEmpty(), Setmeal::getStatus, status);
        queryWrapper.like(code != null && !code.isEmpty(), Setmeal::getCode, code);
        //添加排序条件，根据更新时间降序排列
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        // 多租户过滤：MyBatis-Plus TenantLineInnerInterceptor 已自动处理

        setmealService.page(pageInfo, queryWrapper);

        BeanUtils.copyProperties(pageInfo, dtoPage, "records");
        List<Setmeal> records = pageInfo.getRecords();
        if (records.isEmpty()) {
            return R.success(dtoPage);
        }

        //批量查询分类名称
        List<Long> categoryIds = records.stream().map(Setmeal::getCategoryId).collect(Collectors.toList());
        List<Category> categories = categoryService.listByIds(categoryIds);
        Map<Long, String> categoryMap = categories.stream()
            .collect(Collectors.toMap(Category::getId, Category::getName));

        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            BeanUtils.copyProperties(item, setmealDto);
            setmealDto.setCategoryName(categoryMap.get(item.getCategoryId()));
            return setmealDto;
        }).collect(Collectors.toList());

        dtoPage.setRecords(list);
        return R.success(dtoPage);
    }

    /**
     * 根据ID查询套餐详情
     *
     * @param id 套餐ID
     * @return 套餐详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询套餐详情", description = "根据ID查询套餐基本信息及关联菜品列表")
    @Parameter(name = "id", description = "套餐ID", required = true)
    public R<SetmealDto> get(@PathVariable Long id) {
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        // 租户校验：确保只能查询当前租户的套餐
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (setmealDto.getTenantId() != null && !setmealDto.getTenantId().equals(currentTenantId)) {
            return R.error("套餐不存在");
        }
        return R.success(setmealDto);
    }

    /**
     * 修改套餐信息
     *
     * @param setmealDto 套餐DTO（包含ID、基本信息及菜品列表）
     * @return 操作结果
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @PutMapping
    @Operation(summary = "修改套餐", description = "更新套餐基本信息及关联菜品")
    @Parameter(name = "setmealDto", description = "套餐DTO（包含ID、基本信息及菜品列表）", required = true)
    public R<String> update(@Valid @RequestBody SetmealDto setmealDto) {
        // 租户校验：确保只能修改当前租户的套餐
        Setmeal existing = setmealService.getById(setmealDto.getId());
        if (existing == null) {
            return R.error("套餐不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (!currentTenantId.equals(existing.getTenantId())) {
            return R.error("套餐不存在");
        }
        setmealService.updateWithDish(setmealDto);
        return R.success("修改套餐成功");
    }

    /**
     * 批量更新套餐状态
     *
     * @param status 状态值：1-起售，0-停售
     * @param ids 套餐ID列表，逗号分隔（如 1,2,3）
     * @return 操作结果
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @PostMapping("/status/{status}")
    @Operation(summary = "更新套餐状态", description = "批量更新套餐售卖状态（起售/停售）")
    @Parameter(name = "status", description = "状态值：1-起售，0-停售", required = true)
    @Parameter(name = "ids", description = "套餐ID列表，逗号分隔（如 1,2,3）", required = true)
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam String ids) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return R.error("请选择要操作的套餐");
        }
        // 租户校验
        List<Setmeal> setmeals = setmealService.listByIds(idList);
        Long currentTenantId = BaseContext.getCurrentTenantId();
        List<Long> unauthorizedIds = setmeals.stream()
            .filter(s -> !currentTenantId.equals(s.getTenantId()))
            .map(Setmeal::getId)
            .collect(Collectors.toList());
        if (!unauthorizedIds.isEmpty()) {
            return R.error("以下套餐不属于当前租户，无法操作：ID=" + unauthorizedIds);
        }
        setmealService.updateStatus(status, idList);
        return R.success("操作成功");
    }

    /**
     * 查询套餐包含的菜品列表
     *
     * @param id 套餐ID
     * @return 套餐菜品列表
     */
    @GetMapping("/dish/{id}")
    @Operation(summary = "查询套餐菜品", description = "查询套餐包含的菜品列表")
    @Parameter(name = "id", description = "套餐ID", required = true)
    public R<List<SetmealDish>> dish(@PathVariable Long id) {
        // 租户校验：先查套餐确保归属当前租户
        Setmeal setmeal = setmealService.getById(id);
        if (setmeal == null) {
            return R.error("套餐不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (!currentTenantId.equals(setmeal.getTenantId())) {
            return R.error("套餐不存在");
        }

        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        return R.success(list);
    }

    /**
     * 删除套餐
     * @param ids 套餐ID列表（逗号分隔字符串）
     * @return 操作结果
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @DeleteMapping
    @Operation(summary = "删除套餐", description = "批量删除套餐及关联菜品数据")
    @Parameter(name = "ids", description = "套餐ID列表，逗号分隔（如 1,2,3）", required = true)
    public R<String> delete(@RequestParam String ids){
        log.info("ids:{}", ids);
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return R.error("请选择要删除的套餐");
        }
        // 租户校验：确保只能删除当前租户的套餐
        List<Setmeal> setmeals = setmealService.listByIds(idList);
        Long currentTenantId = BaseContext.getCurrentTenantId();
        List<Long> unauthorizedIds = setmeals.stream()
            .filter(s -> !currentTenantId.equals(s.getTenantId()))
            .map(Setmeal::getId)
            .collect(Collectors.toList());
        if (!unauthorizedIds.isEmpty()) {
            return R.error("以下套餐不属于当前租户，无法删除：ID=" + unauthorizedIds);
        }
        setmealService.removeWithDish(idList);
        return R.success("套餐数据删除成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal 查询条件
     * @return 套餐列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询套餐列表", description = "根据条件查询套餐数据，支持分类ID和状态筛选")
    @Parameter(name = "setmeal", description = "套餐查询条件（categoryId分类ID、status状态）")
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.like(setmeal.getName() != null && !setmeal.getName().trim().isEmpty(), Setmeal::getName, setmeal.getName());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        // 多租户过滤：MyBatis-Plus TenantLineInnerInterceptor 已自动处理

        // 分页上限保护，防止全表扫描导致 OOM
        int maxPageSize = 200;
        Page<Setmeal> pageInfo = PageUtils.of(1, maxPageSize);
        setmealService.page(pageInfo, queryWrapper);
        if (pageInfo.getTotal() > maxPageSize) {
            log.warn("[setmeal/list] 查询结果共 {} 条，超过上限 {} 条，已截断返回", pageInfo.getTotal(), maxPageSize);
        }

        return R.success(pageInfo.getRecords());
    }

    /**
     * 解析逗号分隔的ID字符串为Long列表
     * 兼容前端传递的格式：单个ID("1")、逗号分隔("1,2,3")
     */
    private List<Long> parseIds(String ids) {
        if (ids == null || ids.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    /**
     * 获取筛选下拉选项（套餐名称列表）
     * <p>从数据库动态查询所有套餐名称，供前端下拉框使用</p>
     *
     * @return 包含 names 列表的 Map
     */
    @GetMapping("/options")
    @Operation(summary = "筛选选项", description = "获取所有套餐名称，供搜索条件下拉框使用")
    public R<Map<String, List<String>>> options() {
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        // 多租户过滤：MyBatis-Plus TenantLineInnerInterceptor 已自动处理
        queryWrapper.orderByAsc(Setmeal::getName);
        List<Setmeal> list = setmealService.list(queryWrapper);

        Set<String> nameSet = new HashSet<>();
        for (Setmeal meal : list) {
            if (meal.getName() != null && !meal.getName().isEmpty()) {
                nameSet.add(meal.getName());
            }
        }

        Map<String, List<String>> result = new HashMap<>();
        result.put("names", new ArrayList<>(nameSet));
        return R.success(result);
    }

}



