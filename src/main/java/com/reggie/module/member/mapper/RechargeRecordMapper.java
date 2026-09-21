package com.reggie.module.member.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.member.model.RechargeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 充值记录 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface RechargeRecordMapper extends BaseMapper<RechargeRecord> {

    /**
     * 门店确认到账（单向状态机 CAS）：PENDING → SUCCESS。
     * 仅当当前状态为 PENDING 时生效，affected=0 说明已被确认/取消，调用方据此幂等拦截，杜绝重复入账。
     * tenant_id 由 TenantLineInnerInterceptor 自动注入，无需手动拼接。
     *
     * @param id         充值记录ID
     * @param employeeId 确认操作员工ID
     * @return 受影响行数，1=确认成功，0=状态非待确认（已处理）
     */
    @Update("UPDATE recharge_record SET status = 'SUCCESS', confirm_employee_id = #{employeeId}, " +
            "confirm_time = NOW(), update_time = NOW() " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int casConfirm(@Param("id") Long id, @Param("employeeId") Long employeeId);
}
