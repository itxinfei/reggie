package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.DishDto;
import com.reggie.entity.Category;
import com.reggie.entity.Dish;
import com.reggie.entity.DishFlavor;
import com.reggie.enums.DishStatus;
import com.reggie.service.CategoryService;
import com.reggie.service.DishFlavorService;
import com.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/dish")
@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.saveWithFlavor(dishDto);

        return R.success("新增菜品成功");
    }

    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
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
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){

        DishDto dishDto = dishService.getByIdWithFlavor(id);

        return R.success(dishDto);
    }

    /**
     * 修改菜品
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        log.info(dishDto.toString());

        dishService.updateWithFlavor(dishDto);

        return R.success("修改菜品成功");
    }

    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids) {
        dishService.removeByIds(ids);
        return R.success("删除成功");
    }

    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        dishService.updateStatus(status, ids);
        return R.success("操作成功");
    }

    /**
     * 根据条件查询对应的菜品数据
     * @param dish
     * @return
     */
    /*@GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
        //添加条件，查询状态为1（起售状态）的菜品
        queryWrapper.eq(Dish::getStatus, DishStatus.ENABLED.getValue());

        //添加排序条件
        queryWrapper.orderByAsc(Dish::getSort).orderByDesc(Dish::getUpdateTime);

        List<Dish> list = dishService.list(queryWrapper);

        return R.success(list);
    }*/

    @GetMapping("/list")
    public R<List<DishDto>> list(Dish dish){
        //构造查询条件
        LambdaQueryWrapper<Dish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(dish.getCategoryId() != null ,Dish::getCategoryId,dish.getCategoryId());
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

}
