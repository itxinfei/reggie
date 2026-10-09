package com.reggie.module.subsidy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.subsidy.model.MealSubsidyAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 餐补账户 Mapper
 * <p>余额变动走本类的原子 UPDATE（条件含余额下限/状态），杜绝 read-then-write 竞态；
 * 返回受影响行数，0 表示账户不存在/冻结/余额不足。</p>
 */
@Mapper
public interface MealSubsidyAccountMapper extends BaseMapper<MealSubsidyAccount> {

    /**
     * 发放入账：余额与累计发放同步增加。
     *
     * @return 受影响行数（0=账户不存在或冻结）
     */
    @Update("UPDATE meal_subsidy_account SET balance = balance + #{amount}, "
            + "total_granted = total_granted + #{amount}, update_time = NOW() "
            + "WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND status = 1 AND is_deleted = 0")
    int grantCredit(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
                    @Param("amount") BigDecimal amount);

    /**
     * 核销扣减：余额不足（balance < amount）时原子失败。
     *
     * @return 受影响行数（0=余额不足/账户不存在/冻结）
     */
    @Update("UPDATE meal_subsidy_account SET balance = balance - #{amount}, "
            + "total_used = total_used + #{amount}, update_time = NOW() "
            + "WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND status = 1 AND is_deleted = 0 "
            + "AND balance >= #{amount}")
    int consumeDeduct(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
                      @Param("amount") BigDecimal amount);

    /**
     * 退款回充：余额增加、累计核销回退。
     *
     * @return 受影响行数（0=账户不存在或冻结）
     */
    @Update("UPDATE meal_subsidy_account SET balance = balance + #{amount}, "
            + "total_used = total_used - #{amount}, update_time = NOW() "
            + "WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND status = 1 AND is_deleted = 0")
    int refundCredit(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
                     @Param("amount") BigDecimal amount);

    /** 读取交易后余额（记账后写流水用）。 */
    @Select("SELECT balance FROM meal_subsidy_account "
            + "WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND is_deleted = 0")
    BigDecimal selectBalance(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /**
     * 租户级账户汇总：账户数 / 余额合计 / 累计发放 / 累计核销。
     */
    @Select("SELECT COUNT(*) AS accountCount, IFNULL(SUM(balance), 0) AS totalBalance, "
            + "IFNULL(SUM(total_granted), 0) AS totalGranted, IFNULL(SUM(total_used), 0) AS totalUsed "
            + "FROM meal_subsidy_account WHERE tenant_id = #{tenantId} AND is_deleted = 0")
    java.util.Map<String, Object> selectStats(@Param("tenantId") Long tenantId);
}
