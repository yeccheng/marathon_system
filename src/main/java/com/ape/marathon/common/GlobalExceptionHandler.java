package com.ape.marathon.common;

import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 使用 @RestControllerAdvice 拦截 Controller 层抛出的异常，统一转为 Result 返回，
 * 避免异常直接堆栈暴露给前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 @RequestBody + @Valid 校验失败（未手动接 BindingResult 时由 Spring 抛出）
     * 返回第一个字段错误信息，格式：{"code":500,"msg":"...","data":null}
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return Result.fail(firstErrorMessage(e.getBindingResult()));
    }

    /**
     * 处理表单 / 查询参数绑定校验失败（兜底）
     */
    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        return Result.fail(firstErrorMessage(e.getBindingResult()));
    }

    /**
     * 处理业务异常（自定义运行时异常）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntime(RuntimeException e) {
        // 返回业务异常信息，状态码 500
        return Result.fail(e.getMessage());
    }

    /**
     * 处理所有未知异常（兜底），打印完整堆栈便于排查
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        return Result.fail("系统异常，请稍后重试");
    }

    /** 取 BindingResult 中第一个字段错误的中文 message */
    private static String firstErrorMessage(BindingResult bindingResult) {
        if (bindingResult != null && bindingResult.hasErrors()) {
            FieldError fieldError = bindingResult.getFieldError();
            if (fieldError != null && fieldError.getDefaultMessage() != null) {
                return fieldError.getDefaultMessage();
            }
        }
        return "参数校验失败";
    }
}
