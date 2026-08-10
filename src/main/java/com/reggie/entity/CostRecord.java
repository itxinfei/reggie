package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本记录实体
 *
 * @author reggie
 * @since 2026-08-10
 */
@Data
@TableName("cost_record")
@Schema(description = "成本记录")
public class CostRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "成本类型：1-食材成本，2-人工成本，3-其他成本")
    private Integer costType;

    @Schema(description = "关联ID（菜品ID/员工ID等）")
    private Long refId;

    @Schema(description = "关联名称")
    private String refName;

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

    @Schema(description = "创建人")
    private Long createUser;
}
