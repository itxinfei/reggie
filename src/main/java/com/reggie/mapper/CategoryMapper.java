package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Category;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
