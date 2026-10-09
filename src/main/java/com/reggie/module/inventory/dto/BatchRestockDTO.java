package com.reggie.module.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * 批量补货请求 DTO
 * 勾选多个预警食材 → 一键生成采购单并自动入库
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
@Schema(description = "批量补货请求")
public class BatchRestockDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "食材ID列表", required = true)
    @Size(max = 200, message = "食材ID列表不能超过200个")
    private List<Long> materialIds;

    @Schema(description = "补货明细：食材ID → 补货数量")
    @Size(max = 200, message = "补货明细不能超过200项")
    private List<RestockItem> items;

    @Schema(description = "采购单备注")
    private String remark;

    @Schema(description = "操作人")
    private String operator;

    /**
     * 单条补货明细
     */
    @Data
    @Schema(description = "补货明细项")
    public static class RestockItem implements Serializable {
        private static final long serialVersionUID = 1L;

        @Schema(description = "食材ID", required = true)
        private Long materialId;

        @Schema(description = "补货数量", required = true)
        private String qty;
    }
}