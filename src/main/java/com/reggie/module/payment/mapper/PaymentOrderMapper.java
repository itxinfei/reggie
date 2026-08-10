package com.reggie.module.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.payment.model.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 支付单 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    /**
     * 支付回调专用：按交易号查询支付订单（忽略租户拦截）。
     * 支付回调为外部请求无登录态/租户上下文，fail-closed 策略下普通查询会返回空，
     * 故用 @InterceptorIgnore 跳过租户过滤，由调用方查到订单后设置其 tenantId 上下文再处理。
     * 注意：原生注解 SQL 不会自动应用 @TableLogic 逻辑删除过滤，需手动添加 is_deleted = 0。
     *
     * @param tradeNo 交易号
     * @return 支付订单
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM payment_order WHERE trade_no = #{tradeNo} AND is_deleted = 0 LIMIT 1")
    PaymentOrder selectByTradeNoIgnoreTenant(@Param("tradeNo") String tradeNo);

    /**
     * 幂等原子更新：仅当支付单状态为期望旧状态时才更新为新状态，解决回调 read-then-write 竞态。
     * 返回受影响行数：1=本次处理成功（首次），0=状态已被他人变更（幂等跳过）。
     * 注意：原生注解 SQL 不会自动应用 @TableLogic 逻辑删除过滤，需手动添加 is_deleted = 0。
     *
     * @param tradeNo         交易号
     * @param expectStatus    期望的旧状态
     * @param newStatus       新状态
     * @param channelTradeNo  渠道交易号
     * @param paidTime        支付时间
     * @return 受影响行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE payment_order SET status = #{newStatus}, channel_trade_no = #{channelTradeNo}, " +
            "paid_time = #{paidTime}, update_time = #{paidTime} " +
            "WHERE trade_no = #{tradeNo} AND status = #{expectStatus} AND is_deleted = 0")
    int casUpdateStatus(@Param("tradeNo") String tradeNo,
                        @Param("expectStatus") String expectStatus,
                        @Param("newStatus") String newStatus,
                        @Param("channelTradeNo") String channelTradeNo,
                        @Param("paidTime") LocalDateTime paidTime);

    /**
     * 按订单ID查询是否存在指定状态的支付单（用于重复支付检查，忽略租户拦截）。
     * 注意：原生注解 SQL 不会自动应用 @TableLogic 逻辑删除过滤，需手动添加 is_deleted = 0，
     * 否则已删除的 SUCCESS 支付单会错误阻止用户重新支付。
     *
     * @param orderId 业务订单ID
     * @param statuses 状态集合（逗号分隔，已拼接为 IN 参数）
     * @return 数量
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM payment_order WHERE order_id = #{orderId} AND status IN (${statuses}) AND is_deleted = 0")
    int countByOrderIdAndStatuses(@Param("orderId") Long orderId, @Param("statuses") String statuses);

    /**
     * 查询某支付单已成功退款的总金额（用于退款累计超额校验，忽略租户拦截）。
     * 注意：原生注解 SQL 不会自动应用 @TableLogic 逻辑删除过滤，需手动添加 is_deleted = 0，
     * 否则已删除的退款记录金额会被错误计入累计退款额。
     *
     * @param paymentOrderId 支付单ID
     * @param successCode    退款成功状态码
     * @return 已退款总额
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COALESCE(SUM(amount),0) FROM refund_record WHERE payment_order_id = #{paymentOrderId} AND status = #{successCode} AND is_deleted = 0")
    BigDecimal sumRefundedAmount(@Param("paymentOrderId") Long paymentOrderId, @Param("successCode") String successCode);
}