package com.divination.liuyao.assemblies.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ModelType {
    DeepSeek("deepseek-r1", "deepseek-r1","deepseek-r1-250120");

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
}
