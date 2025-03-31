package com.divination.liuyao.assemblies.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * LLM服务类型枚举
 */
public enum LLMServiceType {
    /**
     * 火山引擎服务
     */
    VOLCENGINE("volcengine"),

    /**
     * 阿里百练服务
     */
    DASHSCOPE("dashscope");

    private final String value;

    LLMServiceType(String value) {
        this.value = value;
    }

    /**
     * 根据value获取枚举值
     */
    public static LLMServiceType fromvalue(String value) {
        if (value == null) {
            return null;
        }

        for (LLMServiceType type : values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
