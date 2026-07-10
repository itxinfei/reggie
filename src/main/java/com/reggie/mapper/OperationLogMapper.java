package com.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志Mapper接口
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
