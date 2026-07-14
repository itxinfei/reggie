package com.reggie.module.printer.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印机配置
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("printer_config")
@Schema(description = "打印机配置")
public class PrinterConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "打印机配置ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "门店ID", example = "1")
    private Long storeId;

    @Schema(description = "打印机名称", example = "收银台打印机")
    private String name;

    @Schema(description = "打印机类型：network=网络打印机，usb=USB打印机，bluetooth=蓝牙打印机", example = "network")
    private String type;

    @Schema(description = "打印机品牌", example = "EPSON")
    private String brand;

    @Schema(description = "设备ID", example = "DEV001")
    private String deviceId;

    @Schema(description = "IP地址", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "端口号", example = "9100")
    private Integer port;

    @Schema(description = "纸张尺寸", example = "80mm")
    private String paperSize;

    @Schema(description = "打印类型（支持多类型绑定，逗号分隔）：BILL=收银小票，KITCHEN=厨房制作单，DELIVERY=配送单", example = "BILL,KITCHEN")
    private String printTypes;

    @Schema(description = "打印机状态：0=离线，1=在线", example = "1")
    private Integer status;

    @Schema(description = "排序号", example = "1")
    private Integer sort;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    private LocalDateTime updatedTime;

    @Schema(description = "系统打印机名称（CUPS名称或Windows打印机名）", example = "EPSON_TM_T88V")
    private String systemPrinterName;

    // 兼容别名：测试代码和部分API使用 printType（单数），此处做映射
    public void setPrintType(String printType) { this.printTypes = printType; }
    public String getPrintType() { return this.printTypes; }
}
