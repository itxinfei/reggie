package com.reggie.module.subsidy.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 部门对账聚合 Mapper（企业内部订餐）
 * <p>两个聚合都以「用户 → user.department_id」为分组维度：
 * ① 有效订单（待接单/配送中/已完成，排除待付款/已取消/已退款）金额；
 * ② 餐补流水（发放 GRANT / 核销 CONSUME）金额。</p>
 */
@Mapper
public interface SubsidyStatMapper {

    /**
     * 按部门聚合有效订单数与金额（未挂部门用户 departmentId 为 null，归入"未分配"）。
     *
     * @return 每行: departmentId, orderCount, orderAmount
     */
    @Select("SELECT u.department_id AS departmentId, COUNT(*) AS orderCount, "
            + "IFNULL(SUM(o.amount), 0) AS orderAmount "
            + "FROM orders o JOIN user u ON u.id = o.user_id AND u.tenant_id = o.tenant_id "
            + "WHERE o.tenant_id = #{tenantId} AND o.is_deleted = 0 "
            + "AND o.status IN (2, 3, 4) "
            + "AND o.order_time >= #{start} AND o.order_time < #{end} "
            + "GROUP BY u.department_id")
    List<Map<String, Object>> aggregateOrdersByDepartment(@Param("tenantId") Long tenantId,
                                                          @Param("start") String start,
                                                          @Param("end") String end);

    /**
     * 按部门聚合餐补流水金额（发放/核销两类）。
     *
     * @return 每行: recordType, departmentId, total
     */
    @Select("SELECT r.record_type AS recordType, u.department_id AS departmentId, "
            + "IFNULL(SUM(r.amount), 0) AS total "
            + "FROM meal_subsidy_record r JOIN user u ON u.id = r.user_id AND u.tenant_id = r.tenant_id "
            + "WHERE r.tenant_id = #{tenantId} "
            + "AND r.record_type IN ('GRANT', 'CONSUME') "
            + "AND r.create_time >= #{start} AND r.create_time < #{end} "
            + "GROUP BY r.record_type, u.department_id")
    List<Map<String, Object>> aggregateRecordsByDepartment(@Param("tenantId") Long tenantId,
                                                           @Param("start") String start,
                                                           @Param("end") String end);
}
