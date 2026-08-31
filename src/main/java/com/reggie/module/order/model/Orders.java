package com.reggie.module.order.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单
 */
@Data
@TableName("orders")
@Schema(description = "订单")
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

    @Schema(description = "下单时间", example = "2024-01-01 12:30:00")
    private LocalDateTime orderTime;

    @Schema(description = "结账时间", example = "2024-01-01 13:00:00")
    private LocalDateTime checkoutTime;

    @Schema(description = "支付方式：1=现金，2=微信，3=支付宝，4=银行卡，5=会员储值，6=货到付款", example = "1")
    private Integer payMethod;

    @Schema(description = "实收金额", example = "88.00", required = true)
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.0", inclusive = false, message = "订单金额必须大于0")
    private BigDecimal amount;

    @Schema(description = "配送费（外卖单独立存储，堂食为0）", example = "5.00")
    @TableField("delivery_fee")
    private BigDecimal deliveryFee;

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

    @Schema(description = "桌台名称（非数据库字段，冗余展示）", example = "A01")
    @TableField(exist = false)
    private String tableName;

    @Schema(description = "用餐人数（非数据库字段）", example = "4")
    @TableField(exist = false)
    private Integer customerCount;

    @Schema(description = "创建时间", example = "2024-01-01 12:30:00")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2024-01-01 13:00:00")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @Schema(description = "修改人ID", example = "1")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

    @Schema(description = "是否删除：0=否，1=是", example = "0")
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;

    @Schema(description = "租户ID", example = "1")
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @Schema(description = "幂等令牌（防止重复下单）", example = "uuid-xxxx-xxxx")
    private String idempotencyKey;

    /** 平台类型：MEITUAN / ELEME / DOUYIN / SELF / OTHER，平台订单使用 */
    @Schema(description = "平台来源：MEITUAN/ELEME/DOUYIN/SELF/OTHER", example = "MEITUAN")
    @TableField("platform_type")
    private String platformType;

    /** 平台订单号（唯一键，用于去重和状态回传） */
    @Schema(description = "平台订单号（各平台原始订单号，用于去重）", example = "MT202608240001")
    @TableField("platform_order_id")
    private String platformOrderId;

    /** 平台侧门店 ID */
    @Schema(description = "平台侧门店ID", example = "shop_001")
    @TableField("platform_shop_id")
    private String platformShopId;

    /** 平台原始订单 JSON（便于排查与字段补全） */
    @Schema(description = "平台原始订单JSON(用于排查)")
    @TableField("platform_raw")
    private String platformRaw;

    @Schema(description = "库存是否已回退：0=否，1=是", example = "0")
    @TableField("stock_refunded")
    private Integer stockRefunded;

    @Schema(description = "本单使用的优惠券ID（用户优惠券记录ID），未使用为 null", example = "1")
    @TableField("used_coupon_id")
    private Long usedCouponId;

    @Schema(description = "乐观锁版本号")
    @Version
    private Integer version;

    /** 父订单 ID：AA 分账时指向主订单，子单为 null */
    @Schema(description = "父订单ID（AA 分账时指向主订单）", example = "100")
    @TableField("master_order_id")
    private Long masterOrderId;

    /** 分账份数：AA 分账后主单记录拆分子单数量 */
    @Schema(description = "分账份数（AA 分账时记录拆分数量）", example = "3")
    @TableField("split_count")
    private Integer splitCount;
}

