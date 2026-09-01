package com.reggie.module.invoice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.reggie.common.BaseContext;
import com.reggie.common.CustomException;
import com.reggie.module.invoice.mapper.InvoiceRecordMapper;
import com.reggie.module.invoice.mapper.InvoiceTitleMapper;
import com.reggie.module.invoice.model.InvoiceRecord;
import com.reggie.module.invoice.model.InvoiceTitle;
import com.reggie.module.invoice.service.InvoiceService;
import com.reggie.module.order.model.Orders;
import com.reggie.module.order.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 发票服务实现
 */
@Slf4j
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceRecordMapper, InvoiceRecord> implements InvoiceService {

    @Autowired
    private InvoiceTitleMapper invoiceTitleMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Override
    public void saveTitle(InvoiceTitle title) {
        title.setTenantId(BaseContext.getCurrentTenantId());
        title.setType(title.getType() == null ? 1 : title.getType());
        invoiceTitleMapper.insert(title);
    }

    @Override
    public List<InvoiceTitle> listTitles(Long tenantId, Long userId) {
        // 发票抬头按租户隔离，userId 用于前端筛选（可选）
        LambdaQueryWrapper<InvoiceTitle> qw = new LambdaQueryWrapper<>();
        qw.eq(InvoiceTitle::getTenantId, tenantId);
        qw.orderByDesc(InvoiceTitle::getCreateTime);
        return invoiceTitleMapper.selectList(qw);
    }

    @Override
    public boolean deleteTitle(Long id, Long tenantId, Long userId) {
        InvoiceTitle title = invoiceTitleMapper.selectById(id);
        if (title == null || !tenantId.equals(title.getTenantId())) {
            throw new CustomException("发票抬头不存在");
        }
        return invoiceTitleMapper.deleteById(id) > 0;
    }

    @Override
    public InvoiceRecord applyInvoice(Long orderId, Long userId, Long tenantId, Long titleId, String title, String taxNumber, Integer type) {
        // 校验订单存在且已完成
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (order.getStatus() == null || order.getStatus() != Orders.STATUS_COMPLETED) {
            throw new CustomException("只有已完成的订单才能申请开票");
        }
        if (!tenantId.equals(order.getTenantId())) {
            throw new CustomException("无权操作该订单");
        }
        // 检查是否已申请过
        LambdaQueryWrapper<InvoiceRecord> existQw = new LambdaQueryWrapper<>();
        existQw.eq(InvoiceRecord::getOrderId, orderId);
        existQw.eq(InvoiceRecord::getTenantId, tenantId);
        InvoiceRecord existing = getOne(existQw, false);
        if (existing != null) {
            throw new CustomException("该订单已申请过发票");
        }

        InvoiceRecord record = new InvoiceRecord();
        record.setOrderId(orderId);
        record.setOrderNo(order.getNumber());
        record.setTitleId(titleId);
        record.setTitle(title);
        record.setTaxNumber(taxNumber);
        record.setType(type != null ? type : 1);
        record.setAmount(order.getAmount());
        // 修改点(2026-09-01)：申请即"已申请"，否则后台开票校验（仅接受 STATUS_APPLIED）永远无法流转
        record.setStatus(InvoiceRecord.STATUS_APPLIED);
        record.setTenantId(tenantId);
        record.setApplyTime(LocalDateTime.now());
        save(record);
        return record;
    }

    @Override
    public InvoiceRecord getInvoiceByOrder(Long orderId, Long userId, Long tenantId) {
        LambdaQueryWrapper<InvoiceRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(InvoiceRecord::getOrderId, orderId);
        qw.eq(InvoiceRecord::getTenantId, tenantId);
        return getOne(qw, false);
    }

    @Override
    public List<InvoiceRecord> listRecords(Integer status, Long tenantId) {
        LambdaQueryWrapper<InvoiceRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(InvoiceRecord::getTenantId, tenantId);
        if (status != null) {
            qw.eq(InvoiceRecord::getStatus, status);
        }
        qw.orderByDesc(InvoiceRecord::getCreateTime);
        return list(qw);
    }

    @Override
    public boolean issueInvoice(Long recordId, String invoiceNo, String invoiceCode, String invoiceUrl, Long tenantId) {
        InvoiceRecord record = getById(recordId);
        if (record == null || !tenantId.equals(record.getTenantId())) {
            throw new CustomException("发票记录不存在");
        }
        if (record.getStatus() == null || record.getStatus() != InvoiceRecord.STATUS_APPLIED) {
            throw new CustomException("只有已申请的发票才能开具");
        }
        record.setInvoiceNo(invoiceNo);
        record.setInvoiceCode(invoiceCode);
        record.setInvoiceUrl(invoiceUrl);
        record.setStatus(InvoiceRecord.STATUS_ISSUED);
        record.setIssueTime(LocalDateTime.now());
        return updateById(record);
    }

    @Override
    public boolean voidInvoice(Long recordId, Long tenantId) {
        InvoiceRecord record = getById(recordId);
        if (record == null || !tenantId.equals(record.getTenantId())) {
            throw new CustomException("发票记录不存在");
        }
        if (record.getStatus() == null || record.getStatus() != InvoiceRecord.STATUS_ISSUED) {
            throw new CustomException("只有已开具的发票才能作废");
        }
        record.setStatus(InvoiceRecord.STATUS_VOIDED);
        record.setUpdateTime(LocalDateTime.now());
        return updateById(record);
    }
}