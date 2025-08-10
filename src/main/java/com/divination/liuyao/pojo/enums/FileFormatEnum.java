package com.divination.liuyao.pojo.enums;

import java.util.List;

public enum FileFormatEnum {
    PDF("pdf"),
    DOC("doc"),
    DOCX("docx"),
    JPG("jpg"),
    PNG("png"),
    SVG("svg"),
    ;

    private final String value;

    FileFormatEnum(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static boolean isSupported(String format) {
        for (FileFormatEnum type : FileFormatEnum.values()) {
            if (type.getValue().equalsIgnoreCase(format)) {
                return true;
            }
        }
        return false;
    }

    public static FileFormatEnum getByValue(String value) {
        for (FileFormatEnum type : FileFormatEnum.values()) {
            if (type.getValue().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }

}

