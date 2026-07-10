package com.reggie.module.dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dining.model.QueueRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排队记录 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface QueueMapper extends BaseMapper<QueueRecord> {
}
