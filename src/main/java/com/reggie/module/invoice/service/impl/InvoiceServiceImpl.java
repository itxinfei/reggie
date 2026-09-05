package com.reggie.module.invoice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        // 修改点(2026-09-05)：IDOR 防护——订单归属校验，防止越权为他人订单申请开票
        if (userId == null || !Objects.equals(order.getUserId(), userId)) {
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
        record.setUserId(userId);
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
        // 修改点(2026-09-05)：IDOR 防护——先校验订单归属，再查询发票记录，防止越权查看他人订单发票
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new CustomException("订单不存在");
        }
        if (userId == null || !Objects.equals(order.getUserId(), userId)) {
            throw new CustomException("无权操作该订单");
        }
        LambdaQueryWrapper<InvoiceRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(InvoiceRecord::getOrderId, orderId);
        qw.eq(InvoiceRecord::getTenantId, tenantId);
        return getOne(qw, false);
    }

    @Override
    public Page<InvoiceRecord> listUserRecords(Page<InvoiceRecord> page, Long userId, Long tenantId) {
        LambdaQueryWrapper<InvoiceRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(InvoiceRecord::getUserId, userId);
        qw.eq(InvoiceRecord::getTenantId, tenantId);
        qw.orderByDesc(InvoiceRecord::getCreateTime);
        return page(page, qw);
    }

    @Override
    public boolean updateTitle(Long id, Long tenantId, String title, String taxNumber, String companyName, Integer type) {
        InvoiceTitle titleEntity = invoiceTitleMapper.selectById(id);
        if (titleEntity == null || !tenantId.equals(titleEntity.getTenantId())) {
            throw new CustomException("发票抬头不存在");
        }
        String trimmedTitle = title == null ? null : title.trim();
        if (trimmedTitle == null || trimmedTitle.isEmpty()) {
            throw new CustomException("请填写抬头名称");
        }
        Integer safeType = type != null ? type : titleEntity.getType();
        if (safeType == null || safeType != 2) {
            safeType = 1;
        }
        if (safeType == 2 && (taxNumber == null || taxNumber.trim().isEmpty())) {
            throw new CustomException("企业抬头请填写税号");
        }
        titleEntity.setTitle(trimmedTitle);
        titleEntity.setTaxNumber(taxNumber == null ? null : taxNumber.trim());
        titleEntity.setCompanyName(companyName == null ? null : companyName.trim());
        titleEntity.setType(safeType);
        titleEntity.setUpdateTime(LocalDateTime.now());
        return invoiceTitleMapper.updateById(titleEntity) > 0;
    }

    @Override
    public Page<InvoiceRecord> listRecords(Page<InvoiceRecord> page, Integer status, Long tenantId) {
        LambdaQueryWrapper<InvoiceRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(InvoiceRecord::getTenantId, tenantId);
        if (status != null) {
            qw.eq(InvoiceRecord::getStatus, status);
        }
        qw.orderByDesc(InvoiceRecord::getCreateTime);
        return page(page, qw);
    }

    @Override
    public Map<String, Integer> listStats(Long tenantId) {
        // 按状态分别 count（索引命中，避免全量加载行数据），统计卡全量不随分页/筛选变化
        Map<String, Integer> stats = new HashMap<>();
        stats.put("total", (int) count(statusQw(tenantId, null)));
        stats.put("applied", (int) count(statusQw(tenantId, InvoiceRecord.STATUS_APPLIED)));
        stats.put("issued", (int) count(statusQw(tenantId, InvoiceRecord.STATUS_ISSUED)));
        stats.put("voided", (int) count(statusQw(tenantId, InvoiceRecord.STATUS_VOIDED)));
        return stats;
    }

    /**
     * 按租户与状态构造统计查询条件
     *
     * @param tenantId 租户ID
     * @param status   开票状态（null 时不加状态条件）
     * @return 查询条件
     */
    private LambdaQueryWrapper<InvoiceRecord> statusQw(Long tenantId, Integer status) {
        LambdaQueryWrapper<InvoiceRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(InvoiceRecord::getTenantId, tenantId);
        if (status != null) {
            qw.eq(InvoiceRecord::getStatus, status);
        }
        return qw;
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