package com.divination.liuyao.mcp.controller;

import com.divination.liuyao.mcp.exception.McpProtocolException;
import com.divination.liuyao.mcp.protocol.McpProtocolSupport;
import com.divination.liuyao.mcp.schema.ToolSchemaGenerator;
import com.divination.liuyao.mcp.tool.ToolHandler;
import com.divination.liuyao.mcp.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
public class McpController {

    private static final Logger log = LoggerFactory.getLogger(McpController.class);

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
    public ResponseEntity<JsonNode> handleRpc(
        @RequestHeader(value = McpProtocolSupport.PROTOCOL_VERSION_HEADER, required = false) String protocolVersionHeader,
        @RequestBody(required = false) JsonNode request
    ) {
        String requestProtocolVersion = extractProtocolVersion(request);
        String negotiatedProtocolVersion;
        try {
            negotiatedProtocolVersion = negotiateProtocolVersion(protocolVersionHeader, requestProtocolVersion);
        } catch (McpProtocolException ex) {
            return buildResponse(
                HttpStatus.BAD_REQUEST,
                buildErrorResponse(null, ex),
                McpProtocolSupport.DEFAULT_PROTOCOL_VERSION
            );
        }

        if (request == null || !request.isObject()) {
            return buildResponse(
                HttpStatus.BAD_REQUEST,
                buildErrorResponse(null, McpProtocolException.invalidRequest("Request body must be a JSON object")),
                negotiatedProtocolVersion
            );
        }

        JsonNode id = request.get("id");
        String method = request.path("method").asText(null);
        if (method == null || method.isBlank()) {
            return buildResponse(
                HttpStatus.OK,
                buildErrorResponse(id, McpProtocolException.invalidRequest("method is required")),
                negotiatedProtocolVersion
            );
        }

        if ("notifications/initialized".equals(method)) {
            return buildResponse(HttpStatus.OK, objectMapper.createObjectNode(), negotiatedProtocolVersion);
        }

        try {
            return buildResponse(
                HttpStatus.OK,
                successResponse(id, handleRequest(method, request.path("params"), negotiatedProtocolVersion)),
                negotiatedProtocolVersion
            );
        } catch (McpProtocolException ex) {
            return buildResponse(HttpStatus.OK, buildErrorResponse(id, ex), negotiatedProtocolVersion);
        } catch (Exception ex) {
            log.error("MCP request failed. method={}, id={}", method, id, ex);
            return buildResponse(
                HttpStatus.OK,
                buildErrorResponse(id, McpProtocolException.internalError(buildErrorMessage(ex))),
                negotiatedProtocolVersion
            );
        }
    }

    @GetMapping(value = "/tools", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> listTools(
        @RequestHeader(value = McpProtocolSupport.PROTOCOL_VERSION_HEADER, required = false) String protocolVersionHeader
    ) {
        String protocolVersion;
        try {
            protocolVersion = negotiateProtocolVersion(protocolVersionHeader, null);
        } catch (McpProtocolException ex) {
            return buildResponse(
                HttpStatus.BAD_REQUEST,
                buildErrorResponse(null, ex),
                McpProtocolSupport.DEFAULT_PROTOCOL_VERSION
            );
        }
        return buildResponse(HttpStatus.OK, buildToolsListResult(), protocolVersion);
    }

    private JsonNode handleRequest(String method, JsonNode params, String protocolVersion) {
        switch (method) {
            case "initialize":
                return buildInitializeResult(protocolVersion);
            case "ping":
                return objectMapper.createObjectNode();
            case "tools/list":
                return buildToolsListResult();
            case "tools/call":
                return handleToolCallSafely(params);
            default:
                throw McpProtocolException.methodNotFound("Unsupported method: " + method);
        }
    }

    private ObjectNode buildInitializeResult(String protocolVersion) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", protocolVersion);
        result.putObject("capabilities").putObject("tools");
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", McpProtocolSupport.SERVER_NAME);
        serverInfo.put("version", McpProtocolSupport.SERVER_VERSION);
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

    private ObjectNode handleToolCallSafely(JsonNode params) {
        try {
            return handleToolCall(params);
        } catch (McpProtocolException ex) {
            throw ex;
        } catch (Exception ex) {
            String toolName = params == null ? "" : params.path("name").asText("");
            log.error("MCP tool call failed. tool={}", toolName, ex);
            return buildToolErrorResult(buildErrorMessage(ex));
        }
    }

    private ObjectNode handleToolCall(JsonNode params) {
        if (params == null || !params.isObject()) {
            throw McpProtocolException.invalidParams("params must be a JSON object");
        }

        String toolName = params.path("name").asText(null);
        if (toolName == null || toolName.isBlank()) {
            throw McpProtocolException.invalidParams("Tool name is required");
        }

        JsonNode arguments = params.path("arguments");
        ToolHandler<?, ?> toolHandler = toolRegistry.getRequired(toolName);
        return toolHandler.execute(arguments == null || arguments.isMissingNode()
            ? objectMapper.createObjectNode()
            : arguments);
    }

    private ObjectNode buildToolErrorResult(String message) {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("isError", true);
        ArrayNode content = result.putArray("content");
        ObjectNode textItem = content.addObject();
        textItem.put("type", "text");
        textItem.put("text", message == null || message.isBlank() ? "Tool execution failed" : message);
        return result;
    }

    private ObjectNode successResponse(JsonNode id, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);
        response.set("result", result);
        return response;
    }

    private ObjectNode buildErrorResponse(JsonNode id, McpProtocolException ex) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id == null ? objectMapper.nullNode() : id);
        ObjectNode error = response.putObject("error");
        error.put("code", ex.getCode());
        error.put("message", buildErrorMessage(ex));
        return response;
    }

    private ResponseEntity<JsonNode> buildResponse(HttpStatus status, JsonNode body, String protocolVersion) {
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .header(McpProtocolSupport.PROTOCOL_VERSION_HEADER, protocolVersion)
            .body(body);
    }

    private String negotiateProtocolVersion(String headerVersion, String requestVersion) {
        String normalizedHeader = normalizeProtocolVersion(headerVersion);
        String normalizedRequest = normalizeProtocolVersion(requestVersion);

        if (normalizedHeader != null && normalizedRequest != null && !Objects.equals(normalizedHeader, normalizedRequest)) {
            throw McpProtocolException.invalidRequest(
                "Conflicting MCP protocol versions: header=" + normalizedHeader + ", request=" + normalizedRequest
            );
        }

        String chosen = normalizedHeader != null
            ? normalizedHeader
            : (normalizedRequest != null ? normalizedRequest : McpProtocolSupport.DEFAULT_PROTOCOL_VERSION);

        if (!McpProtocolSupport.isSupported(chosen)) {
            throw McpProtocolException.invalidRequest("Unsupported MCP protocol version: " + chosen);
        }
        return chosen;
    }

    private String normalizeProtocolVersion(String version) {
        if (version == null) {
            return null;
        }
        String trimmed = version.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String extractProtocolVersion(JsonNode request) {
        if (request == null || !request.isObject()) {
            return null;
        }
        JsonNode params = request.path("params");
        if (params == null || !params.isObject()) {
            return null;
        }
        JsonNode protocolVersion = params.get("protocolVersion");
        if (protocolVersion == null || protocolVersion.isNull()) {
            return null;
        }
        String text = protocolVersion.asText(null);
        return normalizeProtocolVersion(text);
    }

    private String buildErrorMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return root.getClass().getSimpleName();
    }
}
