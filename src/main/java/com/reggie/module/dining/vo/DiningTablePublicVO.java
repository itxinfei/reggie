package com.reggie.module.dining.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 扫码点餐公开桌台视图（C 端顾客可见，仅含非敏感展示字段）
 * <p>用于顾客扫描桌上二维码后在浏览器中点餐页展示桌台信息，
 * 不暴露 tenantId、订单、最低消费等内部数据。</p>
 *
 * @author reggie
 * @since 2026-09-17
 */
@Data
@Schema(description = "扫码点餐公开桌台信息")
public class DiningTablePublicVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "桌台ID", example = "9100016001")
    private Long id;

    @Schema(description = "桌台名称/编号", example = "A01")
    private String name;

    @Schema(description = "座位数", example = "4")
    private Integer seatCount;

    @Schema(description = "桌台状态：FREE=空闲，OCCUPIED=使用中，RESERVED=已预订，CLEANING=清洁中", example = "FREE")
    private String status;

    @Schema(description = "区域名称（非数据库字段）", example = "大厅")
    private String areaName;
}
