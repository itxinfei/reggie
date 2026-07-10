package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface UserMapper extends BaseMapper<User>{
}
