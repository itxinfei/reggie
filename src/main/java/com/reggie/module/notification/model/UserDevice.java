package com.reggie.module.notification.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户设备实体
 * APP推送需要设备Token，支持Android/iOS/H5多端
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("user_device")
public class UserDevice implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 用户ID */
    private Long userId;

    /** 平台: ANDROID/IOS/H5 */
    private String platform;

    /** 设备推送Token */
    private String deviceToken;

    /** APP版本号 */
    private String appVersion;

    /** 是否开启推送: 1=是, 0=否 */
    private Integer pushEnabled;

    /** 最后活跃时间 */
    private LocalDateTime lastActiveTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0=未删除，1=已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
