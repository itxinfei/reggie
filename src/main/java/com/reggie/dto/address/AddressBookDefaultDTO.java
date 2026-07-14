package com.reggie.dto.address;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * <p>
 * 地址设置默认DTO
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Data
@Schema(description = "地址设置默认")
public class AddressBookDefaultDTO {

    @Schema(description = "地址ID", required = true)
    @NotNull(message = "地址ID不能为空")
    private Long id;
}
