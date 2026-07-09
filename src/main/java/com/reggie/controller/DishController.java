package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.DishDto;
import com.reggie.dto.dish.DishSaveDTO;
import com.reggie.entity.Category;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.enums.DishStatus;
import com.reggie.service.CategoryService;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
@Tag(name = "菜品管理", description = "菜品CRUD及口味管理接口")
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
     * @return 分页结果
     */
    @GetMapping("/page")
    @Operation(summary = "菜品分页查询", description = "分页查询菜品列表，支持按名称模糊搜索，自动关联分类名称")
    @Parameter(name = "page", description = "页码，从1开始", required = true, example = "1")
    @Parameter(name = "pageSize", description = "每页数量", required = true, example = "10")
    @Parameter(name = "name", description = "菜品名称（可选，模糊查询）")
    public R<Page<DishDto>> page(int page,int pageSize,String name){

        //构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page,pageSize);
        Page<DishDto> dishDtoPage = new Page<>();

        //条件构造器
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件
        queryWrapper.like(name != null,Dish::getName,name);
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

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto 菜品信息
     * @return 操作结果
     */
    @PutMapping
    @Operation(summary = "修改菜品", description = "更新菜品基本信息及口味信息")
    @Parameter(name = "dishDto", description = "菜品DTO（包含ID、基本信息及口味列表）", required = true)
    public R<String> update(@Valid @RequestBody DishDto dishDto){
        log.info("修改菜品：id={}, name={}", dishDto.getId(), dishDto.getName());

        dishService.updateWithFlavor(dishDto);

        return R.success("修改菜品成功");
    }

    /**
     * 修改点：原Controller中for循环逐条删除无事务保护，改为调用Service层事务方法
     */
    @DeleteMapping
    @Operation(summary = "删除菜品", description = "批量删除菜品及关联口味数据，自动校验套餐引用")
    @Parameter(name = "ids", description = "菜品ID列表，逗号分隔（如 1,2,3）", required = true)
    public R<String> delete(@RequestParam String ids) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return R.error("请选择要删除的菜品");
        }
        dishService.deleteWithFlavorCheck(idList);
        return R.success("删除成功");
    }

    @PostMapping("/status/{status}")
    @Operation(summary = "更新菜品状态", description = "批量更新菜品售卖状态（起售/停售）")
    @Parameter(name = "status", description = "状态值：1-起售，0-停售", required = true)
    @Parameter(name = "ids", description = "菜品ID列表，逗号分隔（如 1,2,3）", required = true)
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam String ids) {
        List<Long> idList = parseIds(ids);
        if (idList.isEmpty()) {
            return R.error("请选择要操作的菜品");
        }
        dishService.updateStatus(status, idList);
        return R.success("操作成功");
    }

    @GetMapping("/list")
    @Operation(summary = "查询菜品列表", description = "根据条件查询在售菜品数据，自动过滤停售菜品")
    @Parameter(name = "dish", description = "菜品查询条件（categoryId分类ID）")
    public R<List<DishDto>> list(Dish dish){
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        // 修改点：支持按菜品名称模糊搜索
        queryWrapper.like(dish.getName() != null && !dish.getName().trim().isEmpty(), Dish::getName, dish.getName());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus, DishStatus.ENABLED.getValue());

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);
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
        if (ids == null || ids.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

}
