package com.reggie.module.printer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.printer.model.PrinterConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 打印机配置 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface PrinterConfigMapper extends BaseMapper<PrinterConfig> {
}
