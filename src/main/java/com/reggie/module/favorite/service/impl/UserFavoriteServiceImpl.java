package com.reggie.module.favorite.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.dish.mapper.DishMapper;
import com.reggie.module.dish.model.Dish;
import com.reggie.module.favorite.model.UserFavorite;
import com.reggie.module.favorite.mapper.UserFavoriteMapper;
import com.reggie.module.favorite.service.UserFavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户收藏服务实现。
 * <p>业务方法直接使用注入的 Mapper（与 ServiceImpl.baseMapper 为同一 Bean），
 * 不依赖 IService 门面方法，便于纯 Mockito 单测。</p>
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserFavoriteServiceImpl extends ServiceImpl<UserFavoriteMapper, UserFavorite>
        implements UserFavoriteService {

    @Autowired
    private UserFavoriteMapper favoriteMapper;

    @Autowired
    private DishMapper dishMapper;

    @Override
    public Map<String, Object> toggle(Integer targetType, Long targetId) {
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        if (userId == null) {
            throw new CustomException("请先登录");
        }
        if (targetType == null || targetId == null) {
            throw new CustomException("收藏参数不完整");
        }
        if (targetType != UserFavorite.TYPE_DISH) {
            throw new CustomException("暂不支持该类型的收藏");
        }
        // 校验菜品真实存在且属于当前租户，避免收藏幽灵商品
        Dish dish = dishMapper.selectById(targetId);
        if (dish == null || dish.getTenantId() == null || !dish.getTenantId().equals(tenantId)) {
            throw new CustomException("菜品不存在，无法收藏");
        }

        UserFavorite existing = findExisting(userId, tenantId, targetType, targetId);
        Map<String, Object> result = new HashMap<>();
        if (existing != null) {
            // 已收藏 → 取消（物理删除）
            favoriteMapper.deleteById(existing.getId());
            result.put("favorited", false);
        } else {
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setTargetType(targetType);
            favorite.setTargetId(targetId);
            favoriteMapper.insert(favorite);
            result.put("favorited", true);
        }
        return result;
    }

    @Override
    public List<Long> listFavoriteIds(Integer targetType) {
        Long userId = BaseContext.getCurrentId();
        List<Long> ids = new ArrayList<>();
        if (userId == null || targetType == null) {
            return ids;
        }
        LambdaQueryWrapper<UserFavorite> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, targetType);
        for (UserFavorite favorite : favoriteMapper.selectList(qw)) {
            ids.add(favorite.getTargetId());
        }
        return ids;
    }

    @Override
    public List<Map<String, Object>> listFavorites(Integer targetType) {
        Long userId = BaseContext.getCurrentId();
        Long tenantId = BaseContext.getCurrentTenantId();
        List<Map<String, Object>> views = new ArrayList<>();
        if (userId == null) {
            return views;
        }
        int type = targetType != null ? targetType : UserFavorite.TYPE_DISH;

        // 收藏记录按收藏时间倒序
        LambdaQueryWrapper<UserFavorite> favQw = new LambdaQueryWrapper<>();
        favQw.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTargetType, type)
                .orderByDesc(UserFavorite::getCreateTime)
                .orderByDesc(UserFavorite::getId);
        List<UserFavorite> favorites = favoriteMapper.selectList(favQw);
        if (favorites.isEmpty()) {
            return views;
        }

        List<Long> targetIds = new ArrayList<>();
        for (UserFavorite fav : favorites) {
            targetIds.add(fav.getTargetId());
        }
        // 实时关联菜品（MP 自动过滤 is_deleted=0），取最新名称/图片/价格/状态
        LambdaQueryWrapper<Dish> dishQw = new LambdaQueryWrapper<>();
        dishQw.in(Dish::getId, targetIds).eq(Dish::getTenantId, tenantId);
        Map<Long, Dish> dishMap = new HashMap<>();
        for (Dish dish : dishMapper.selectList(dishQw)) {
            dishMap.put(dish.getId(), dish);
        }

        for (UserFavorite fav : favorites) {
            Dish dish = dishMap.get(fav.getTargetId());
            Map<String, Object> view = new HashMap<>();
            view.put("targetId", fav.getTargetId());
            if (dish != null) {
                view.put("available", true);
                view.put("name", dish.getName());
                view.put("image", dish.getImage());
                view.put("price", dish.getPrice());
                view.put("status", dish.getStatus());
                view.put("onSale", dish.getStatus() != null && dish.getStatus() == 1);
            } else {
                // 菜品已被删除：标记失效，供用户清理
                view.put("available", false);
                view.put("name", "商品已失效");
                view.put("onSale", false);
            }
            views.add(view);
        }
        return views;
    }

    private UserFavorite findExisting(Long userId, Long tenantId, Integer targetType, Long targetId) {
        LambdaQueryWrapper<UserFavorite> qw = new LambdaQueryWrapper<>();
        qw.eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getTenantId, tenantId)
                .eq(UserFavorite::getTargetType, targetType)
                .eq(UserFavorite::getTargetId, targetId)
                .last("LIMIT 1");
        return favoriteMapper.selectOne(qw);
    }
}
