package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 批量定向发券请求DTO（按会员ID列表发放）
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
public class IssueByMembersDTO {

    @Schema(description = "优惠券模板ID", required = true, example = "1")
    @NotNull(message = "优惠券模板ID不能为空")
    private Long templateId;

    @Schema(description = "会员ID列表", required = true, example = "[1, 2, 3]")
    @NotEmpty(message = "会员ID列表不能为空")
    @Size(max = 200, message = "会员ID列表不能超过200个")
    private List<Long> memberIds;
}