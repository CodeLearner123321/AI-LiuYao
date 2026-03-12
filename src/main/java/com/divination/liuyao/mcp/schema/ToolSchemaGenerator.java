package com.divination.liuyao.mcp.schema;

import com.divination.liuyao.mcp.tool.ToolField;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import org.springframework.stereotype.Component;

@Component
public class ToolSchemaGenerator {

    private final ObjectMapper objectMapper;

    public ToolSchemaGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectNode generate(Class<?> inputType) {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ArrayNode required = schema.putArray("required");

        for (Field field : inputType.getDeclaredFields()) {
            ObjectNode property = properties.putObject(field.getName());
            property.put("type", mapType(field.getType()));

            ToolField toolField = field.getAnnotation(ToolField.class);
            if (toolField != null && !toolField.description().isBlank()) {
                property.put("description", toolField.description());
            }
            if (toolField != null && toolField.required()) {
                required.add(field.getName());
            }
        }

        return schema;
    }

    private String mapType(Class<?> fieldType) {
        if (fieldType == Integer.class || fieldType == int.class
            || fieldType == Long.class || fieldType == long.class) {
            return "integer";
        }
        if (fieldType == Boolean.class || fieldType == boolean.class) {
            return "boolean";
        }
        if (fieldType == Double.class || fieldType == double.class
            || fieldType == Float.class || fieldType == float.class) {
            return "number";
        }
        return "string";
    }
}
