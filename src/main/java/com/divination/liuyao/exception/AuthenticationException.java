package com.divination.liuyao.exception;

public class AuthenticationException extends RuntimeException {
    private final int code;

    public AuthenticationException(String message, int code) {
        super(message);
        this.code = code;
    }

    public AuthenticationException(int code, String message) {
        super(message);
        this.code = code;
    }


    public int getCode() { return code; }

} 