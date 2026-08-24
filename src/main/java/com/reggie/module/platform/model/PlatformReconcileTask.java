package com.reggie.module.platform.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 平台对账任务
 * <p>
 * 记录每次对账任务的执行结果，包括匹配/差异统计。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Data
@TableName("platform_reconcile_task")
public class PlatformReconcileTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String platformType;

    private LocalDate reconcileDate;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

    /** 平台侧订单数 */
    private Integer totalPlatformCount;

    /** 本地订单数 */
    private Integer totalLocalCount;

    /** 匹配成功数 */
    private Integer matchCount;

    /** 平台有本地无（差异：少单） */
    private Integer missingLocalCount;

    /** 本地有平台无（差异：多单） */
    private Integer missingPlatformCount;

    /** 状态：0=进行中 1=完成 2=失败 */
    private Integer status;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
