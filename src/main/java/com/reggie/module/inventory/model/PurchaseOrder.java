package com.reggie.module.inventory.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购订单
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("purchase_order")
@Schema(description = "采购订单")
public class PurchaseOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "采购订单ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "采购单号", example = "PO20260709001")
    private String orderNo;

    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    @Schema(description = "总金额（元）", example = "500.00")
    private BigDecimal totalAmount;

    @Schema(description = "状态：PENDING=待审核，APPROVED=已审核，COMPLETED=已完成，CANCELLED=已取消", example = "PENDING")
    private String status;

    @Schema(description = "操作员", example = "张三")
    private String operator;

    @Schema(description = "备注", example = "紧急采购")
    private String remark;

    @Schema(description = "创建时间", example = "2026-07-09 10:00:00")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "更新时间", example = "2026-07-09 12:00:00")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 供应商名称（关联查询填充，数据库无此列） */
    @TableField(exist = false)
    private String supplierName;

    @Schema(description = "逻辑删除：0=未删除，1=已删除")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;

    /**
     * 乐观锁版本号：保护 status（状态机流转）与 totalAmount（金额）的并发更新。
     * 采购单审核/取消/完成等状态流转场景必须走 MP 的 updateById/update(entity, wrapper) 携带 version 条件，
     * 防止并发审核/取消导致状态跳变或总金额漂移。
     * 数据库列 version 默认值 0，由 OptimisticLockerInnerInterceptor 在 update 时自动 +1。
     */
    @Version
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;
}
