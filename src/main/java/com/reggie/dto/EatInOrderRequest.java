package com.reggie.dto;

import com.reggie.module.order.model.OrderDetail;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * <p>
 * 堂食扫码下单请求DTO
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Data
@Schema(description = "堂食扫码下单请求")
public class EatInOrderRequest {

    @NotNull(message = "订单信息不能为空")
    @Schema(description = "订单基本信息", required = true)
    private OrderInfo order;

    @NotEmpty(message = "订单明细不能为空")
    @Schema(description = "订单明细列表", required = true)
    private List<OrderDetail> orderDetails;

    @Schema(description = "订单信息")
    @Data
    public static class OrderInfo {
        @NotNull(message = "桌台ID不能为空")
        @Schema(description = "桌台ID", required = true, example = "1")
        private Long tableId;

        @Schema(description = "桌台名称", example = "A01")
        private String tableName;

        @Schema(description = "联系人姓名", example = "张三")
        private String userName;

        @Schema(description = "手机号", example = "13800138000")
        private String phone;

        @Schema(description = "备注", example = "少放辣")
        private String remark;

        @Schema(description = "支付方式：1=微信，2=支付宝", example = "1")
        private Integer payMethod;

        @Schema(description = "用餐人数", example = "4")
        private Integer customerCount;
    }
}

