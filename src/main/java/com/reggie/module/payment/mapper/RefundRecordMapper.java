package com.reggie.module.payment.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.payment.model.RefundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 * 退款记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface RefundRecordMapper extends BaseMapper<RefundRecord> {

    /**
     * 按退款单号跨租户查询退款记录（退款异步回调场景专用）。
     * <p>
     * 回调无登录态、无租户 ThreadLocal，且需在"选对租户配置完成验签"后据此定位退款单，
     * 故显式忽略租户插件、不加 tenant_id 条件。调用方必须已完成渠道验签，
     * 并在后续复用租户内 service 方法前由本记录的 tenantId 设置 BaseContext。
     * </p>
     *
     * @param refundNo 退款单号（即渠道 out_refund_no / out_request_no）
     * @return 退款记录；不存在返回 null
     */
    // 同步成功的售后流程会以同一 refund_no 存「售后行(有 order_id)」与「资金行(order_id 为空)」两行，
    // 而 PROCESSING 路径仅有一行。ORDER BY 优先取处于 processing 的行，保证回调定位到待终态化的记录。
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM refund_record WHERE refund_no = #{refundNo} AND is_deleted = 0 "
            + "ORDER BY (status = 'processing') DESC, id DESC LIMIT 1")
    RefundRecord selectByRefundNoIgnoreTenant(@Param("refundNo") String refundNo);
}
