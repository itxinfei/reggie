package com.reggie.module.printer.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印终端（门店 PC 打印代理）
 *
 * <p>打印机安装在门店收银 PC 上，由本地打印代理程序调用 Windows 系统打印机；
 * 本表记录代理终端注册信息，后端据此派发打印任务（不直连服务器打印机）。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Data
@TableName("print_terminal")
@Schema(description = "打印终端（门店 PC 打印代理）")
public class PrintTerminal implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "终端ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "门店租户ID", example = "2")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "门店编码（代理注册时上报，用于匹配门店/租户）", example = "S0001")
    private String storeCode;

    @Schema(description = "终端唯一码（代理自生成 UUID）", example = "f4b8a1c0-...")
    private String terminalCode;

    @Schema(description = "代理鉴权 token（注册时后端生成）")
    private String token;

    @Schema(description = "终端名称", example = "收银台-01")
    private String name;

    @Schema(description = "本地打印机名（Windows 系统打印机名）", example = "EPSON TM-T88V")
    private String printerName;

    @Schema(description = "纸张：58mm / 80mm", example = "80mm")
    private String paperSize;

    @Schema(description = "绑定打印类型（逗号分隔）：BILL,KITCHEN,DELIVERY", example = "BILL,KITCHEN")
    private String printTypes;

    @Schema(description = "状态：0=停用（不派发任务）1=启用", example = "1")
    private Integer status;

    @Schema(description = "最近心跳时间（在线判定依据）")
    private LocalDateTime lastHeartbeat;

    @Schema(description = "代理客户端版本", example = "1.0.0")
    private String clientVersion;

    @Schema(description = "创建时间")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "是否删除：0=未删除，1=已删除", example = "0")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
