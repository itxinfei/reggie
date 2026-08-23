package com.reggie.module.dining.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 用餐桌台信息
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("dining_table")
@Schema(description = "用餐桌台")
public class DiningTable implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "桌台ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    private Long tenantId;

    @Schema(description = "区域ID", example = "1")
    private Long areaId;

    @Schema(description = "区域名称（非数据库字段，用于关联查询）", example = "大厅")
    @TableField(exist = false)
    private String areaName;

    @Schema(description = "桌台名称/编号", example = "A01")
    @NotBlank(message = "桌台名称不能为空")
    private String name;

    @Schema(description = "座位数", example = "4")
    @NotNull(message = "座位数不能为空")
    @Positive(message = "座位数必须大于0")
    private Integer seatCount;

    @Schema(description = "桌台状态：FREE=空闲，OCCUPIED=使用中，RESERVED=已预订，CLEANING=清洁中", example = "FREE")
    private String status;

    @Schema(description = "最低消费金额（元）", example = "50.00")
    private BigDecimal minAmount;

    @Schema(description = "桌台二维码URL", example = "https://xxx.com/qr/A01.png")
    private String qrCodeUrl;

    @Schema(description = "排序号（升序）", example = "1")
    private Integer sort;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=未删除，1=已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
