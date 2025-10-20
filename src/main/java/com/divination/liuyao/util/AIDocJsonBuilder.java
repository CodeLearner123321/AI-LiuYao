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
        // todo 待优化
        jsonBuilder.append("补充说明,六十四卦如下：风地观,雷山小过,火泽暌,火山旅,乾为天,泽火革,雷泽归妹,天火同人,火风鼎,风水涣,坎为水,雷风恒,离为火,地天泰,泽山咸,山地剥,坤为地,地火明夷,风泽中孚,雷天大壮,火天大有,泽雷随,山火贲,地水师,巽为风,山水蒙,震为雷,火雷噬嗑,泽天夬,水地比,天地否,泽地萃,艮为山,水火即济,风火家人,地泽临,山泽损,地雷复,水风井,泽水困,地风升,天水讼,山风蛊,天雷无妄,水泽节,水山蹇,风山渐,火地晋,雷地豫,天山遁,兑为泽,水雷屯,山天大蓄,风雷益,雷火丰,天泽履,火水未济,泽风大过,天风姤,雷水解,山雷颐,水天需,风天小蓄,地山谦");

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
                if (desc.contains("根节点")) {
                    // 根节点直接把描述放到 JSON 中
                    sb.append("\"").append(desc).append("\"");
                } else {
                    // 非根节点，值为空，但说明记录到 notes
                    sb.append("\"\"");
                    FieldNote note = new FieldNote();
                    note.path = fieldPath;
                    note.desc = desc.isEmpty() ? "无" : desc; // 空说明可以写成“无”
                    notes.add(note);
                }
            } else {
                // 自定义类，递归构建
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