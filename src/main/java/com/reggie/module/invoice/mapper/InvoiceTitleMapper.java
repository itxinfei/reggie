package com.reggie.module.invoice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.invoice.model.InvoiceTitle;
import org.apache.ibatis.annotations.Mapper;

/**
 * 发票抬头 Mapper
 */
@Mapper
public interface InvoiceTitleMapper extends BaseMapper<InvoiceTitle> {
}