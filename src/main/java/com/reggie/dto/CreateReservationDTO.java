package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;

/**
 * 创建预订请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class CreateReservationDTO {

    @Schema(description = "客户姓名", required = true, example = "张三")
    @NotBlank(message = "客户姓名不能为空")
    private String customerName;

    @Schema(description = "手机号", required = true, example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "预订时间（格式：yyyy-MM-dd HH:mm:ss）", required = true,
            example = "2026-07-10 18:00:00")
    @NotNull(message = "预订时间不能为空")
    private LocalDateTime reservedTime;

    @Schema(description = "座位数", example = "4")
    @Min(value = 1, message = "座位数必须大于0")
    private Integer seatCount;

    @Schema(description = "桌台ID", example = "1")
    private Long tableId;

    @Schema(description = "备注", example = "靠窗位置")
    private String remark;
}
