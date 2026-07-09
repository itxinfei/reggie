package com.reggie.entity;

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
 * 操作审计日志
 * 记录所有数据变更操作的操作人、时间、IP、操作类型、变更内容
 */
@Data
@TableName("operation_log")
public class OperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人姓名
     */
    private String operatorName;

    /**
     * 操作人IP
     */
    private String operatorIp;

    /**
     * 操作模块（如：菜品管理、订单管理、员工管理）
     */
    private String module;

    /**
     * 操作类型：INSERT/UPDATE/DELETE/OTHER
     */
    private String operationType;

    /**
     * 业务表名
     */
    private String tableName;

    /**
     * 业务记录ID
     */
    private Long bizId;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 变更前的值（JSON格式）
     */
    private String oldValue;

    /**
     * 变更后的值（JSON格式）
     */
    private String newValue;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法（GET/POST/PUT/DELETE）
     */
    private String requestMethod;

    /**
     * 请求参数（JSON格式）
     */
    private String requestParams;

    /**
     * 执行时长（毫秒）
     */
    private Long duration;

    /**
     * 是否成功：0失败 1成功
     */
    private Integer isSuccess;

    /**
     * 错误信息
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 是否删除（逻辑删除）
     */
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;
}
