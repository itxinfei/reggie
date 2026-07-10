package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.Region;
import org.apache.ibatis.annotations.Mapper;

/**
 * 区域Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface RegionMapper extends BaseMapper<Region> {
}
