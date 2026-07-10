package com.reggie.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.member.model.PointsRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 积分记录 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface PointsRecordMapper extends BaseMapper<PointsRecord> {
}
