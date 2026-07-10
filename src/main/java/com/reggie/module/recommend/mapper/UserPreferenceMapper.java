package com.reggie.module.recommend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.recommend.model.UserPreferenceTag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户偏好标签 Mapper
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
}
