package com.divination.liuyao.pojo.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 视图权限枚举
 */
public enum ViewPermission {
    /**
     * 上传视图
     */
    UPLOAD_VIEW("uploadView", "上传视图");

    private final String code;
    private final String description;

    ViewPermission(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取视图权限
     */
    public static ViewPermission getByCode(String code) {
        for (ViewPermission permission : values()) {
            if (permission.getCode().equals(code)) {
                return permission;
            }
        }
        return null;
    }

}