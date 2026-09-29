package com.reggie.module.dining.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * C 端顾客到店预订请求
 *
 * @author reggie
 * @since 2026-09-29
 */
@Data
@Schema(description = "顾客到店预订请求")
public class CustomerReservationDTO {

    /** 顾客姓名（不传则用登录用户姓名） */
    @Size(max = 20, message = "姓名不能超过 20 字")
    @Schema(description = "顾客姓名，不传使用登录用户姓名")
    private String customerName;

    /** 预订时间 */
    @NotNull(message = "请选择预订时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预订时间", example = "2026-09-30 18:30:00")
    private LocalDateTime reservedTime;

    /** 就座人数 */
    @NotNull(message = "请选择就餐人数")
    @Min(value = 1, message = "就餐人数至少 1 人")
    @Max(value = 20, message = "就餐人数不能超过 20 人")
    @Schema(description = "就座人数", example = "2")
    private Integer seatCount;

    /** 指定桌台ID（可选） */
    @Schema(description = "指定桌台ID，可不传")
    private Long tableId;

    /** 备注 */
    @Size(max = 200, message = "备注不能超过 200 字")
    @Schema(description = "备注信息")
    private String remark;
}
