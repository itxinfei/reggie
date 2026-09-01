package com.reggie.module.invoice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.invoice.model.InvoiceRecord;
import com.reggie.module.invoice.model.InvoiceTitle;

import java.util.List;
import java.util.Map;

/**
 * 发票服务接口
 */
public interface InvoiceService extends IService<InvoiceRecord> {

    /** 保存发票抬头 */
    void saveTitle(InvoiceTitle title);

    /** 获取用户发票抬头列表 */
    List<InvoiceTitle> listTitles(Long tenantId, Long userId);

    /** 删除发票抬头 */
    boolean deleteTitle(Long id, Long tenantId, Long userId);

    /** 申请开票 */
    InvoiceRecord applyInvoice(Long orderId, Long userId, Long tenantId, Long titleId, String title, String taxNumber, Integer type);

    /** 获取订单发票记录 */
    InvoiceRecord getInvoiceByOrder(Long orderId, Long userId, Long tenantId);

    /**
     * 获取发票列表（后台，分页）
     *
     * @param page     分页对象（PageUtils.of/cap 构造，pageSize 上限 100）
     * @param status   开票状态筛选（可选）
     * @param tenantId 租户ID
     * @return 分页发票记录
     */
    Page<InvoiceRecord> listRecords(Page<InvoiceRecord> page, Integer status, Long tenantId);

    /**
     * 发票状态统计（后台统计卡，按租户全量统计）
     *
     * @param tenantId 租户ID
     * @return 统计键值：total/applied/issued/voided
     */
    Map<String, Integer> listStats(Long tenantId);

    /** 开具发票 */
    boolean issueInvoice(Long recordId, String invoiceNo, String invoiceCode, String invoiceUrl, Long tenantId);

    /** 作废发票 */
    boolean voidInvoice(Long recordId, Long tenantId);
}