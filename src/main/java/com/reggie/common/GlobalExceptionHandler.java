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
        log.error(ex.getMessage());

        if(ex.getMessage().contains("Duplicate entry")){
            String[] split = ex.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return R.error(msg);
        }

        return R.error("未知错误");
    }

    /**
     * 异常处理方法
     * @return
     */
    @ExceptionHandler(CustomException.class)
    public R<String> exceptionHandler(CustomException ex){
        log.error(ex.getMessage());

        return R.error(ex.getMessage());
    }

    /**
     * 处理参数校验异常（@Valid / @Validated）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public R<String> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("参数校验失败：{}", ex.getMessage());
        String message = ex.getConstraintViolations()
                           .stream()
                           .map(ConstraintViolation::getMessage)
                           .collect(Collectors.joining(", "));
        return R.error("参数校验失败：" + message);
    }

    /**
     * 处理请求体校验异常（@Valid @RequestBody）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public R<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        log.error("请求参数校验失败：{}", ex.getMessage());
        String message = ex.getBindingResult()
                           .getFieldErrors()
                           .stream()
                           .map(error -> error.getField() + ": " + error.getDefaultMessage())
                           .collect(Collectors.joining(", "));
        return R.error("参数校验失败：" + message);
    }

}
