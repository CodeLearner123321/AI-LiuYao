package com.divination.liuyao.mcp.controller;

import com.divination.liuyao.mcp.schema.ToolSchemaGenerator;
import com.divination.liuyao.mcp.tool.ToolHandler;
import com.divination.liuyao.mcp.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final ToolSchemaGenerator toolSchemaGenerator;

    public McpController(
        ObjectMapper objectMapper,
        ToolRegistry toolRegistry,
        ToolSchemaGenerator toolSchemaGenerator
    ) {
        this.objectMapper = objectMapper;
        this.toolRegistry = toolRegistry;
        this.toolSchemaGenerator = toolSchemaGenerator;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode handleRpc(@RequestBody JsonNode request) {
        JsonNode id = request.get("id");
        String method = request.path("method").asText();

        if ("notifications/initialized".equals(method)) {
            return objectMapper.createObjectNode();
        }

        try {
            return handleRequest(id, method, request.path("params"));
        } catch (Exception ex) {
            return errorResponse(id, -32000, ex.getMessage());
        }
    }

    @GetMapping(value = "/tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode listTools() {
        return buildToolsListResult();
    }

    private JsonNode handleRequest(JsonNode id, String method, JsonNode params) {
        switch (method) {
            case "initialize":
                return successResponse(id, buildInitializeResult());
            case "ping":
                return successResponse(id, objectMapper.createObjectNode());
            case "tools/list":
                return successResponse(id, buildToolsListResult());
            case "tools/call":
                return successResponse(id, handleToolCall(params));
            default:
                throw new IllegalArgumentException("Unsupported method: " + method);
        }
    }

    private ObjectNode buildInitializeResult() {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", PROTOCOL_VERSION);
        result.putObject("capabilities").putObject("tools");
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", "ai-liuyao-mcp");
        serverInfo.put("version", "0.2.0");
        return result;
    }

    private ObjectNode buildToolsListResult() {
        ObjectNode result = objectMapper.createObjectNode();
        ArrayNode tools = result.putArray("tools");
        for (ToolHandler<?, ?> toolHandler : toolRegistry.getAll()) {
            ObjectNode tool = tools.addObject();
            tool.put("name", toolHandler.getName());
            tool.put("description", toolHandler.getDescription());
            tool.set("inputSchema", toolSchemaGenerator.generate(toolHandler.getInputType()));
            tool.set("outputSchema", toolSchemaGenerator.generate(toolHandler.getOutputType()));
        }
        return result;
    }

    private ObjectNode handleToolCall(JsonNode params) {
        String toolName = params.path("name").asText();
        JsonNode arguments = params.path("arguments");
        ToolHandler<?, ?> toolHandler = toolRegistry.getRequired(toolName);
        return toolHandler.execute(arguments == null ? objectMapper.createObjectNode() : arguments);
    }

    private ObjectNode successResponse(JsonNode id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private ObjectNode errorResponse(JsonNode id, int code, String message) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id == null ? objectMapper.nullNode() : id);
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message == null ? "Unknown error" : message);
        return response;
    }
}
