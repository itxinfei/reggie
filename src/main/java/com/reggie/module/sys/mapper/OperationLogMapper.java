package com.reggie.module.sys.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.reggie.module.sys.model.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志 Mapper 接口
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {

    /**
     * 批量逻辑删除过期操作日志（跨租户系统维护操作）。
     * 使用 @InterceptorIgnore 跳过租户拦截，避免 fail-closed 策略下无租户上下文返回空集；
     * 同时用单条 UPDATE 替代逐条更新，解决 N+1 性能问题。
     *
     * @param expireTime 过期时间阈值（早于此时间的记录将被逻辑删除）
     * @return 受影响行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE operation_log SET is_deleted = 1 WHERE create_time < #{expireTime} AND is_deleted = 0")
    int cleanExpiredLogsBatch(@Param("expireTime") LocalDateTime expireTime);
}
