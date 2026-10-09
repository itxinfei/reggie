package com.reggie.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * 同步菜品到外卖平台请求DTO
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
public class SyncMenuDTO {

    @Schema(description = "外卖平台（MEITUAN-美团、ELEME-饿了么、DOUYIN-抖音、JD-京东）", required = true, example = "MEITUAN")
    @NotBlank(message = "平台不能为空")
    private String platform;

    @Schema(description = "菜品列表（可选，为空时由后端自动从数据库获取）")
    private List<Map<String, Object>> dishes;
}
