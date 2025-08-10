package com.divination.liuyao.exception;

public class YsyjException extends RuntimeException {

    private final int code;

    public YsyjException(String message) {
        this(428, message);
    }

    public YsyjException(int code, String message) {
        super(message);
        this.code = code;
    }

}
