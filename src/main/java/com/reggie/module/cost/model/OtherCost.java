package com.reggie.module.cost.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 其他成本实体
 *
 * @author reggie
 * @since 2026-08-10
 */
@Data
@TableName("other_cost")
@Schema(description = "其他成本")
public class OtherCost implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "成本名称")
    private String name;

    @Schema(description = "成本类型：1-租金，2-水电，3-设备，4-耗材，5-营销，6-其他")
    private Integer costType;

    @Schema(description = "成本金额")
    private BigDecimal amount;

    @Schema(description = "成本日期")
    private LocalDateTime costDate;

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
