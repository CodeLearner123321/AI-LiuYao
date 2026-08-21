package com.divination.liuyao.mcp.controller;

import com.divination.liuyao.mcp.exception.McpProtocolException;
import com.divination.liuyao.mcp.protocol.McpProtocolSupport;
import com.divination.liuyao.mcp.schema.ToolSchemaGenerator;
import com.divination.liuyao.mcp.tool.ToolField;
import com.divination.liuyao.mcp.tool.ToolHandler;
import com.divination.liuyao.mcp.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

public class McpControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private McpController controller;

    @BeforeEach
    void setUp() {
        controller = new McpController(
            objectMapper,
            new ToolRegistry(List.of(new DummyTool(objectMapper))),
            new ToolSchemaGenerator(objectMapper)
        );
    }

    @Test
    void shouldNegotiateInitializeVersionAndEchoHeader() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "initialize");
        ObjectNode params = request.putObject("params");
        params.put("protocolVersion", "2025-06-18");

        ResponseEntity<JsonNode> response = controller.handleRpc(null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("2025-06-18", response.getHeaders().getFirst(McpProtocolSupport.PROTOCOL_VERSION_HEADER));
        assertEquals("2.0", response.getBody().path("jsonrpc").asText());
        assertEquals("2025-06-18", response.getBody().path("result").path("protocolVersion").asText());
        assertTrue(response.getBody().path("result").path("capabilities").path("tools").isObject());
    }

    @Test
    void shouldListToolsWithSchemas() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 2);
        request.put("method", "tools/list");
        request.putObject("params");

        ResponseEntity<JsonNode> response = controller.handleRpc(null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode tool = response.getBody().path("result").path("tools").get(0);
        assertEquals("dummy_tool", tool.path("name").asText());
        assertEquals("A dummy tool for MCP tests.", tool.path("description").asText());
        assertTrue(tool.path("inputSchema").path("properties").path("value").isObject());
        assertTrue(tool.path("outputSchema").path("properties").path("structuredContent").path("properties").path("value").isObject());
    }

    @Test
    void shouldReturnJsonRpcErrorForUnknownTool() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 3);
        request.put("method", "tools/call");
        ObjectNode params = request.putObject("params");
        params.put("name", "missing_tool");
        params.putObject("arguments").put("value", "ok");

        ResponseEntity<JsonNode> response = controller.handleRpc(null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().has("error"));
        assertEquals(McpProtocolException.INVALID_PARAMS, response.getBody().path("error").path("code").asInt());
        assertTrue(response.getBody().path("result").isMissingNode());
    }

    @Test
    void shouldReturnJsonRpcErrorForMissingRequiredArguments() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 4);
        request.put("method", "tools/call");
        ObjectNode params = request.putObject("params");
        params.put("name", "dummy_tool");
        params.putObject("arguments");

        ResponseEntity<JsonNode> response = controller.handleRpc(null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().has("error"));
        assertEquals(McpProtocolException.INVALID_PARAMS, response.getBody().path("error").path("code").asInt());
    }

    @Test
    void shouldReturnToolErrorResultForBusinessFailure() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 5);
        request.put("method", "tools/call");
        ObjectNode params = request.putObject("params");
        params.put("name", "dummy_tool");
        params.putObject("arguments").put("value", "boom");

        ResponseEntity<JsonNode> response = controller.handleRpc(null, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().path("result").path("isError").asBoolean());
        assertTrue(response.getBody().path("result").path("content").get(0).path("text").asText().contains("exploded"));
    }

    @Test
    void shouldExposeCompatibilityToolsAlias() {
        ResponseEntity<JsonNode> response = controller.listTools(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(McpProtocolSupport.DEFAULT_PROTOCOL_VERSION, response.getHeaders().getFirst(McpProtocolSupport.PROTOCOL_VERSION_HEADER));
        assertEquals("dummy_tool", response.getBody().path("tools").get(0).path("name").asText());
    }

    private static class DummyTool implements ToolHandler<DummyInput, DummyOutput> {

        private final ObjectMapper objectMapper;

        private DummyTool(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public String getName() {
            return "dummy_tool";
        }

        @Override
        public String getDescription() {
            return "A dummy tool for MCP tests.";
        }

        @Override
        public Class<DummyInput> getInputType() {
            return DummyInput.class;
        }

        @Override
        public Class<DummyOutput> getOutputType() {
            return DummyOutput.class;
        }

        @Override
        public ObjectNode execute(JsonNode arguments) {
            String value = arguments.path("value").asText(null);
            if (value == null || value.isBlank()) {
                throw McpProtocolException.invalidParams("value is required");
            }
            if ("boom".equals(value)) {
                throw new IllegalStateException("tool exploded");
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.put("isError", false);
            ObjectNode structuredContent = response.putObject("structuredContent");
            structuredContent.put("value", value);
            ArrayNode content = response.putArray("content");
            ObjectNode text = content.addObject();
            text.put("type", "text");
            text.put("text", "ok:" + value);
            return response;
        }
    }

    private static class DummyInput {

        @ToolField(description = "A required value.", required = true)
        private String value;
    }

    private static class DummyOutput {

        @ToolField(description = "Whether the tool failed.", required = true)
        private Boolean isError;

        @ToolField(description = "Structured content.", required = true)
        private DummyStructuredContent structuredContent;

        @ToolField(description = "Human-readable content.", required = true)
        private DummyContentItem[] content;
    }

    private static class DummyStructuredContent {

        @ToolField(description = "Echoed value.", required = true)
        private String value;
    }

    private static class DummyContentItem {

        @ToolField(description = "Content type.", required = true)
        private String type;

        @ToolField(description = "Content text.", required = true)
        private String text;
    }

}
