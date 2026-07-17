package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 分类 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 排序号冲突处理：将指定 type 下 sort >= targetSort 且 id != excludeId 的记录 sort + 1
     * 修改点：参数化 @Update 替代 setSql 字符串拼接
     * @param type 分类类型
     * @param targetSort 目标排序号
     * @param excludeId 排除的分类ID（编辑时跳过自身），为 null 时不排除
     */
    @Update("UPDATE category SET sort = sort + 1 " +
            "WHERE type = #{type} AND sort >= #{targetSort} " +
            "AND (id != #{excludeId} OR #{excludeId} IS NULL)")
    int incrementSortByType(@Param("type") Integer type, @Param("targetSort") int targetSort, @Param("excludeId") Long excludeId);
}
