package com.reggie.module.dining.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.dining.model.QueueRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QueueMapper extends BaseMapper<QueueRecord> {
}
