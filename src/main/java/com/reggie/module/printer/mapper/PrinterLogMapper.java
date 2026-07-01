package com.reggie.module.printer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.printer.model.PrinterLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PrinterLogMapper extends BaseMapper<PrinterLog> {
}
