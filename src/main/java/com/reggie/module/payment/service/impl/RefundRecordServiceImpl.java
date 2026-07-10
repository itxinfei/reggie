package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.module.payment.mapper.RefundRecordMapper;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.RefundRecordService;
import org.springframework.stereotype.Service;

/**
 * 退款记录服务实现
 *
 * @author reggie
 * @since 2026-07-09
 */
@Service
public class RefundRecordServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecord> implements RefundRecordService {
}
