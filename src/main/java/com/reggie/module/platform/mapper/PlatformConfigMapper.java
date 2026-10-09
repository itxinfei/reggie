package com.reggie.module.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.platform.model.PlatformConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 外卖平台接入配置 Mapper
 *
 * @author reggie
 * @since 2026-08-24
 */
@Mapper
public interface PlatformConfigMapper extends BaseMapper<PlatformConfig> {
}
