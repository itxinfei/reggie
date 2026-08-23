package com.reggie.module.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 * 键值对存储系统级配置，支持多租户隔离
 */
@Data
@TableName("system_config")
@Schema(description = "系统配置实体")
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

    @Schema(description = "配置ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID（NULL表示全局配置）", example = "1")
    private Long tenantId;

    @Schema(description = "配置键", example = "order.auto_cancel_minutes", required = true)
    private String configKey;

    @Schema(description = "配置值", example = "30", required = true)
    private String configValue;

    @Schema(description = "配置类型：1=功能开关，2=运营参数，3=显示设置，4=其他", example = "1")
    private Integer configType;

    @Schema(description = "配置说明", example = "订单自动取消时间（分钟）")
    private String description;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    private Long createUser;

    @Schema(description = "修改人ID")
    private Long updateUser;
}
