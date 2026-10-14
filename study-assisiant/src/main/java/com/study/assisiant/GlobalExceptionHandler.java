
package com.study.assisiant;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(ResponseStatusException.class)
    public Result<Void> handleResponseStatusException(
            ResponseStatusException ex) {

        int code = ex.getStatusCode().value();

        return Result.error(code, ex.getReason());
    }

    // 处理 JSON 格式错误或请求体无法解析
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex) {

        return Result.error(400, "请求参数格式错误");
    }

    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(
            MethodArgumentNotValidException ex) {

        return Result.error(400, "请求参数校验失败");
    }

    // 处理其他未预期异常
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {

        return Result.error(500, "服务器内部错误");
    }
}