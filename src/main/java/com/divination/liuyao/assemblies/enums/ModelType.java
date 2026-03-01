package com.divination.liuyao.assemblies.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ModelType {
    DeepSeek("deepseek-r1", "deepseek-r1","deepseek-r1-250528"), //文本识别模型
    qwenPlus("qwen-plus", "qwen-plus","qwen-plus"), //文本识别模型
    qwenVLPlus("qwen-vl-max", "qwen-vl-max","qwen-vl-max") //图形识别模型
    ;


    private final String value;
    //阿里百练平台的modelId
    private final String dashScopeValue;
    //火山引擎平台的modelId
    private final String volcengineValue;


    ModelType(String value, String dashScopeValue, String volcengineValue) {
        this.value = value;
        this.dashScopeValue = dashScopeValue;
        this.volcengineValue = volcengineValue;
    }

    @JsonValue
    public String getValue() {
        return value;
    }



    public String getDashScopeValue() {
        return dashScopeValue;
    }

    public String getVolcengineValue() {
        return volcengineValue;
    }

    /**
     * 根据 value 获取枚举
     */
    public static ModelType fromValue(String value) {
        return Arrays.stream(ModelType.values())
                .filter(e -> e.getValue().equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown ModelType value: " + value));
    }

    /**
     * 判断是否存在某个 value
     */
    public static boolean exists(String value) {
        return Arrays.stream(ModelType.values())
                .anyMatch(e -> e.getValue().equals(value));
    }

    @Override
    public String toString() {
        return "ModelType{" +
                "value='" + value + '\'' +
                ", dashScopeValue='" + dashScopeValue + '\'' +
                ", volcengineValue='" + volcengineValue + '\'' +
                '}';
    }
}
