package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import java.sql.SQLIntegrityConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * <p>
 * HTTP 状态码映射（客户端据此区分成功/失败）：
 * - 400 BAD_REQUEST：参数校验失败、缺失必填参数、请求体格式错误
 * - 401 UNAUTHORIZED：登录态缺失（由 LoginCheckFilter 直接返回，不走本处理器）
 * - 404 NOT_FOUND：资源不存在
 * - 409 CONFLICT：唯一约束冲突（如账号已存在）
 * - 415 UNSUPPORTED_MEDIA_TYPE：Content-Type 不支持
 * - 422 UNPROCESSABLE_ENTITY：业务校验失败（CustomException）
 * - 429 TOO_MANY_REQUESTS：接口限流
 * - 500 INTERNAL_SERVER_ERROR：未预期系统异常
 *
 * 安全加固（2026-08-23）：
 * G1: 新增 MissingServletRequestParameterException 处理器，缺失必填参数返回 400
 * G2: 新增 MissingServletRequestPartException 处理器，缺失请求体返回 400
 * G3: ConstraintViolationException/MethodArgumentNotValidException 返回 400 而非 200
 * 客户端可据此区分校验失败与业务失败，避免所有错误返回 HTTP 200 的安全隐患。
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理SQL完整性约束违反异常（唯一索引冲突）
     * 返回 409 Conflict
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<R<String>> exceptionHandler(SQLIntegrityConstraintViolationException ex) {
        log.error("SQL integrity violation", ex);

        String message = ex.getMessage();
        if (message != null && message.contains("Duplicate entry")) {
            // 不回显重复值（防止泄露账号/手机号等敏感信息），只返回通用提示
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(R.error("数据已存在，请勿重复提交"));
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(R.error("数据已存在，请勿重复提交"));
    }

    /**
     * 处理唯一约束冲突（DuplicateKeyException，Spring 对 JDBC 唯一键冲突的统一定义）
     * 返回 409 Conflict
     * <p>
     * MyBatis-Plus/MyBatis 在唯一键冲突时抛出的 {@code SQLIntegrityConstraintViolationException}
     * 会被 Spring 异常转换器包装为 {@code DuplicateKeyException}（DataAccessException 子类），
     * 此兜底保证无论以哪种异常形态到达全局处理器都返回 409 而非 500。
     * </p>
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<R<String>> handleDuplicateKeyException(DuplicateKeyException ex) {
        log.error("Duplicate key violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(R.error("数据已存在，请勿重复提交"));
    }

    /**
     * 处理自定义业务异常
     * 返回 422 Unprocessable Entity
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<R<String>> exceptionHandler(CustomException ex) {
        log.error("Business exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(R.error(ex.getMessage()));
    }

    /**
     * 处理约束违反异常（@PathVariable/@RequestParam 上的校验注解触发）
     * 安全加固 G3：返回 400 而非 200，客户端可区分校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<String>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("参数校验失败", ex);
        String message = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error("参数校验失败：" + message));
    }

    /**
     * 处理方法参数校验异常（@RequestBody 上的 @Valid 触发）
     * 安全加固 G3：返回 400 而非 200，客户端可区分校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("请求参数校验失败", ex);
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String message = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error("参数校验失败：" + message));
    }

    /**
     * 处理表单绑定异常（@ModelAttribute 上的 @Valid 触发）
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<String>> handleBindException(BindException ex) {
        log.error("表单参数绑定失败", ex);
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors();
        String message = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error("参数校验失败：" + message));
    }

    /**
     * 处理缺失请求参数异常
     * 安全加固 G1：缺失必填参数返回 400 而非 200
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<String>> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("缺失必填参数: {}", ex.getParameterName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error("缺少必填参数：" + ex.getParameterName()));
    }

    /**
     * 处理缺失请求体部分异常（multipart/form-data 的 part 缺失）
     * 安全加固 G2：缺失请求体返回 400 而非 200
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<R<String>> handleMissingRequestPart(MissingServletRequestPartException ex) {
        log.warn("缺失请求体: {}", ex.getRequestPartName());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error("缺少请求体部分：" + ex.getRequestPartName()));
    }

    /**
     * 处理请求体格式异常
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<R<String>> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.error("请求体格式错误", ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.error("请求体格式错误，请检查请求参数"));
    }

    /**
     * 处理 Content-Type 不支持异常
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<R<String>> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("不支持的媒体类型: {}", ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(R.error("不支持的请求类型，请使用 application/json"));
    }

    /**
     * 处理限流异常（RateLimitExceededException 在 RateLimitAspect 中抛出）
     * 返回 429 Too Many Requests，与 HTTP 标准对齐
     */
    @ExceptionHandler(RateLimitAspect.RateLimitExceededException.class)
    public ResponseEntity<R<String>> handleRateLimitExceededException(RateLimitAspect.RateLimitExceededException ex) {
        log.warn("接口限流：{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(R.error(ex.getMessage()));
    }

    /**
     * 处理系统通用异常（兜底）
     * 返回 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<String>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.error("系统繁忙，请稍后重试"));
    }

}