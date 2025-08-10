package com.divination.liuyao.pojo.enums;

/**
 * 起卦类型枚举
 */
public enum CastType {
    LIUYAO("六爻"),
    QIMEN("奇门"),
    ZIWEI("紫微"),
    BAZI("八字");

    private final String description;

    CastType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static CastType fromString(String text) {
        for (CastType type : CastType.values()) {
            if (type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的起卦类型: " + text);
    }
} 