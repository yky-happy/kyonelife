package com.yky.blog.common.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.yky.blog.common.result.Result;
import com.yky.blog.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("未登录访问: {}", e.getMessage());
        return Result.failed(ResultCode.UNAUTHORIZED);
    }

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.failed(ResultCode.FAILED, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.failed(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        return Result.failed(ResultCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.failed("系统繁忙，请稍后再试");
    }
}
