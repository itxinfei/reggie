package com.reggie.module.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.marketing.model.FlashSale;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * Flash Sale Mapper
 *
 * @author reggie
 * @since 2026-08-11
 */
@Mapper
public interface FlashSaleMapper extends BaseMapper<FlashSale> {

    /**
     * 原子扣减秒杀库存：仅当剩余库存 (total_quantity - sold_quantity) >= qty 时才扣减
     * 利用数据库行级锁保证并发安全，防止超卖
     *
     * @param flashSaleId 秒杀活动ID
     * @param qty         购买数量
     * @return 受影响行数（1=扣减成功，0=库存不足）
     */
    @Update("UPDATE flash_sale SET sold_quantity = sold_quantity + #{qty} " +
            "WHERE id = #{flashSaleId} AND (total_quantity - sold_quantity) >= #{qty}")
    int deductStock(@Param("flashSaleId") Long flashSaleId, @Param("qty") int qty);

    /**
     * 原子回退秒杀库存（订单取消/退款时释放名额）。
     * <p>
     * 与 deductStock 对称，但<b>额外要求 sold_quantity &gt;= qty</b>：
     * 回退量不应超过已售量，否则会把 sold_quantity 压成负数，导致
     * (total_quantity - sold_quantity) &gt; total_quantity，库存凭空变多。
     * </p>
     * <p>返回值：1=回退成功，0=无需回退（额度不足/记录不存在）。调用方按 0 视为"未回退"并记录日志。</p>
     *
     * @param flashSaleId 秒杀活动ID
     * @param qty         回退数量
     * @return 受影响行数
     */
    @Update("UPDATE flash_sale SET sold_quantity = sold_quantity - #{qty} " +
            "WHERE id = #{flashSaleId} AND sold_quantity >= #{qty}")
    int revertStock(@Param("flashSaleId") Long flashSaleId, @Param("qty") int qty);

    /**
     * 删除用户在某秒杀活动上的限购占用记录（订单取消/退款时释放限购额度）。
     * <p>
     * 与 sumPurchasedQuantity 保持同一口径（rule_type = 3 即秒杀活动），
     * 保证"释放后重新购买不受限购拦截"。
     * </p>
     *
     * @param flashSaleId 秒杀活动ID
     * @param userId      用户ID
     * @param tenantId    租户ID
     * @return 删除行数
     */
    @Delete("DELETE FROM campaign_usage_record " +
            "WHERE campaign_id = #{flashSaleId} AND rule_type = 3 " +
            "AND user_id = #{userId} AND tenant_id = #{tenantId}")
    int deleteUsageRecord(@Param("flashSaleId") Long flashSaleId, @Param("userId") Long userId,
            @Param("tenantId") Long tenantId);

    /**
     * 统计用户在某秒杀活动上已占用限购的购买件数。
     * <p>关联订单排除已取消(5)、已退款(6)——交易未成立/已逆转不占限购；
     * 待付款/待接单/派送中/已完成均占用，与"扣了秒杀库存即占额度"一致。</p>
     *
     * @param flashSaleId 秒杀活动ID
     * @param userId      用户ID
     * @param tenantId    租户ID
     * @return 已购件数（无记录为 0）
     */
    @Select("SELECT COALESCE(SUM(cur.quantity), 0) " +
            "FROM campaign_usage_record cur " +
            "LEFT JOIN orders o ON cur.order_id = o.id " +
            "WHERE cur.campaign_id = #{flashSaleId} AND cur.rule_type = 3 " +
            "AND cur.user_id = #{userId} AND cur.tenant_id = #{tenantId} " +
            "AND (o.id IS NULL OR o.status NOT IN (5, 6))")
    int sumPurchasedQuantity(@Param("flashSaleId") Long flashSaleId, @Param("userId") Long userId,
            @Param("tenantId") Long tenantId);
}
