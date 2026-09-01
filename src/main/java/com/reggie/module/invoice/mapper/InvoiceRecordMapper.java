package com.reggie.module.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.invoice.model.InvoiceRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票申请记录 Mapper
 */
@Mapper
public interface InvoiceRecordMapper extends BaseMapper<InvoiceRecord> {
}