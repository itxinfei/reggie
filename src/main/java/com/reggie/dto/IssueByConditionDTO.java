package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Map;

/**
 * 条件定向发券请求DTO（按条件筛选会员后发放）
 * <p>
 * 支持条件（均在 condition 中传入）：
 * - levelId: 会员等级ID（等于匹配）
 * - status: 会员状态（0=禁用，1=启用）
 * - minPoints: 最小积分（>=）
 * - maxPoints: 最大积分（<=）
 * - minConsumption: 最小累计消费（>=）
 * - maxConsumption: 最大累计消费（<=）
 * - newMemberDays: 新会员天数（createdTime >= now - N 天）
 * </p>
 *
 * @author reggie
 * @since 2026-08-22
 */
@Data
public class IssueByConditionDTO {

    @Schema(description = "优惠券模板ID", required = true, example = "1")
    @NotNull(message = "优惠券模板ID不能为空")
    private Long templateId;

    @Schema(description = "筛选条件（可选：levelId/status/minPoints/maxPoints/minConsumption/maxConsumption/newMemberDays）",
            example = "{\"levelId\": 2, \"minPoints\": 100}")
    @Size(max = 20, message = "筛选条件不能超过20个")
    private Map<String, Object> condition;
}