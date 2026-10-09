package com.reggie.module.favorite.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.favorite.model.UserFavorite;

import java.util.List;
import java.util.Map;

/**
 * 用户收藏服务
 */
public interface UserFavoriteService extends IService<UserFavorite> {

    /**
     * 切换收藏状态（已收藏→取消，未收藏→收藏）。
     *
     * @param targetType 收藏类型
     * @param targetId   收藏对象ID
     * @return {favorited: true/false}
     */
    Map<String, Object> toggle(Integer targetType, Long targetId);

    /**
     * 查询当前用户某类型的全部已收藏对象ID（供前端星标初始化）。
     */
    List<Long> listFavoriteIds(Integer targetType);

    /**
     * 收藏列表（菜品收藏实时关联菜品，取最新名称/图片/价格/在售状态）。
     */
    List<Map<String, Object>> listFavorites(Integer targetType);
}
