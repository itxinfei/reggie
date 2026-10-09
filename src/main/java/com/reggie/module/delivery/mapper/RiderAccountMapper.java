package com.reggie.module.delivery.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.delivery.model.RiderAccount;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 骑手账户 Mapper。
 *
 * @author reggie
 * @since 2026-09-28
 */
@Mapper
public interface RiderAccountMapper extends BaseMapper<RiderAccount> {

    /**
     * 按骑手 + 租户查询账户。
     */
    @Select("SELECT * FROM rider_account WHERE rider_id = #{riderId} AND tenant_id = #{tenantId} LIMIT 1")
    RiderAccount selectByRiderTenant(@Param("riderId") Long riderId, @Param("tenantId") Long tenantId);

    /**
     * 入账（原子 upsert）：首次插入，后续累加可提现余额与累计收入。
     */
    @Insert("INSERT INTO rider_account (rider_id, tenant_id, withdrawable_balance, frozen_balance, "
            + "total_income, total_withdrawn, version, create_time, update_time) "
            + "VALUES (#{riderId}, #{tenantId}, #{amount}, 0, #{amount}, 0, 0, NOW(), NOW()) "
            + "ON DUPLICATE KEY UPDATE withdrawable_balance = withdrawable_balance + #{amount}, "
            + "total_income = total_income + #{amount}, update_time = NOW()")
    int upsertBalance(@Param("riderId") Long riderId, @Param("tenantId") Long tenantId,
                      @Param("amount") BigDecimal amount);

    /**
     * 申请提现：冻结可提现金额（WHERE 余额充足才扣，防超扣）。
     * @return 受影响行数，0 表示余额不足
     */
    @Update("UPDATE rider_account SET withdrawable_balance = withdrawable_balance - #{amount}, "
            + "frozen_balance = frozen_balance + #{amount}, update_time = NOW() "
            + "WHERE rider_id = #{riderId} AND tenant_id = #{tenantId} AND withdrawable_balance >= #{amount}")
    int freeze(@Param("riderId") Long riderId, @Param("tenantId") Long tenantId,
               @Param("amount") BigDecimal amount);

    /**
     * 驳回提现：冻结金额退回可提现。
     */
    @Update("UPDATE rider_account SET frozen_balance = frozen_balance - #{amount}, "
            + "withdrawable_balance = withdrawable_balance + #{amount}, update_time = NOW() "
            + "WHERE rider_id = #{riderId} AND tenant_id = #{tenantId}")
    int returnFrozen(@Param("riderId") Long riderId, @Param("tenantId") Long tenantId,
                     @Param("amount") BigDecimal amount);

    /**
     * 通过提现：释放冻结并计入已提现。
     */
    @Update("UPDATE rider_account SET frozen_balance = frozen_balance - #{amount}, "
            + "total_withdrawn = total_withdrawn + #{amount}, update_time = NOW() "
            + "WHERE rider_id = #{riderId} AND tenant_id = #{tenantId}")
    int clearFrozen(@Param("riderId") Long riderId, @Param("tenantId") Long tenantId,
                    @Param("amount") BigDecimal amount);
}
