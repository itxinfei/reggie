package com.reggie.module.store.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 门店配置
 * 门店级别的功能开关和运营参数
 *
 * @author reggie
 * @since 2026-07-09
 */
@Data
@TableName("store_config")
public class StoreConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置类型 - 功能开关 */
    public static final int CONFIG_TYPE_SWITCH = 1;
    /** 配置类型 - 运营参数 */
    public static final int CONFIG_TYPE_OPERATION = 2;
    /** 配置类型 - 显示设置 */
    public static final int CONFIG_TYPE_DISPLAY = 3;
    /** 配置类型 - 其他 */
    public static final int CONFIG_TYPE_OTHER = 4;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 门店ID */
    private Long tenantId;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置类型 */
    private Integer configType;

    /** 配置说明 */
    private String description;

    /** 创建人(总部管理员) */
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标识 0:未删除 1:已删除 */
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted;
}
