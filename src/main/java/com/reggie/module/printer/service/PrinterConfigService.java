package com.reggie.module.printer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.printer.model.PrinterConfig;

import java.util.List;

/**
 * <p>
 * 打印机配置服务接口
 * </p>
 * <p>管理小票打印机的配置信息（设备名称、IP地址、品牌型号等）</p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
public interface PrinterConfigService extends IService<PrinterConfig> {

    /**
     * 查询当前租户的打印机配置
     *
     * @return 打印机配置列表
     */
    List<PrinterConfig> listByTenant();

    /**
     * 根据打印机类型查询配置
     *
     * @param printerType 打印机类型
     * @return 打印机配置
     */
    PrinterConfig getByType(String printerType);
}
