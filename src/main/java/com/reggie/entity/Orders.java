package com.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
public class Orders implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    //订单号
    private String number;

    public static final int STATUS_PENDING_PAY = 1;
    public static final int STATUS_ORDERED = 2;
    public static final int STATUS_DELIVERING = 3;
    public static final int STATUS_COMPLETED = 4;
    public static final int STATUS_CANCELLED = 5;
    public static final int STATUS_REFUNDED = 6;

    //订单状态 1待付款，2待接单，3已接单，4派送中，5已完成，6已取消
    @NotNull(message = "订单状态不能为空")
    private Integer status;


    //下单用户id
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    //地址id
    @NotNull(message = "地址ID不能为空")
    private Long addressBookId;


    //下单时间
    private LocalDateTime orderTime;


    //结账时间
    private LocalDateTime checkoutTime;


    //支付方式 1微信，2支付宝
    private Integer payMethod;


    //实收金额
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "订单金额必须大于0")
    private BigDecimal amount;

    //备注
    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;

    //预计送达时间
    @Size(max = 20, message = "送达时间格式不正确")
    private String expectDeliveryTime;

    //用户名
    @NotBlank(message = "用户名不能为空")
    @Size(max = 30, message = "用户名不能超过30个字符")
    private String userName;

    //手机号
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    //地址
    @NotBlank(message = "收货地址不能为空")
    @Size(max = 200, message = "收货地址不能超过200个字符")
    private String address;

    //收货人
    @NotBlank(message = "收货人不能为空")
    @Size(max = 30, message = "收货人姓名不能超过30个字符")
    private String consignee;

    //桌号
    private Long tableId;

    //就餐方式
    @Size(max = 20, message = "就餐方式不能超过20个字符")
    private String diningType;


    //创建时间
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


    //更新时间
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;


    //创建人
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;


    //修改人
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;


    //是否删除
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;

    //租户id
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
