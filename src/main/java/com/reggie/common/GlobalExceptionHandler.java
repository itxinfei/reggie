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
 * 全局异常处理
 */
@ControllerAdvice(annotations = {RestController.class, Controller.class})
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 异常处理方法
     * @return
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

    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException ex){
        log.error("Business exception: {}", ex.getMessage(), ex);
        return R.error(ex.getMessage());
    }

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

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R<String> handleException(Exception ex) {
        log.error("系统异常", ex);
        return R.error("系统繁忙，请稍后重试");
    }

}
