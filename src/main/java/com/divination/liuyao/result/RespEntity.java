package com.divination.liuyao.result;

import java.io.Serializable;
import org.springframework.http.HttpStatus;

public class RespEntity<T> implements Serializable {
    private Integer code;
    private String msg;
    private T data;

    public RespEntity() {
    }


    public static <T> RespEntity<T> error(int code, String msg) {
        RespEntity<T> respEntity = new RespEntity();
        respEntity.setMsg(msg);
        respEntity.setCode(code);
        return respEntity;
    }

    public static <T> RespEntity<T> error(String msg) {
        RespEntity<T> respEntity = new RespEntity();
        respEntity.setMsg(msg);
        respEntity.setCode(HttpStatus.BAD_REQUEST.value());
        return respEntity;
    }

    public static <T> RespEntity<T> ok() {
        RespEntity<T> respEntity = new RespEntity();
        respEntity.setCode(HttpStatus.OK.value());
        respEntity.setMsg("SUCCESS");
        return respEntity;
    }

    public static <T> RespEntity<T> ok(T data) {
        RespEntity<T> respEntity = new RespEntity();
        respEntity.setData(data);
        respEntity.setMsg("SUCCESS");
        respEntity.setCode(HttpStatus.OK.value());
        return respEntity;
    }

    public Integer getCode() {
        return this.code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }


}