package com.divination.liuyao.util;

import com.divination.liuyao.common.annotation.AIDocField;

import java.lang.reflect.Field;
import java.util.*;

public class AIDocJsonBuilder {

    private static final Set<Class<?>> PRIMITIVE_TYPES = new HashSet<>(Arrays.asList(
            String.class, Integer.class, Long.class, Boolean.class, Double.class, Float.class, Short.class, Byte.class
    ));

    private static class FieldNote {
        String path;
        String desc;
    }

    public static String generateJsonWithNotes(Class<?> clazz) {
        StringBuilder jsonBuilder = new StringBuilder();
        List<FieldNote> notes = new ArrayList<>();

        // 生成JSON结构
        String json = buildJson(clazz, 0, "", notes);

        // 组装结果
        jsonBuilder.append(json);
//        jsonBuilder.append("\n\n补充说明：\n");
//        for (FieldNote note : notes) {
//            jsonBuilder.append("- ").append(note.path).append(": ").append(note.desc).append("\n");
//        }

        return jsonBuilder.toString();
    }

    private static String buildJson(Class<?> clazz, int indent, String parentPath, List<FieldNote> notes) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent(indent)).append("{\n");

        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            String fieldName = f.getName();
            String fieldPath = parentPath.isEmpty() ? fieldName : parentPath + "." + fieldName;

            AIDocField fieldDoc = f.getAnnotation(AIDocField.class);
            String desc = (fieldDoc != null) ? fieldDoc.desc() : "";

            sb.append(indent(indent + 2)).append("\"").append(fieldName).append("\": ");

            if (isPrimitiveOrCommonType(f.getType())) {
                // 逻辑改为：基本类型 → 直接输出描述 desc
                if (desc != null && !desc.isEmpty()) {
                    sb.append("\"").append(desc).append("\"");
                } else {
                    sb.append("\"\"");
                }

                // 基本类型不应该写 notes（根据你新的要求）
                // 所以移除 notes.add(...) 这部分

            } else {
                // 自定义对象类型 → 递归处理（保持不变）
                sb.append(buildJson(f.getType(), indent + 2, fieldPath, notes));
            }

            if (i < fields.length - 1) sb.append(",");
            sb.append("\n");
        }

        sb.append(indent(indent)).append("}");
        return sb.toString();
    }


    private static boolean isPrimitiveOrCommonType(Class<?> clazz) {
        return clazz.isPrimitive() || PRIMITIVE_TYPES.contains(clazz);
    }

    private static String indent(int n) {
        return " ".repeat(Math.max(0, n));
    }
}