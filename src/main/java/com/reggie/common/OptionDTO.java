package com.reggie.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一选项 DTO（用于下拉框、单选框等）
 *
 * @author reggie
 * @since 2026-08-10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "下拉选项")
public class OptionDTO {

    @Schema(description = "选项值")
    private String value;

    @Schema(description = "选项标签")
    private String label;

    /**
     * 快速创建选项
     */
    public static OptionDTO of(String value, String label) {
        return new OptionDTO(value, label);
    }
}
