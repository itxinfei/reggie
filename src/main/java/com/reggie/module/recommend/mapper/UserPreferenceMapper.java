package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.UserPreferenceTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 用户偏好标签 Mapper
 * 修改点：新增口味偏好分布统计查询，替换原先硬编码假数据
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreferenceTag> {

    /**
     * 查询用户指定类型的偏好标签，按权重降序
     */
    @Select("SELECT * FROM user_preference_tag WHERE user_id = #{userId} AND tag_type = #{tagType} ORDER BY tag_value DESC")
    List<UserPreferenceTag> findByUserAndType(@Param("userId") Long userId, @Param("tagType") Integer tagType);

    /**
     * 查询用户所有偏好标签
     */
    @Select("SELECT * FROM user_preference_tag WHERE user_id = #{userId} ORDER BY tag_type, tag_value DESC")
    List<UserPreferenceTag> findByUserId(@Param("userId") Long userId);

    /**
     * 统计用户偏好标签数量
     */
    @Select("SELECT COUNT(*) FROM user_preference_tag WHERE user_id = #{userId}")
    int countByUserId(@Param("userId") Long userId);

    /**
     * 统计口味偏好标签分布（tag_type=1）
     * 用于概览页偏好饼图
     *
     * @return 每行: tag_name, user_count (去重用户数)
     */
    @Select("SELECT tag_name AS name, COUNT(DISTINCT user_id) AS value " +
            "FROM user_preference_tag " +
            "WHERE tag_type = 1 " +
            "GROUP BY tag_name " +
            "ORDER BY value DESC " +
            "LIMIT 10")
    List<Map<String, Object>> countTasteDistribution();
}
