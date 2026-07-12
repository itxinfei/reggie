package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@Schema(description = "订单实体")
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "订单ID", example = "1")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "订单号", example = "202607100001")
    private String number;

    /** 待付款 */
    public static final int STATUS_PENDING_PAY = 1;
    /** 待接单/处理中 */
    public static final int STATUS_ORDERED = 2;
    /** 已接单/派送中 */
    public static final int STATUS_DELIVERING = 3;
    /** 已完成 */
    public static final int STATUS_COMPLETED = 4;
    /** 已取消 */
    public static final int STATUS_CANCELLED = 5;
    /** 已退款 */
    public static final int STATUS_REFUNDED = 6;

    @Schema(description = "订单状态：1=待付款，2=待接单，3=派送中，4=已完成，5=已取消，6=已退款", example = "1", required = true)
    @NotNull(message = "订单状态不能为空")
    private Integer status;

    @Schema(description = "下单用户ID", example = "1", required = true)
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "收货地址ID", example = "1", required = true)
    @NotNull(message = "地址ID不能为空")
    private Long addressBookId;

    @Schema(description = "下单时间")
    private LocalDateTime orderTime;

    @Schema(description = "结账时间")
    private LocalDateTime checkoutTime;

    @Schema(description = "支付方式：1=微信，2=支付宝", example = "1")
    private Integer payMethod;

    @Schema(description = "实收金额", example = "88.00", required = true)
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "订单金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "备注", example = "少放辣")
    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;

    @Schema(description = "预计送达时间", example = "30分钟内")
    @Size(max = 20, message = "送达时间格式不正确")
    private String expectDeliveryTime;

    @Schema(description = "用户名", example = "张三", required = true)
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名不能超过30个字符")
    private String userName;

    @Schema(description = "手机号", example = "13800138000", required = true)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "收货地址", example = "北京市朝阳区xxx", required = true)
    @NotBlank(message = "收货地址不能为空")
    @Size(max = 200, message = "收货地址不能超过200个字符")
    private String address;

    @Schema(description = "收货人", example = "张三", required = true)
    @NotBlank(message = "收货人不能为空")
    @Size(max = 30, message = "收货人姓名不能超过30个字符")
    private String consignee;

    @Schema(description = "就餐方式：TAKEOUT=外卖配送，EAT_IN=堂食扫码，QUEUE=排队，RESERVATION=预订", example = "TAKEOUT")
    @TableField("dining_type")
    @Size(max = 20, message = "就餐方式不能超过20个字符")
    private String source;

    @Schema(description = "桌台ID（堂食/排队/预订使用）", example = "1")
    private Long tableId;

    @Schema(description = "桌台名称（冗余，便于展示）", example = "A01")
    private String tableName;

    @Schema(description = "排队记录ID（排队场景）", example = "1")
    private Long queueId;

    @Schema(description = "预订记录ID（预订场景）", example = "1")
    private Long reservationId;

    @Schema(description = "用餐人数", example = "4")
    private Integer customerCount;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @Schema(description = "是否删除：0=否，1=是")
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "幂等令牌（防止重复下单）", example = "uuid-xxxx-xxxx")
    private String idempotencyKey;
}
