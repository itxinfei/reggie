package com.reggie.module.member.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.member.mapper.PointsRecordMapper;
import com.reggie.module.member.model.PointsRecord;
import com.reggie.module.member.service.PointsRecordService;
import org.springframework.stereotype.Service;

@Service
public class PointsRecordServiceImpl extends ServiceImpl<PointsRecordMapper, PointsRecord> implements PointsRecordService {
}
