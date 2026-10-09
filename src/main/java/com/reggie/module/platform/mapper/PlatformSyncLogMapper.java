package com.reggie.module.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.platform.model.PlatformSyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 平台同步操作日志 Mapper
 *
 * @author reggie
 * @since 2026-08-24
 */
@Mapper
public interface PlatformSyncLogMapper extends BaseMapper<PlatformSyncLog> {
}
