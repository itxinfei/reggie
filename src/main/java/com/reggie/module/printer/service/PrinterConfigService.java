package com.reggie.module.printer.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.reggie.module.printer.model.PrinterConfig;

import java.util.List;
import java.util.Map;

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

    /**
     * 打印机配置统计（总数、启用、停用、连接类型种类数）
     * <p>域4 改造：从 PrinterConfigController 下沉，Controller 不再直接操作 Mapper</p>
     *
     * @param tenantId 租户ID（可为 null，超管跳过过滤）
     * @return 聚合结果：totalPrinters/activePrinters/inactivePrinters/typeCount
     */
    Map<String, Object> statPrinters(Long tenantId);
}
