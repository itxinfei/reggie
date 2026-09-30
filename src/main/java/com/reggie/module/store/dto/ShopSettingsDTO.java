package com.reggie.module.store.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 店铺设置更新 DTO（商家自助，C 端品牌化，R-21-A 配套）
 * <p>白名单字段：店名 / Logo / 公告 / 营业时间。目标租户强制取当前登录租户上下文，
 * 不接受前端传入 tenantId，防止越权修改他店；status/phone/passwordType 等敏感字段
 * 不在本白名单，仍走超管租户管理接口。</p>
 *
 * @author reggie
 * @since 2026-09-30
 */
@Data
@Schema(description = "店铺设置更新对象（商家自助）")
public class ShopSettingsDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "店铺名称", example = "张三的小馆", required = true)
    @NotBlank(message = "店铺名称不能为空")
    @Size(max = 64, message = "店铺名称不能超过64个字符")
    private String name;

    @Schema(description = "Logo 图片路径（相对或绝对），空表示不修改", example = "/uploads/public/logo_xxx.png")
    @Size(max = 255, message = "Logo路径不能超过255个字符")
    private String logo;

    @Schema(description = "门店公告（C 端首页展示），空表示不修改", example = "本店精选新鲜食材，欢迎品尝")
    @Size(max = 200, message = "门店公告不能超过200个字符")
    private String notice;

    @Schema(description = "营业时间，空表示不修改", example = "09:00-22:00")
    @Size(max = 100, message = "营业时间不能超过100个字符")
    private String businessHours;
}
