package com.divination.liuyao.exception;

public class BusinessException extends RuntimeException {
    private final Integer code; // 错误码
    private final String message; // 错误消息

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BusinessException(String message) {
        super(message);
        this.code = 401;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}