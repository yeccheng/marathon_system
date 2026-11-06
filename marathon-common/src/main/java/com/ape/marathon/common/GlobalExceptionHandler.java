package com.ape.marathon.common;

import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        return Result.fail(firstErrorMessage(e.getBindingResult()));
    }

    @ExceptionHandler(BindException.class)
    public Result<?> handleBindException(BindException e) {
        return Result.fail(firstErrorMessage(e.getBindingResult()));
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntime(RuntimeException e) {
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        e.printStackTrace();
        return Result.fail("系统异常，请稍后重试");
    }

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
