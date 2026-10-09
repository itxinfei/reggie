package com.reggie.module.platform.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 平台同步操作日志
 * <p>
 * 记录每次与外卖平台交互的请求/响应，用于对账和异常追踪。
 * </p>
 *
 * @author reggie
 * @since 2026-08-24
 */
@Data
@TableName("platform_sync_log")
public class PlatformSyncLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    /** 平台类型：MEITUAN/ELEME/DOUYIN/SELF/OTHER */
    private String platformType;

    /** 平台订单号 */
    private String platformOrderId;

    /** 本地订单ID */
    private Long localOrderId;

    /** 动作类型 */
    private String action;

    /** 方向：IN=拉单 OUT=回传 */
    private String direction;

    /** 请求内容（脱敏） */
    private String requestBody;

    /** 响应内容 */
    private String responseBody;

    /** 结果：0=成功 1=失败 */
    private Integer status;

    /** 错误信息 */
    private String errorMessage;

    /** 重试次数 */
    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
