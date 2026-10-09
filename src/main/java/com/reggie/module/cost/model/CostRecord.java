package com.reggie.module.cost.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
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
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人")
    private Long createUser;

    /**
     * 乐观锁版本号：保护 amount（成本金额）的并发更新，防止重复记账/并发修改导致金额漂移。
     * 更新场景必须走 MP 的 updateById/update(entity, wrapper) 以携带 version 条件，
     * 数据库列 version 默认值 0，由 OptimisticLockerInnerInterceptor 在 update 时自动 +1。
     */
    @Version
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;
}
