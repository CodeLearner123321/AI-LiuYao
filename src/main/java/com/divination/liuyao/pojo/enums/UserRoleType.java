package com.divination.liuyao.pojo.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户角色枚举
 */
public enum UserRoleType {
    /**
     * 普通用户
     */
    USER(1, "普通用户"),

    /**
     * 管理员
     */
    ROOT(100, "管理员");

    private final Integer code;
    private final String description;

    UserRoleType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public Integer getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取角色
     */
    public static UserRoleType getByCode(Integer code) {
        for (UserRoleType role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return USER; // 默认返回普通用户
    }
}