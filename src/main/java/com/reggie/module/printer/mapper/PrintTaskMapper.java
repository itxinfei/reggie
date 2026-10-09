package com.reggie.module.printer.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.reggie.module.printer.model.PrintTask;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 打印任务 Mapper
 *
 * <p>代理端拉取/回执无登录会话，租户上下文为空，统一 {@code @InterceptorIgnore(tenantLine = "true")}
 * 并按 terminal_id 精确匹配（终端归属门店已由注册时 storeCode 解析写入）。</p>
 *
 * @author AI
 * @since 2026-08-30
 */
@Mapper
public interface PrintTaskMapper extends BaseMapper<PrintTask> {

    /**
     * 拉取待处理任务（代理端，按派发终端匹配）
     *
     * @param terminalId 终端ID
     * @param limit      拉取上限
     * @return 待处理任务列表（PENDING）
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM print_task WHERE status = 'PENDING' AND terminal_id = #{terminalId} "
            + "ORDER BY id ASC LIMIT #{limit}")
    List<PrintTask> listPending(@Param("terminalId") Long terminalId, @Param("limit") int limit);

    /**
     * 按主键查询（代理端回执用，绕过租户拦截器）
     *
     * @param id 任务ID
     * @return 任务
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM print_task WHERE id = #{id}")
    PrintTask findByIdIgnoreTenant(@Param("id") Long id);

    /**
     * 插入任务（绕过租户拦截器，显式写入门店租户）。
     * <p>用于测试打印等跨租户派发场景（如总部超管向门店终端派发测试任务），
     * 走 BaseMapper.insert 会被租户拦截器覆盖 tenant_id 为当前会话租户。</p>
     *
     * @param task 任务
     * @return 影响行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT INTO print_task (tenant_id, store_code, order_id, task_type, content, status, "
            + "terminal_id, terminal_code, error_msg, retry_count, created_time, pulled_time, done_time) "
            + "VALUES (COALESCE(#{tenantId}, 0), COALESCE(#{storeCode}, ''), #{orderId}, "
            + "COALESCE(#{taskType}, 'BILL'), #{content}, COALESCE(#{status}, 'PENDING'), #{terminalId}, "
            + "COALESCE(#{terminalCode}, ''), COALESCE(#{errorMsg}, ''), COALESCE(#{retryCount}, 0), "
            + "COALESCE(#{createdTime}, CURRENT_TIMESTAMP), #{pulledTime}, #{doneTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertIgnoreTenant(PrintTask task);

    /**
     * 标记任务为已领取（代理端，条件 UPDATE 防并发重复领取）
     *
     * @param id  任务ID
     * @param now 领取时间
     * @return 更新行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE print_task SET status = 'PULLED', pulled_time = #{now} "
            + "WHERE id = #{id} AND status = 'PENDING'")
    int markPulled(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 回执任务执行结果（代理端）
     *
     * @param id       任务ID
     * @param status   SUCCESS / FAILED
     * @param errorMsg 失败原因（成功为空）
     * @param now      完成时间
     * @return 更新行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE print_task SET status = #{status}, error_msg = #{errorMsg}, done_time = #{now}, "
            + "retry_count = retry_count + 1 WHERE id = #{id}")
    int updateResult(@Param("id") Long id, @Param("status") String status,
                     @Param("errorMsg") String errorMsg, @Param("now") LocalDateTime now);

    /**
     * 任务分页（管理端）：绕过租户拦截器，tenantId 非空时显式过滤（门店员工），
     * 为空时查询全部（总部超管跨门店视角）。
     *
     * @param page     分页
     * @param tenantId 租户ID（可为 null=全部）
     * @param orderId  订单ID
     * @param taskType 任务类型
     * @param status   状态
     * @param beginTime 创建时间起
     * @param endTime   创建时间止
     * @return 分页结果
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT * FROM print_task WHERE 1 = 1 "
            + "<if test='tenantId != null'>AND tenant_id = #{tenantId}</if> "
            + "<if test='orderId != null'>AND order_id = #{orderId}</if> "
            + "<if test='taskType != null and taskType != \"\"'>AND task_type = #{taskType}</if> "
            + "<if test='status != null and status != \"\"'>AND status = #{status}</if> "
            + "<if test='beginTime != null'>AND created_time &gt;= #{beginTime}</if> "
            + "<if test='endTime != null'>AND created_time &lt;= #{endTime}</if> "
            + "ORDER BY id DESC</script>")
    IPage<PrintTask> listPage(IPage<PrintTask> page, @Param("tenantId") Long tenantId,
                              @Param("orderId") Long orderId, @Param("taskType") String taskType,
                              @Param("status") String status, @Param("beginTime") LocalDateTime beginTime,
                              @Param("endTime") LocalDateTime endTime);

    /**
     * 任务统计（管理端）：绕过租户拦截器，tenantId 非空时显式过滤。
     *
     * @param tenantId   租户ID（可为 null=全部）
     * @param todayOnly  仅统计今日（按 created_time）
     * @param today      今日零点
     * @param statusList 状态过滤（可为 null/空=不限制）
     * @return 计数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("<script>SELECT COUNT(*) FROM print_task WHERE 1 = 1 "
            + "<if test='tenantId != null'>AND tenant_id = #{tenantId}</if> "
            + "<if test='todayOnly'>AND created_time &gt;= #{today}</if> "
            + "<if test='statusList != null and statusList.size() > 0'>"
            + "AND status IN <foreach collection='statusList' item='s' open='(' separator=',' close=')'>#{s}</foreach>"
            + "</if></script>")
    long countBy(@Param("tenantId") Long tenantId, @Param("todayOnly") boolean todayOnly,
                 @Param("today") LocalDateTime today, @Param("statusList") List<String> statusList);
}
