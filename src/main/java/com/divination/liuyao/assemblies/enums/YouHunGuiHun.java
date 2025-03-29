package com.divination.liuyao.assemblies.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum YouHunGuiHun {
    YOUHUNGUA("游魂卦"),
    GUIHUNGUA("归魂卦");

    private final String name; // 颜色的十六进制代码

    // 构造方法
    YouHunGuiHun(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
