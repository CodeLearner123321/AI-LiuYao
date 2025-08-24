package com.divination.liuyao.assemblies.enums;

/**
 * 起卦方式枚举
 */
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
}