package com.reggie.module.sys.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 * 键值对存储系统级配置，支持多租户隔离
 */
@Data
@TableName("system_config")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置类型：功能开关 */
    public static final int TYPE_FEATURE = 1;
    /** 配置类型：运营参数 */
    public static final int TYPE_OPERATION = 2;
    /** 配置类型：显示设置 */
    public static final int TYPE_DISPLAY = 3;
    /** 配置类型：其他 */
    public static final int TYPE_OTHER = 4;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 租户ID，NULL表示全局配置 */
    private Long tenantId;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置类型 */
    private Integer configType;

    /** 配置说明 */
    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}
