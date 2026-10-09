package com.reggie.module.sys.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色权限关联实体
 * 多对多关联：一个角色可拥有多个权限
 */
@Data
@TableName("role_permission")
@Schema(description = "角色权限关联实体")
public class RolePermission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关联ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "角色ID", example = "1", required = true)
    private Long roleId;

    @Schema(description = "权限ID", example = "1", required = true)
    private Long permissionId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
