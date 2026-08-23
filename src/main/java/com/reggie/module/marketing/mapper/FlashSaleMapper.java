package com.reggie.module.marketing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.marketing.model.FlashSale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
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
            "WHERE id = #{flashSaleId} AND (total_quantity - sold_quantity) >= #{qty} AND is_deleted = 0")
    int deductStock(@Param("flashSaleId") Long flashSaleId, @Param("qty") int qty);
}
