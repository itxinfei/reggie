package com.reggie.module.dish.controller;
import com.reggie.common.RateLimit;
import com.reggie.common.annotation.RequireEmployee;
import com.reggie.common.utils.PageUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.BaseContext;
import com.reggie.common.R;
import com.reggie.dto.DishDto;
import com.reggie.dto.dish.DishSaveDTO;
import com.reggie.module.category.model.Category;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.dish.model.DishFlavor;
import com.reggie.enums.DishStatus;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.dish.service.DishFlavorService;
import com.reggie.module.dish.service.DishService;
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
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 菜品管理
 *
 * @author reggie
 * @since 2026-07-09
 */
@RestController
@RequestMapping("/dish")
@Slf4j
@Tag(name = "菜品管理", description = "菜品CRUD及口味管理接口")
@Validated
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品
     * @param dishSaveDTO 菜品信息（包含基本信息及口味）
     * @return 操作结果
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @PostMapping
    @Operation(summary = "新增菜品", description = "保存菜品基本信息及口味信息，支持多规格口味配置")
    @Parameter(name = "dishSaveDTO", description = "菜品信息DTO（名称、分类、价格、编码、图片、描述、状态、口味列表）", required = true)
    public R<String> save(@Valid @RequestBody DishSaveDTO dishSaveDTO){
        log.info("新增菜品：name={}, categoryId={}", dishSaveDTO.getName(), dishSaveDTO.getCategoryId());

        // 转换为Dish实体
        Dish dish = new Dish();
        dish.setName(dishSaveDTO.getName());
        dish.setCategoryId(dishSaveDTO.getCategoryId());
        dish.setPrice(dishSaveDTO.getPrice());
        dish.setCode(dishSaveDTO.getCode());
        dish.setImage(dishSaveDTO.getImage());
        dish.setDescription(dishSaveDTO.getDescription());
        dish.setStatus(dishSaveDTO.getStatus());
        dish.setSort(dishSaveDTO.getSort());
        dish.setStockQty(dishSaveDTO.getStockQty());
        dish.setMinStock(dishSaveDTO.getMinStock());

        // 提取口味列表（可能为空）
        List<DishFlavor> flavors = dishSaveDTO.getFlavors();

        // 调用事务保护的服务方法保存菜品及口味
        dishService.saveDish(dish, flavors);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     * @param page 页码
     * @param pageSize 每页数量
     * @param name 菜品名称（可选，模糊查询）
     * @param status 售卖状态（可选，'0'=停售 ,'1'=启售）
     * @param categoryId 菜品分类ID（可选）
     * @return 分页结果
     */
    @RequireEmployee
        @GetMapping("/page")
    @Operation(summary = "菜品分页查询", description = "分页查询菜品列表，支持按名称模糊搜索、状态筛选和分类筛选，自动关联分类名称")
    @Parameter(name = "page", description = "页码，从1开始", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "菜品名称（可选，模糊查询）")
    @Parameter(name = "status", description = "售卖状态（可选，'0'=停售 ,'1'=启售）")
    @Parameter(name = "categoryId", description = "菜品分类ID（可选）")
    @Parameter(name = "code", description = "商品码（可选，模糊查询）")
    public R<Page<DishDto>> page(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String name,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) Long categoryId,
                                  @Parameter(description = "code")
                                  @RequestParam(required = false) String code){

        //构造分页构造器对象
        Page<Dish> pageInfo = PageUtils.of(page,pageSize);
        Page<DishDto> dishDtoPage = PageUtils.of(page, pageSize);

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null && !name.isEmpty(), Dish::getName, name);
        queryWrapper.eq(status != null && !status.isEmpty(), Dish::getStatus, status);
        queryWrapper.eq(categoryId != null, Dish::getCategoryId, categoryId);
        queryWrapper.like(code != null && !code.isEmpty(), Dish::getCode, code);
        //多租户过滤
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(Dish::getTenantId, tenantId);
        }
        //添加排序条件
        queryWrapper.orderByDesc(Dish::getUpdateTime);

        //执行分页查询
        dishService.page(pageInfo,queryWrapper);

        //对象拷贝
        BeanUtils.copyProperties(pageInfo,dishDtoPage,"records");

        List<Dish> records = pageInfo.getRecords();
        if (!records.isEmpty()) {
            List<Long> categoryIds = records.stream().map(Dish::getCategoryId).filter(Objects::nonNull).collect(Collectors.toList());
            Map<Long, String> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap() :
                categoryService.listByIds(categoryIds).stream()
                    .collect(Collectors.toMap(Category::getId, Category::getName));

            List<DishDto> list = records.stream().map((item) -> {
                DishDto dishDto = new DishDto();
                BeanUtils.copyProperties(item, dishDto);
                dishDto.setCategoryName(categoryMap.get(item.getCategoryId()));
                return dishDto;
            }).collect(Collectors.toList());

            dishDtoPage.setRecords(list);
        }

        return R.success(dishDtoPage);
    }

    /**
     * 根据id查询菜品信息和对应的口味信息
     * @param id 菜品ID
     * @return 菜品详情及口味信息
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询菜品详情", description = "根据ID查询菜品基本信息及关联口味信息")
    @Parameter(name = "id", description = "菜品ID", required = true)
    public R<DishDto> get(@PathVariable Long id){
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        if (dishDto == null) {
            return R.error("菜品不存在");
        }
        // 租户校验：确保只能查询当前租户的菜品
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (dishDto.getTenantId() != null && !dishDto.getTenantId().equals(currentTenantId)) {
            return R.error("菜品不存在");
        }
        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto 菜品信息
     * @return 操作结果
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @PutMapping
    @Operation(summary = "修改菜品", description = "更新菜品基本信息及口味信息")
    @Parameter(name = "dishDto", description = "菜品DTO（包含ID、基本信息及口味列表）", required = true)
    public R<String> update(@Valid @RequestBody DishDto dishDto){
        log.info("修改菜品：id={}, name={}", dishDto.getId(), dishDto.getName());
        // 租户校验：确保只能修改当前租户的菜品
        Dish existing = dishService.getById(dishDto.getId());
        if (existing == null) {
            return R.error("菜品不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (!currentTenantId.equals(existing.getTenantId())) {
            return R.error("菜品不存在");
        }
        dishService.updateWithFlavor(dishDto);
        return R.success("修改菜品成功");
    }

    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @DeleteMapping
    @Operation(summary = "删除菜品", description = "批量删除菜品及关联口味数据，自动校验套餐引用")
    @Parameter(name = "ids", description = "菜品ID列表，逗号分隔（如 1,2,3）", required = true)
    public R<String> delete(@RequestParam String ids) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return R.error("请选择要删除的菜品");
        }
        // 租户校验：确保只能删除当前租户的菜品
        Long currentTenantId = BaseContext.getCurrentTenantId();
        List<Dish> dishes = dishService.listByIds(idList);
        List<Long> unauthorizedIds = dishes.stream()
            .filter(d -> !currentTenantId.equals(d.getTenantId()))
            .map(Dish::getId)
            .collect(Collectors.toList());
        if (!unauthorizedIds.isEmpty()) {
            return R.error("以下菜品不属于当前租户，无法删除：ID=" + unauthorizedIds);
        }
        dishService.deleteWithFlavorCheck(idList);
        return R.success("删除成功");
    }

    /**
     * 批量更新菜品状态
     *
     * @param status 状态值：1-起售，0-停售
     * @param ids 菜品ID列表，逗号分隔（如 1,2,3）
     * @return 操作结果
     */
    @RequireEmployee
    @RateLimit(maxRequestsPerSecond = 10)
    @PostMapping("/status/{status}")
    @Operation(summary = "更新菜品状态", description = "批量更新菜品售卖状态（起售/停售）")
    @Parameter(name = "status", description = "状态值：1-起售，0-停售", required = true)
    @Parameter(name = "ids", description = "菜品ID列表，逗号分隔（如 1,2,3）", required = true)
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam String ids) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return R.error("请选择要操作的菜品");
        }
        // 租户校验
        Long currentTenantId = BaseContext.getCurrentTenantId();
        List<Dish> dishes = dishService.listByIds(idList);
        List<Long> unauthorizedIds = dishes.stream()
            .filter(d -> !currentTenantId.equals(d.getTenantId()))
            .map(Dish::getId)
            .collect(Collectors.toList());
        if (!unauthorizedIds.isEmpty()) {
            return R.error("以下菜品不属于当前租户，无法操作：ID=" + unauthorizedIds);
        }
        dishService.updateStatus(status, idList);
        return R.success("操作成功");
    }

    /**
     * 根据条件查询菜品列表
     * 内部使用分页查询限制数据量（最多200条），防止全表扫描导致 OOM
     *
     * @param dish 菜品查询条件（categoryId分类ID）
     * @return 菜品列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询菜品列表", description = "根据条件查询在售菜品数据，自动过滤停售菜品，最多返回200条")
    @Parameter(name = "dish", description = "菜品查询条件（categoryId分类ID、name名称模糊查询）")
    public R<List<DishDto>> list(Dish dish){
        int maxPageSize = 200;

        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        queryWrapper.like(dish.getName() != null && !dish.getName().trim().isEmpty(), Dish::getName, dish.getName());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus, DishStatus.ENABLED.getValue());

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        // 多租户过滤：显式限制当前租户数据
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            queryWrapper.eq(Dish::getTenantId, tenantId);
        }

        // 分页查询，防止全表扫描
        Page<Dish> pageInfo = PageUtils.of(1, maxPageSize);
        dishService.page(pageInfo, queryWrapper);
        // 达到上限时记录警告，避免静默截断
        if (pageInfo.getTotal() > maxPageSize) {
            log.warn("[dish/list] 查询结果共 {} 条，超过上限 {} 条，已截断返回；建议按分类或分页查询", pageInfo.getTotal(), maxPageSize);
        }

        List<Dish> list = pageInfo.getRecords();
        if (list.isEmpty()) {
            return R.success(new ArrayList<>());
        }

        //批量查询分类名称
        List<Long> categoryIds = list.stream().map(Dish::getCategoryId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, String> categoryMap = categoryIds.isEmpty() ? Collections.emptyMap() :
            categoryService.listByIds(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        //批量查询口味
        List<Long> dishIds = list.stream().map(Dish::getId).collect(Collectors.toList());
        LambdaQueryWrapper<DishFlavor> flavorWrapper = new LambdaQueryWrapper<>();
        flavorWrapper.in(DishFlavor::getDishId, dishIds);
        List<DishFlavor> allFlavors = dishFlavorService.list(flavorWrapper);
        Map<Long, List<DishFlavor>> flavorMap = allFlavors.stream()
            .collect(Collectors.groupingBy(DishFlavor::getDishId));

        List<DishDto> dishDtoList = list.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item,dishDto);

            dishDto.setCategoryName(categoryMap.get(item.getCategoryId()));
            dishDto.setFlavors(flavorMap.get(item.getId()));

            return dishDto;
        }).collect(Collectors.toList());

        return R.success(dishDtoList);
    }

    /**
     * 解析逗号分隔的ID字符串为Long列表
     * 兼容前端传递的格式：单个ID("1")、逗号分隔("1,2,3")、数组("1&ids=2")
     */
    private List<Long> parseIds(String ids) {
        return com.reggie.common.ControllerUtils.parseIds(ids);
    }

    /**
     * 获取筛选下拉选项（菜品名称列表）
     * <p>从数据库动态查询所有菜品名称，供前端下拉框使用</p>
     */
    @GetMapping("/options")
    @Operation(summary = "筛选选项", description = "获取所有菜品名称，供搜索条件下拉框使用")
    public R<Map<String, List<String>>> options() {
        LambdaQueryWrapper<Dish> qw = new LambdaQueryWrapper<>();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (tenantId != null) {
            qw.eq(Dish::getTenantId, tenantId);
        }
        qw.orderByAsc(Dish::getName);
        List<Dish> list = dishService.list(qw);
        Set<String> nameSet = new HashSet<>();
        for (Dish d : list) {
            if (d.getName() != null && !d.getName().isEmpty()) { nameSet.add(d.getName()); }
        }
        Map<String, List<String>> result = new HashMap<>();
        result.put("names", new ArrayList<>(nameSet));
        return R.success(result);
    }

    /**
     * 获取菜品统计数据（轻量接口，仅COUNT查询，不拉取全量数据）
     * @return 统计数据（total/active/inactive/lowStock/soldOut）
     */
    @RequireEmployee
        @GetMapping("/stats")
    @Operation(summary = "菜品统计", description = "获取菜品统计数据（总数、起售数、停售数、低库存数、售罄数），轻量COUNT查询")
    public R<Map<String, Object>> stats() {
        Map<String, Object> stats = dishService.getStats();
        return R.success(stats);
    }

    /**
     * 更新菜品库存
     * @param id 菜品ID
     * @param stockQty 库存数量
     * @param minStock 最低库存预警值
     * @return 操作结果
     */
    @RequireEmployee
        @PutMapping("/stock/{id}")
    @Operation(summary = "更新菜品库存", description = "更新菜品的库存数量和最低库存预警值")
    @Parameter(name = "id", description = "菜品ID", required = true)
    @Parameter(name = "stockQty", description = "库存数量（不能小于0）", required = true)
    @Parameter(name = "minStock", description = "最低库存预警值（不能小于0）", required = true)
    public R<String> updateStock(@PathVariable Long id,
                                  @RequestParam @NotNull(message = "库存数量不能为空") BigDecimal stockQty,
                                  @RequestParam @NotNull(message = "最低库存不能为空") BigDecimal minStock) {
        // 租户校验
        Dish dish = dishService.getById(id);
        if (dish == null) {
            return R.error("菜品不存在");
        }
        Long currentTenantId = BaseContext.getCurrentTenantId();
        if (!currentTenantId.equals(dish.getTenantId())) {
            return R.error("菜品不存在");
        }
        dishService.updateStock(id, stockQty, minStock);
        return R.success("库存更新成功");
    }

}




