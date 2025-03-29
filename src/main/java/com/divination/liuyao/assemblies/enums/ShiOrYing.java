package com.divination.liuyao.assemblies.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ShiOrYing {
    SHI("世"),
    YING("应");

    private final String name;

    ShiOrYing(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
