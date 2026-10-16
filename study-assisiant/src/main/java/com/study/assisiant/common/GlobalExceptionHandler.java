package com.study.assisiant.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：统一返回体 + 真实 HTTP 状态码 + 服务端日志。
 *
 * <p>注意：这里必须把业务状态码同时写进 HTTP 状态码，
 * 否则 401/403/429 在浏览器侧会被当成 200 成功响应。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常（ResponseStatusException）：保留原始状态码与原因 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result<Void>> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {

        int code = ex.getStatusCode().value();
        String reason = ex.getReason() == null
                ? "请求处理失败" : ex.getReason();

        if (code >= 500) {
            log.error("业务异常 {} {} -> {} {}",
                    request.getMethod(), request.getRequestURI(),
                    code, reason, ex);
        } else {
            log.warn("业务异常 {} {} -> {} {}",
                    request.getMethod(), request.getRequestURI(),
                    code, reason);
        }

        return ResponseEntity.status(code)
                .body(Result.error(code, reason));
    }

    /** 参数校验失败：返回第一条错误信息，便于前端直接提示 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("请求参数校验失败");

        log.warn("参数校验失败 {} {} -> {}",
                request.getMethod(), request.getRequestURI(), message);

        return ResponseEntity.badRequest()
                .body(Result.error(400, message));
    }

    /** 单个参数校验失败（@Validated + @RequestParam 等） */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse("请求参数校验失败");

        log.warn("参数校验失败 {} {} -> {}",
                request.getMethod(), request.getRequestURI(), message);

        return ResponseEntity.badRequest()
                .body(Result.error(400, message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {

        log.warn("请求体解析失败 {} {}: {}",
                request.getMethod(), request.getRequestURI(),
                ex.getMessage());

        return ResponseEntity.badRequest()
                .body(Result.error(400, "请求参数格式错误"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request) {

        log.warn("缺少请求参数 {} {}: {}",
                request.getMethod(), request.getRequestURI(),
                ex.getParameterName());

        return ResponseEntity.badRequest()
                .body(Result.error(400,
                        "缺少必要参数：" + ex.getParameterName()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleMaxUploadSize(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        log.warn("上传文件过大 {} {}", request.getMethod(),
                request.getRequestURI());

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Result.error(413, "文件过大，请上传 20MB 以内的文件"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.error(405, "请求方法不支持"));
    }

    /** 接口不存在：不要落到兜底的 500 */
    @ExceptionHandler({NoResourceFoundException.class,
            NoHandlerFoundException.class})
    public ResponseEntity<Result<Void>> handleNotFound(
            Exception ex,
            HttpServletRequest request) {

        log.warn("接口不存在 {} {}", request.getMethod(),
                request.getRequestURI());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.error(404, "接口不存在"));
    }

    /** 兜底：记录完整堆栈，对外只暴露通用错误信息 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(
            Exception ex,
            HttpServletRequest request) {

        log.error("未预期异常 {} {}", request.getMethod(),
                request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(500, "服务器内部错误"));
    }
}
