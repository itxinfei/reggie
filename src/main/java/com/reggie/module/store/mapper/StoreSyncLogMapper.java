package com.reggie.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.store.model.StoreSyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店同步日志Mapper
 */
@Mapper
public interface StoreSyncLogMapper extends BaseMapper<StoreSyncLog> {
}
