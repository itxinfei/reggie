package com.reggie.module.printer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 打印代理注册请求
 *
 * <p>门店 PC 打印代理首次启动 / 配置变更时调用，用于登记终端并获取鉴权 token。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Data
@Schema(description = "打印代理注册请求")
public class AgentRegisterDTO {

    @Schema(description = "门店编码（系统内门店的 store_code）", example = "S0001", required = true)
    @NotBlank(message = "门店编码不能为空")
    @Size(max = 50, message = "门店编码过长")
    private String storeCode;

    @Schema(description = "终端唯一码（代理自生成 UUID，首次生成后持久化保存）", required = true)
    @NotBlank(message = "终端编码不能为空")
    @Size(max = 64, message = "终端编码过长")
    private String terminalCode;

    @Schema(description = "终端名称", example = "收银台-01")
    @Size(max = 100, message = "终端名称过长")
    private String name;

    @Schema(description = "本机默认打印机名（留空则后端保留原值）", example = "EPSON TM-T88V")
    @Size(max = 200, message = "打印机名过长")
    private String printerName;

    @Schema(description = "纸张：58mm / 80mm", example = "80mm")
    private String paperSize;

    @Schema(description = "代理客户端版本", example = "1.0.0")
    @Size(max = 30, message = "版本号过长")
    private String clientVersion;
}
