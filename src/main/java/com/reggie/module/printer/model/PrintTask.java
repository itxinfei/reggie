package com.reggie.module.printer.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印任务（代理领取执行）
 *
 * <p>后端构建打印内容入队（PENDING），门店 PC 打印代理心跳时拉取（PULLED）并调用
 * 本地打印机，执行后回执 SUCCESS/FAILED。用于替代"服务器直连打印机"旧模型。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Data
@TableName("print_task")
@Schema(description = "打印任务队列")
public class PrintTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态：待领取 */
    public static final String STATUS_PENDING = "PENDING";

    /** 状态：已领取（代理打印中） */
    public static final String STATUS_PULLED = "PULLED";

    /** 状态：成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";

    /** 状态：失败 */
    public static final String STATUS_FAILED = "FAILED";

    /** 状态：已取消 */
    public static final String STATUS_CANCELLED = "CANCELLED";

    @Schema(description = "任务ID", example = "1")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "门店租户ID", example = "2")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "门店编码", example = "S0001")
    private String storeCode;

    @Schema(description = "关联订单ID（测试任务为空）", example = "10086")
    private Long orderId;

    @Schema(description = "任务类型：BILL/KITCHEN/DELIVERY/TEST", example = "BILL")
    private String taskType;

    @Schema(description = "打印内容 JSON（PrintLine 数组，由 PrinterTemplate 构建）")
    private String content;

    @Schema(description = "状态：PENDING/PULLED/SUCCESS/FAILED/CANCELLED", example = "PENDING")
    private String status;

    @Schema(description = "派发终端ID")
    private Long terminalId;

    @Schema(description = "派发终端编码")
    private String terminalCode;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "重试次数")
    private Integer retryCount;

    @Schema(description = "创建时间")
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @Schema(description = "代理领取时间")
    private LocalDateTime pulledTime;

    @Schema(description = "完成时间")
    private LocalDateTime doneTime;
}
