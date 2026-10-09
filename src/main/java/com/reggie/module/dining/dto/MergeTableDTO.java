package com.reggie.module.dining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 并台请求 DTO
 * <p>
 * 将多个桌台的订单合并为一张订单，适用于顾客想拼桌用餐场景。
 * </p>
 *
 * @author reggie
 * @since 2026-08-31
 */
@Data
@Schema(description = "并台请求")
public class MergeTableDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "主桌台ID不能为空")
    @Schema(description = "主桌台ID（合并后保留）", required = true, example = "1")
    private Long masterTableId;

    @NotNull
    @NotEmpty(message = "合并桌台列表不能为空")
    @Schema(description = "被合并的桌台ID列表", required = true, example = "[2, 3]")
    private List<Long> mergeTableIds;

    @Schema(description = "备注", example = "顾客要求拼桌")
    private String remark;
}
