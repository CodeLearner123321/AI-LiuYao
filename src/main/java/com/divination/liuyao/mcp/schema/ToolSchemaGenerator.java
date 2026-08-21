package com.divination.liuyao.mcp.schema;

import com.divination.liuyao.mcp.tool.ToolField;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ToolSchemaGenerator {

    private final ObjectMapper objectMapper;

    public ToolSchemaGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode generate(Class<?> type) {
        return buildSchema(type, new HashSet<>());
    }

    private ObjectNode buildSchema(Class<?> type, Set<Class<?>> visiting) {
        ObjectNode schema = objectMapper.createObjectNode();

        if (type == Integer.class || type == int.class
            || type == Long.class || type == long.class) {
            schema.put("type", "integer");
            return schema;
        }
        if (type == Boolean.class || type == boolean.class) {
            schema.put("type", "boolean");
            return schema;
        }
        if (type == Double.class || type == double.class
            || type == Float.class || type == float.class) {
            schema.put("type", "number");
            return schema;
        }
        if (type == String.class || type == Character.class || type == char.class) {
            schema.put("type", "string");
            return schema;
        }
        if (type.isEnum()) {
            schema.put("type", "string");
            ArrayNode enumValues = schema.putArray("enum");
            for (Object constant : type.getEnumConstants()) {
                enumValues.add(String.valueOf(constant));
            }
            return schema;
        }
        if (type.isArray()) {
            schema.put("type", "array");
            schema.set("items", buildSchema(type.getComponentType(), visiting));
            return schema;
        }
        if (Collection.class.isAssignableFrom(type)) {
            schema.put("type", "array");
            ObjectNode items = objectMapper.createObjectNode();
            items.put("type", "string");
            schema.set("items", items);
            return schema;
        }
        if (Map.class.isAssignableFrom(type)) {
            schema.put("type", "object");
            schema.put("additionalProperties", true);
            return schema;
        }
        if (type.getPackageName().startsWith("java.")) {
            schema.put("type", "string");
            return schema;
        }
        if (visiting.contains(type)) {
            schema.put("type", "object");
            return schema;
        }

        visiting.add(type);
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        for (Field field : type.getDeclaredFields()) {
            if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            ObjectNode property = buildSchema(field.getType(), visiting);

            ToolField toolField = field.getAnnotation(ToolField.class);
            if (toolField != null && !toolField.description().isBlank()) {
                property.put("description", toolField.description());
            }
            if (toolField != null && toolField.required()) {
                required.add(field.getName());
            }
            properties.set(field.getName(), property);
        }

        if (required.isEmpty()) {
            schema.remove("required");
        }
        visiting.remove(type);
        return schema;
    }
}
