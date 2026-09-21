package com.reggie.module.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.favorite.model.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户收藏 Mapper
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
