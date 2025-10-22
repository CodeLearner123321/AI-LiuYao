package com.divination.liuyao.exception;

import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.divination.liuyao.result.RespEntity;

/**
 * 全局异常处理器
 */
@Slf4j
@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    /**
     * 处理限流异常
     */
    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public RespEntity<Void> handleRateLimitException(RateLimitException e) {
        log.warn("触发限流: {}", e.getMessage());
        return RespEntity.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public RespEntity<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("认证异常: {} 请求路径: {}", e.getMessage(), request.getRequestURI());
        return RespEntity.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public RespEntity<Void> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} 请求路径: {}", e.getMessage(), request.getRequestURI());
        return RespEntity.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RespEntity<Void> handleValidationExceptions(MethodArgumentNotValidException ex) {
        log.error("参数验证异常: {}", ex.getMessage(), ex);
        StringBuilder errorMessage = new StringBuilder("输入验证失败: ");
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMsg = error.getDefaultMessage();
            errorMessage.append(fieldName).append(" ").append(errorMsg).append("; ");
        });
        return RespEntity.error(HttpStatus.BAD_REQUEST.value(), errorMessage.toString());
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RespEntity<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("未处理的异常，请求路径: {}", request.getRequestURI(), e);
        return RespEntity.error(500, "服务器内部错误");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public RespEntity<Void> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("参数错误: ", ex);
        return RespEntity.error(HttpStatus.BAD_REQUEST.value(), "参数错误: " + ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public RespEntity<Void> handleIllegalStateException(IllegalStateException ex) {
        log.error("状态错误: ", ex);
        return RespEntity.error(HttpStatus.BAD_REQUEST.value(), "状态错误: " + ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public RespEntity<Void> handleDateFormatException(Exception ex) {
        log.error("日期格式错误: {}", ex.getMessage(), ex);
        return RespEntity.error(HttpStatus.BAD_REQUEST.value(), "日期格式错误，请使用ISO格式 (例如: 2023-04-15T14:30:00)");
    }
} 