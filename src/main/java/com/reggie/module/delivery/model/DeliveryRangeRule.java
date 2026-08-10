package com.reggie.module.delivery.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配送范围规则实体
 *
 * @author reggie
 * @since 2026-08-11
 */
@Data
@TableName("delivery_range_rule")
@Schema(description = "配送范围规则")
public class DeliveryRangeRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 范围类型 - 圆形 */
    public static final int TYPE_CIRCLE = 1;
    /** 范围类型 - 多边形 */
    public static final int TYPE_POLYGON = 2;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "规则名称")
    @NotNull(message = "规则名称不能为空")
    private String ruleName;

    @Schema(description = "范围类型：1-圆形，2-多边形")
    @NotNull(message = "范围类型不能为空")
    private Integer rangeType;

    @Schema(description = "中心经度（圆形范围）")
    private BigDecimal centerLongitude;

    @Schema(description = "中心纬度（圆形范围）")
    private BigDecimal centerLatitude;

    @Schema(description = "半径（米，圆形范围）")
    private BigDecimal radius;

    @Schema(description = "多边形坐标点JSON（多边形范围）")
    private String polygonPoints;

    @Schema(description = "配送费类型：1-固定，2-距离阶梯，3-时间加价")
    private Integer feeType;

    @Schema(description = "基础配送费")
    private BigDecimal baseFee;

    @Schema(description = "每公里配送费")
    private BigDecimal feePerKm;

    @Schema(description = "最低配送费")
    private BigDecimal minFee;

    @Schema(description = "最高配送费")
    private BigDecimal maxFee;

    @Schema(description = "免费配送金额门槛")
    private BigDecimal freeThreshold;

    @Schema(description = "是否启用：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private Long createUser;

    @Schema(description = "更新人")
    private Long updateUser;
}
