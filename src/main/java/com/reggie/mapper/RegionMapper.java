package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Region;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 行政区域 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface RegionMapper extends BaseMapper<Region> {
}
