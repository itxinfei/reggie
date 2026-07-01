package com.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.reggie.common.R;
import com.reggie.dto.SetmealDto;
import com.reggie.entity.Category;
import com.reggie.entity.Setmeal;
import com.reggie.entity.SetmealDish;
import com.reggie.service.CategoryService;
import com.reggie.service.SetmealDishService;
import com.reggie.service.SetmealService;
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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 套餐管理
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
     * @param setmealDto
     * @return
     */
    @PostMapping
    @Operation(summary = "新增套餐", description = "创建新的套餐及关联菜品")
    @Parameter(name = "setmealDto", description = "套餐DTO", required = true)
    public R<String> save(@RequestBody SetmealDto setmealDto){
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
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    @Operation(summary = "套餐分页查询", description = "分页查询套餐列表")
    @Parameter(name = "page", description = "页码", required = true)
    @Parameter(name = "pageSize", description = "每页数量", required = true)
    @Parameter(name = "name", description = "套餐名称（可选）")
    public R<Page<SetmealDto>> page(int page,int pageSize,String name){
        //分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);
        Page<SetmealDto> dtoPage = new Page<>();

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据name进行like模糊查询
        queryWrapper.like(name != null,Setmeal::getName,name);
        //添加排序条件，根据更新时间降序排列
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(pageInfo,queryWrapper);

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

    @GetMapping("/{id}")
    @Operation(summary = "查询套餐详情", description = "根据ID查询套餐及关联菜品")
    @Parameter(name = "id", description = "套餐ID", required = true)
    public R<SetmealDto> get(@PathVariable Long id) {
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        return R.success(setmealDto);
    }

    @PutMapping
    @Operation(summary = "修改套餐", description = "更新套餐基本信息及关联菜品")
    @Parameter(name = "setmealDto", description = "套餐DTO", required = true)
    public R<String> update(@RequestBody SetmealDto setmealDto) {
        setmealService.updateWithDish(setmealDto);
        return R.success("修改套餐成功");
    }

    @PostMapping("/status/{status}")
    @Operation(summary = "更新套餐状态", description = "批量更新套餐售卖状态")
    @Parameter(name = "status", description = "状态值", required = true)
    @Parameter(name = "ids", description = "套餐ID列表", required = true)
    public R<String> updateStatus(@PathVariable Integer status, @RequestParam List<Long> ids) {
        setmealService.updateStatus(status, ids);
        return R.success("操作成功");
    }

    @GetMapping("/dish/{id}")
    @Operation(summary = "查询套餐菜品", description = "查询套餐包含的菜品列表")
    @Parameter(name = "id", description = "套餐ID", required = true)
    public R<List<SetmealDish>> dish(@PathVariable Long id) {
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        return R.success(list);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    @Operation(summary = "删除套餐", description = "批量删除套餐")
    @Parameter(name = "ids", description = "套餐ID列表", required = true)
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("ids:{}",ids);

        setmealService.removeWithDish(ids);

        return R.success("套餐数据删除成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    @Operation(summary = "查询套餐列表", description = "根据条件查询套餐数据")
    @Parameter(name = "setmeal", description = "套餐查询条件")
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }
}
