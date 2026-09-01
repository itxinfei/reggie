package com.reggie.module.schedule.aspect;

import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.reggie.common.JacksonObjectMapper;
import com.reggie.common.LogMaskUtils;
import com.reggie.module.sys.model.OperationLog;
import com.reggie.module.schedule.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * <p>
 * 操作日志AOP切面，自动记录Controller层的增删改操作。
 * </p>
 *
 * @author 心飞为你飞
 * @since 2026-07-09
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    /** 操作日志服务 */
    @Autowired
    private OperationLogService operationLogService;

    /** JSON序列化工具 */
    private static final JacksonObjectMapper OBJECT_MAPPER = new JacksonObjectMapper();

    /** error_msg 数据库字段上限，留 10 个字符安全余量 */
    private static final int ERROR_MSG_MAX_LENGTH = 490;

    // 只拦截 POST/PUT/DELETE（增删改）
    /**
     * 环绕通知：记录操作日志
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 异常
     */
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.PutMapping) " +
            "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping)")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        String errorMsg = null;
        Object result = null;

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attributes.getRequest();

        try {
            result = joinPoint.proceed();
            success = true;
            return result;
        } catch (Exception e) {
            errorMsg = truncateErrorMsg(e.getMessage());
            throw e;
        } finally {
            try {
                long duration = System.currentTimeMillis() - startTime;
                OperationLog opLog = buildOperationLog(joinPoint, request, success, errorMsg, duration);
                if (opLog != null) {
                    operationLogService.recordLog(opLog);
                }
            } catch (Exception e) {
                log.error("记录操作日志失败", e);
            }
        }
    }

    /**
     * 截断错误信息，避免超出数据库字段长度
     */
    private String truncateErrorMsg(String errorMsg) {
        if (errorMsg == null) {
            return null;
        }
        if (errorMsg.length() <= ERROR_MSG_MAX_LENGTH) {
            return errorMsg;
        }
        return errorMsg.substring(0, ERROR_MSG_MAX_LENGTH) + "...";
    }

    /**
     * 构建操作日志对象
     *
     * @param joinPoint 连接点
     * @param request   HTTP请求
     * @param success   是否成功
     * @param errorMsg  错误信息
     * @param duration  执行时长（毫秒）
     * @return 操作日志对象
     */
    private OperationLog buildOperationLog(ProceedingJoinPoint joinPoint,
                                           HttpServletRequest request,
                                           boolean success,
                                           String errorMsg,
                                           long duration) {
        try {
            OperationLog opLog = new OperationLog();
            opLog.setIsSuccess(success ? 1 : 0);
            opLog.setErrorMsg(errorMsg);
            opLog.setDuration(duration);
            opLog.setRequestUrl(request.getRequestURI());
            opLog.setRequestMethod(request.getMethod());
            opLog.setOperatorIp(getClientIp(request));

            // 从Session获取操作人信息
            Long empId = (Long) request.getSession().getAttribute("employee");
            Long userId = (Long) request.getSession().getAttribute("user");
            if (empId != null) {
                opLog.setOperatorId(empId);
                opLog.setOperatorName("员工-" + empId);
            } else if (userId != null) {
                opLog.setOperatorId(userId);
                opLog.setOperatorName("用户-" + userId);
            }

            // 提取类名推断模块
            String className = joinPoint.getTarget().getClass().getName();
            opLog.setModule(extractModule(className));
            opLog.setTableName(extractTableName(className));
            opLog.setOperationType(determineOperationType(request.getMethod()));

            // 记录请求参数（脱敏处理）：跳过 Servlet 基础设施对象（request/response/session 不可序列化），
            // 取第一个可序列化的业务参数（如 DTO），避免每次写日志都触发序列化 WARN
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                for (Object arg : args) {
                    String params = serializeRequestParam(arg);
                    if (params != null) {
                        opLog.setRequestParams(LogMaskUtils.maskSensitiveInfo(params));
                        break;
                    }
                }
            }

            // 操作描述
            opLog.setDescription(String.format("%s %s %s",
                opLog.getModule(), opLog.getOperationType(), request.getRequestURI()));

            // 尝试获取业务ID
            if (args != null && args.length > 0 && args[0] != null) {
                try {
                    Object bizId = ReflectionKit.getFieldValue(args[0], "id");
                    if (bizId instanceof Number) {
                        opLog.setBizId(((Number) bizId).longValue());
                    }
                } catch (Exception ignored) {
                    // 无法获取ID时跳过
                }
            }

            return opLog;
        } catch (Exception e) {
            log.error("构建操作日志对象失败", e);
            return null;
        }
    }

    /**
     * 序列化单个请求参数为 JSON。
     * <p>Servlet 基础设施对象（ServletRequest/Response/HttpSession）无法被 Jackson 序列化，
     * 直接跳过；其余业务参数序列化失败时记 WARN 并返回 null（不影响操作日志落库）。
     *
     * @param arg 方法入参
     * @return JSON 字符串；不可序列化/失败返回 null
     */
    private String serializeRequestParam(Object arg) {
        if (arg == null
            || arg instanceof javax.servlet.ServletRequest
            || arg instanceof javax.servlet.ServletResponse
            || arg instanceof javax.servlet.http.HttpSession) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(arg);
        } catch (Exception e) {
            log.warn("序列化请求参数失败: {}", arg.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求
     * @return 客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }

    /**
     * 从类名提取模块名
     */
    private String extractModule(String className) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return simpleName.replace("Controller", "")
                         .replace("ServiceImpl", "")
                         .replace("Service", "");
    }

    /**
     * 从类名提取表名（近似推断）
     */
    private String extractTableName(String className) {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        return simpleName.replace("Controller", "")
                         .replace("ServiceImpl", "")
                         .replace("Service", "");
    }

    /**
     * 根据HTTP方法确定操作类型
     */
    private String determineOperationType(String httpMethod) {
        switch (httpMethod.toUpperCase()) {
            case "POST": return "INSERT";
            case "PUT": return "UPDATE";
            case "DELETE": return "DELETE";
            default: return "OTHER";
        }
    }
}

