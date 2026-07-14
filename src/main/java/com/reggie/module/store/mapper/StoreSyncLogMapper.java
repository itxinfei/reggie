package com.reggie.module.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.store.model.StoreSyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 门店同步日志 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface StoreSyncLogMapper extends BaseMapper<StoreSyncLog> {
}
