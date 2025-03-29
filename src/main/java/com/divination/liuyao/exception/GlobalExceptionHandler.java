package com.divination.liuyao.exception;

import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.divination.liuyao.result.RespEntity;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    public RespEntity<Void> handleAuthenticationException(AuthenticationException ex) {
        log.error("认证异常: {}", ex.getMessage(), ex);
        return RespEntity.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
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

    @ExceptionHandler(Exception.class)
    public RespEntity<Void> handleGenericException(Exception ex) {
        log.error("服务器异常: {}", ex.getMessage(), ex);
        return RespEntity.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
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