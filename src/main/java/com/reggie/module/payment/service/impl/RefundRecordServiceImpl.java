package com.reggie.module.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.enums.RefundStatus;
import com.reggie.module.payment.mapper.PaymentOrderMapper;
import com.reggie.module.payment.mapper.RefundRecordMapper;
import com.reggie.module.payment.model.RefundRecord;
import com.reggie.module.payment.service.RefundRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 退款记录服务实现
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RefundRecordServiceImpl extends ServiceImpl<RefundRecordMapper, RefundRecord> implements RefundRecordService {

    /** 退款流水号时间格式 */
    private static final DateTimeFormatter REFUND_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 支付单Mapper（用于累计退款金额查询） */
    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Override
    public List<RefundRecord> listByOrderId(Long orderId) {
        return this.list(new LambdaQueryWrapper<RefundRecord>()
                .eq(RefundRecord::getPaymentOrderId, orderId)
                .eq(RefundRecord::getTenantId, BaseContext.getCurrentTenantId())
                .orderByDesc(RefundRecord::getCreatedTime));
    }

    @Override
    public RefundRecord createRefund(Long paymentOrderId, BigDecimal amount, String reason) {
        RefundRecord record = new RefundRecord();
        record.setPaymentOrderId(paymentOrderId);
        record.setTenantId(BaseContext.getCurrentTenantId());
        record.setRefundNo(generateRefundNo());
        record.setAmount(amount);
        record.setReason(reason);
        record.setStatus(RefundStatus.PENDING.getCode());
        record.setCreatedTime(LocalDateTime.now());
        this.save(record);
        return record;
    }

    @Override
    public void markRefundSuccess(String refundNo) {
        // 将退款记录从 PENDING 更新为 SUCCESS，修复原先记录永远停留在 PENDING 的问题
        this.update(new LambdaUpdateWrapper<RefundRecord>()
                .eq(RefundRecord::getRefundNo, refundNo)
                .eq(RefundRecord::getStatus, RefundStatus.PENDING.getCode())
                .set(RefundRecord::getStatus, RefundStatus.SUCCESS.getCode()));
    }

    @Override
    public BigDecimal sumRefundedAmount(Long paymentOrderId) {
        // 查询该支付单已成功退款的总额（跨租户由 Mapper 的 @InterceptorIgnore 保证）
        BigDecimal sum = paymentOrderMapper.sumRefundedAmount(paymentOrderId, RefundStatus.SUCCESS.getCode());
        return sum == null ? BigDecimal.ZERO : sum;
    }

    /**
     * 生成退款流水号：RF + 时间戳 + UUID（保证唯一性）
     */
    private String generateRefundNo() {
        return "RF" + LocalDateTime.now().format(REFUND_NO_FMT)
            + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}


