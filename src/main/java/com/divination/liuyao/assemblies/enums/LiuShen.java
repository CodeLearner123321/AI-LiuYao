package com.divination.liuyao.assemblies.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LiuShen {

    QINGLONG("青龙"),
    ZHUQUE("朱雀"),
    GOUCHEN("勾陈"),
    TENGSHE("腾蛇"),
    BAIHU("白虎"),
    XUANWU("玄武");

    private final String name;

    LiuShen(String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
