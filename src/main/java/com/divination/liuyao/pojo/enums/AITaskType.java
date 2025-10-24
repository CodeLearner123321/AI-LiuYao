package com.divination.liuyao.pojo.enums;

import lombok.Getter;

@Getter
public enum AITaskType {
    IMAGE(1, "图像处理"),
    TEXT(2, "文本处理");

    private final Integer code;
    private final String name;

    AITaskType(int code, String name) {
        this.code = code;
        this.name = name;
    }


    /**
     * 根据code获取枚举
     */
    public static AITaskType fromCode(int code) {
        for (AITaskType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的支付类型 code: " + code);
    }

}