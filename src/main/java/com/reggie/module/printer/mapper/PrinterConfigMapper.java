package com.reggie.module.printer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.printer.model.PrinterConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打印机配置 Mapper
 *
 * @author reggie
 * @since 2026-07-09
 */
@Mapper
public interface PrinterConfigMapper extends BaseMapper<PrinterConfig> {
}
