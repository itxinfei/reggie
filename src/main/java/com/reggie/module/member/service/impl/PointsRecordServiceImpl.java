package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.member.mapper.PointsRecordMapper;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.PointsRecordService;
import org.springframework.stereotype.Service;

/**
 * 积分记录服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements PointsRecordService {
}
