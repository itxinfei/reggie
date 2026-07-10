package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作审计日志
 * 记录所有数据变更操作的操作人、时间、IP、操作类型、变更内容
 */
@Data
@TableName("operation_log")
@Schema(description = "操作审计日志实体")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "日志ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "操作人ID", example = "1")
    private Long operatorId;

    @Schema(description = "操作人姓名", example = "张三")
    private String operatorName;

    @Schema(description = "操作人IP", example = "192.168.1.100")
    private String operatorIp;

    @Schema(description = "操作模块", example = "菜品管理")
    private String module;

    @Schema(description = "操作类型：INSERT/UPDATE/DELETE/OTHER", example = "UPDATE")
    private String operationType;

    @Schema(description = "业务表名", example = "dish")
    private String tableName;

    @Schema(description = "业务记录ID", example = "1")
    private Long bizId;

    @Schema(description = "操作描述", example = "修改菜品价格")
    private String description;

    @Schema(description = "变更前的值（JSON格式）")
    private String oldValue;

    @Schema(description = "变更后的值（JSON格式）")
    private String newValue;

    @Schema(description = "请求URL", example = "/dish/1")
    private String requestUrl;

    @Schema(description = "请求方法", example = "PUT")
    private String requestMethod;

    @Schema(description = "请求参数（JSON格式）")
    private String requestParams;

    @Schema(description = "执行时长（毫秒）", example = "50")
    private Long duration;

    @Schema(description = "是否成功：0=失败，1=成功", example = "1")
    private Integer isSuccess;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "是否删除：0=否，1=是")
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
