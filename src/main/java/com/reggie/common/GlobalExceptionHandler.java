package com.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;
import java.sql.SQLIntegrityConstraintViolationException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * <p>
 * 全局异常处理器
 * </p>
 *
 * @author 心飞为你飞
 * @since 2024-01-01
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理SQL完整性约束违反异常
     *
     * @param ex SQL完整性约束违反异常
     * @return 统一响应对象
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException ex){
        log.error("SQL integrity violation", ex);

        if(ex.getMessage().contains("Duplicate entry")){
            String[] split = ex.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return R.error(msg);
        }

        return R.error("未知错误");
    }

    /**
     * 处理自定义业务异常
     *
     * @param ex 自定义业务异常
     * @return 统一响应对象
     */
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException ex){
        log.error("Business exception: {}", ex.getMessage(), ex);
        return R.error(ex.getMessage());
    }

    /**
     * 处理约束违反异常
     *
     * @param ex 约束违反异常
     * @return 统一响应对象
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public R<String> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("参数校验失败", ex);
        String message = ex.getConstraintViolations()
                           .stream()
                           .map(ConstraintViolation::getMessage)
                           .collect(Collectors.joining(", "));
        return R.error("参数校验失败：" + message);
    }

    /**
     * 处理方法参数校验异常
     *
     * @param ex 方法参数校验异常
     * @return 统一响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public R<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("请求参数校验失败", ex);
        String message = ex.getBindingResult()
                           .getFieldErrors()
                           .stream()
                           .map(error -> error.getField() + ": " + error.getDefaultMessage())
                           .collect(Collectors.joining(", "));
        return R.error("参数校验失败：" + message);
    }

    /**
     * 处理系统通用异常
     *
     * @param ex 系统异常
     * @return 统一响应对象
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R<String> handleException(Exception ex) {
        log.error("系统异常", ex);
        return R.error("系统繁忙，请稍后重试");
    }

}
