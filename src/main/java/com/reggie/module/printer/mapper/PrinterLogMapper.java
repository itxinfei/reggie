package com.reggie.module.printer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.printer.model.PrinterLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打印日志 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface PrinterLogMapper extends BaseMapper<PrinterLog> {
}
