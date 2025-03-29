package com.divination.liuyao.assemblies.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LiuQin {
    /**
     * 手动起卦
     */
    XIONGDI("兄弟"),
    ZISUN("子孙"),
    QICAI("妻财"),
    GUANGUI("官鬼"),
    FUMU("父母");

    private final String name;

    // 构造方法
    LiuQin(String name) {
        this.name = name;
    }

    public static LiuQin getLiuQinByName(String name) {
        for (LiuQin liuQin : LiuQin.values()) {
            if (liuQin.getName().equals(name)) {
                return liuQin;
            }
        }
        throw new IllegalArgumentException("No LiuQin found with name: " + name);
    }

    @JsonValue
    public String getName() {
        return name;
    }
}
