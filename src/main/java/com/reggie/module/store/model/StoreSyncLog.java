package com.reggie.module.store.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店同步日志
 * 记录总部向分店同步菜品、分类、套餐、配置等数据的操作日志
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("store_sync_log")
public class StoreSyncLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 同步类型 - 菜品同步 */
    public static final int SYNC_TYPE_DISH = 1;
    /** 同步类型 - 分类同步 */
    public static final int SYNC_TYPE_CATEGORY = 2;
    /** 同步类型 - 套餐同步 */
    public static final int SYNC_TYPE_SETMEAL = 3;
    /** 同步类型 - 配置同步 */
    public static final int SYNC_TYPE_CONFIG = 4;
    /** 同步类型 - 优惠券同步 */
    public static final int SYNC_TYPE_COUPON = 5;

    /** 同步模式 - 全量同步 */
    public static final int SYNC_MODE_FULL = 1;
    /** 同步模式 - 增量同步 */
    public static final int SYNC_MODE_INCREMENTAL = 2;
    /** 同步模式 - 选择性同步 */
    public static final int SYNC_MODE_SELECTIVE = 3;

    /** 同步状态 - 进行中 */
    public static final int STATUS_IN_PROGRESS = 0;
    /** 同步状态 - 成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 同步状态 - 失败 */
    public static final int STATUS_FAILED = 2;
    /** 同步状态 - 部分成功 */
    public static final int STATUS_PARTIAL = 3;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 来源门店ID */
    private Long sourceTenantId;

    /** 目标门店ID */
    private Long targetTenantId;

    /** 同步类型 */
    private Integer syncType;

    /** 同步模式 */
    private Integer syncMode;

    /** 同步状态 */
    private Integer syncStatus;

    /** 同步数量 */
    private Integer syncCount;

    /** 失败数量 */
    private Integer failCount;

    /** 错误详情 */
    private String errorDetail;

    /** 操作人ID */
    private Long operatorId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;
}
