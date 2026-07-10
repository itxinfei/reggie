package com.reggie.module.printer.model;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印机配置信息类
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class PrinterConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 门店ID */
    private Long storeId;

    /** 打印机名称 */
    private String name;

    /** 打印机类型 */
    private String type;

    /** 打印机品牌 */
    private String brand;

    /** 设备ID */
    private String deviceId;

    /** IP地址 */
    private String ipAddress;

    /** 端口号 */
    private Integer port;

    /** 纸张尺寸 */
    private String paperSize;

    /**
     * 打印类型（支持多类型绑定，逗号分隔：BILL,KITCHEN,DELIVERY）
     * BILL - 收银小票，KITCHEN - 厨房制作单，DELIVERY - 配送单
     */
    private String printTypes;

    /** 打印机状态：0-离线，1-在线 */
    private Integer status;

    /** 排序号 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新时间 */
    private LocalDateTime updatedTime;

    /** 系统打印机名称 */
    private String systemPrinterName;

    // 兼容别名：测试代码和部分API使用 printType（单数），此处做映射
    public void setPrintType(String printType) { this.printTypes = printType; }
    public String getPrintType() { return this.printTypes; }
}
