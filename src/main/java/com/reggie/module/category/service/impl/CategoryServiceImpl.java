package com.reggie.module.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.CustomException;
import com.reggie.common.RedisCacheUtil;
import com.reggie.module.category.model.Category;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.setmeal.model.Setmeal;
import com.reggie.module.category.mapper.CategoryMapper;
import com.reggie.module.category.service.CategoryService;
import com.reggie.module.dish.service.DishService;
import com.reggie.module.setmeal.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

/**
 * 分类服务实现类
 * <p>修改点（7月14日）：修复缓存顺序(先写DB后清缓存)、排序自动分配与冲突处理、同类型名称唯一性校验</p>
 *
 * @author reggie
 * @since 2026-07-09
 */
@Slf4j
/**
 * Category service implementation
 *
 * @author reggie
 * @since 2026-08-11
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private RedisCacheUtil redisCacheUtil;

    /**
     * 删除分类——先校验关联性再删除，最后清缓存
     *
     * @param id 分类ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        // 修改点：校验分类是否存在
        Category category = this.getById(id);
        if (category == null) {
            throw new CustomException("分类不存在或已被删除");
        }

        // 查询是否关联了菜品
        LambdaQueryWrapper<Dish> dishQuery = new LambdaQueryWrapper<>();
        dishQuery.eq(Dish::getCategoryId, id);
        int dishCount = dishService.count(dishQuery);
        if (dishCount > 0) {
            throw new CustomException("当前分类下关联了" + dishCount + "个菜品，不能删除");
        }

        // 查询是否关联了套餐
        LambdaQueryWrapper<Setmeal> setmealQuery = new LambdaQueryWrapper<>();
        setmealQuery.eq(Setmeal::getCategoryId, id);
        int setmealCount = setmealService.count(setmealQuery);
        if (setmealCount > 0) {
            throw new CustomException("当前分类下关联了" + setmealCount + "个套餐，不能删除");
        }

        // 修改点：先删DB成功
        super.removeById(id);
    }

    /**
     * 新增分类——名称唯一性校验 + 排序自动分配/冲突处理 + 写DB后清缓存
     *
     * @param entity 分类实体
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Category entity) {
        // 修改点：同类型下分类名称唯一性校验
        LambdaQueryWrapper<Category> nameCheck = new LambdaQueryWrapper<>();
        nameCheck.eq(Category::getType, entity.getType())
                 .eq(Category::getName, entity.getName());
        if (this.count(nameCheck) > 0) {
            throw new CustomException("该类型下已存在同名分类【" + entity.getName() + "】，请勿重复添加");
        }

        // 修改点：排序号处理——未指定或<=0则自动分配，否则处理冲突
        if (entity.getSort() == null || entity.getSort() <= 0) {
            int maxSort = getMaxSortByType(entity.getType());
            entity.setSort(maxSort + 1);
            log.info("自动分配排序号：type={}, sort={}", entity.getType(), entity.getSort());
        } else {
            resolveSortConflict(entity.getType(), entity.getSort(), null);
        }

        // 修改点：先写DB成功；并发竞态由唯一索引兜底，转友好提示
        try {
            boolean result = super.save(entity);
            return result;
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new CustomException("该分类名称已存在（并发冲突），请刷新后重试");
        }
    }

    /**
     * 更新分类——校验存在性 + 防type篡改 + 名称唯一性校验 + 排序冲突处理 + 写DB后清缓存
     *
     * @param entity 分类实体
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Category entity) {
        if (entity.getId() == null) {
            throw new CustomException("分类ID不能为空");
        }

        // 修改点：校验分类是否存在
        Category existing = this.getById(entity.getId());
        if (existing == null) {
            throw new CustomException("分类不存在或已被删除");
        }

        // 修改点：禁止修改分类类型
        if (entity.getType() != null && !entity.getType().equals(existing.getType())) {
            throw new CustomException("不允许修改分类类型，请删除后重新创建");
        }
        entity.setType(existing.getType());

        // 修改点：改名时校验同类型下是否重名
        if (entity.getName() != null && !entity.getName().equals(existing.getName())) {
            LambdaQueryWrapper<Category> nameCheck = new LambdaQueryWrapper<>();
            nameCheck.eq(Category::getType, entity.getType())
                     .eq(Category::getName, entity.getName())
                     .ne(Category::getId, entity.getId());
            if (this.count(nameCheck) > 0) {
                throw new CustomException("该类型下已存在同名分类【" + entity.getName() + "】，请修改名称");
            }
        }

        // 修改点：排序号变更时处理冲突
        if (entity.getSort() != null && !entity.getSort().equals(existing.getSort())) {
            resolveSortConflict(entity.getType(), entity.getSort(), entity.getId());
        }

        // 修改点：先写DB成功，再清缓存；并发竞态由唯一索引兜底，转友好提示
        boolean result;
        try {
            result = super.updateById(entity);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new CustomException("该分类名称已存在（并发冲突），请修改名称");
        }
        if (result) {
            redisCacheUtil.doubleDeleteAllEntries("categories");
        }
        return result;
    }

    // ==================== 修改点：以下为新增的辅助方法 ====================

    /**
     * 获取指定类型的最大排序号
     *
     * @param type 分类类型 1=菜品分类 2=套餐分类
     * @return 最大排序号，无数据返回0
     */
    private int getMaxSortByType(Integer type) {
        LambdaQueryWrapper<Category> query = new LambdaQueryWrapper<>();
        query.eq(Category::getType, type)
             .orderByDesc(Category::getSort)
             .last("LIMIT 1");
        Category max = this.getOne(query);
        return (max != null && max.getSort() != null) ? max.getSort() : 0;
    }

    /**
     * 解决排序号冲突——将同类型中 sort >= targetSort 的记录批量 +1
     * <p>使用单条 SQL 批量更新，避免逐条 UPDATE</p>
     *
     * @param type       分类类型
     * @param targetSort 目标排序号
     * @param excludeId  排除的分类ID（编辑时跳过自身）
     */
    private void resolveSortConflict(Integer type, int targetSort, Long excludeId) {
        int affected = getBaseMapper().incrementSortByType(type, targetSort, excludeId);
        log.info("排序号冲突处理完成：type={}, targetSort={}, 影响{}条记录", type, targetSort, affected);
    }
}




