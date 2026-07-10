package com.reggie.module.printer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.printer.model.PrinterConfig;

/**
 * 打印机配置服务接口
 * 管理小票打印机的配置信息（设备名称、IP地址、品牌型号等）
 *
 * @author reggie
 * @since 2026-07-09
 */
public interface PrinterConfigService extends IService<PrinterConfig> {
}
