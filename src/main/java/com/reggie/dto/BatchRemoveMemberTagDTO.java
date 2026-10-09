package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * <p>
 * 批量删除会员标签请求DTO
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Data
public class BatchRemoveMemberTagDTO {

    @Schema(description = "标签ID列表", example = "[1, 2, 3]")
    @Size(max = 50, message = "标签ID列表不能超过50个")
    private List<Long> tagIds;
}
