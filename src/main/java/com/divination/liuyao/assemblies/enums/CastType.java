package com.divination.liuyao.assemblies.enums;

import lombok.Getter;

/**
 * 起卦方式枚举
 */
@Getter
public enum CastType {
    /**
     * 手动起卦
     */
    MANUAL("手动起卦"),

    /**
     * 系统随机起卦
     */
    RANDOM("系统随机起卦"),

    /**
     * 系统时间起卦
     */
    TIME("系统时间起卦"),

    IMAGE("图片起卦");

    ;


    private final String description;

    CastType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // 根据数据库中的字段（MANUAL / RANDOM / TIME），返回对应枚举
    public static CastType fromCode(String code) {
        for (CastType c : values()) {
            if (c.name().equalsIgnoreCase(code)) {
                return c;
            }
        }
        return null;
    }

}