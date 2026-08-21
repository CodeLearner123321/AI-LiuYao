package com.divination.liuyao.mcp.controller;

import com.divination.liuyao.mcp.protocol.McpProtocolSupport;
import com.divination.liuyao.mcp.schema.ToolSchemaGenerator;
import com.divination.liuyao.mcp.service.HexagramTextFormatter;
import com.divination.liuyao.mcp.tool.ToolRegistry;
import com.divination.liuyao.mcp.tool.impl.CastTodayFortuneTool;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.service.AiAnalysisService;
import com.divination.liuyao.service.HexagramService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class McpControllerTodayFortuneTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private McpController controller;

    @BeforeEach
    void setUp() throws Exception {
        HexagramService hexagramService = mock(HexagramService.class);
        AiAnalysisService aiAnalysisService = mock(AiAnalysisService.class);
        HexagramTextFormatter hexagramTextFormatter = mock(HexagramTextFormatter.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-28T02:30:00Z"), ZONE);

        Hexagram hexagram = new Hexagram();
        when(hexagramService.castHexagram(any(CastDto.class))).thenReturn(hexagram);
        when(hexagramTextFormatter.format(hexagram)).thenReturn("今日运势卦象文本");

        AiResult aiResult = new AiResult();
        aiResult.setText("今日运势分析");
        aiResult.setKeyOutcome("稳中有进");
        aiResult.setIsTrue(true);
        when(aiAnalysisService.analyzeTodayFortune(eq(hexagram), any(CastDto.class))).thenReturn(aiResult);

        CastTodayFortuneTool tool = new CastTodayFortuneTool(
            objectMapper,
            hexagramService,
            aiAnalysisService,
            hexagramTextFormatter,
            clock
        );

        controller = new McpController(
            objectMapper,
            new ToolRegistry(List.of(tool)),
            new ToolSchemaGenerator(objectMapper)
        );
    }

    @Test
    void shouldListTodayFortuneTool() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 1);
        request.put("method", "tools/list");
        request.putObject("params");

        JsonNode response = controller.handleRpc(null, request).getBody();

        assertEquals("cast_today_fortune", response.path("result").path("tools").get(0).path("name").asText());
        assertTrue(response.path("result").path("tools").get(0).path("inputSchema").path("properties").path("castTime").isObject());
        assertTrue(response.path("result").path("tools").get(0).path("outputSchema").path("properties").path("structuredContent").path("properties").path("keyOutcome").isObject());
    }

    @Test
    void shouldCallTodayFortuneToolAndReturnStructuredContent() {
        ObjectNode request = objectMapper.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", 2);
        request.put("method", "tools/call");
        ObjectNode params = request.putObject("params");
        params.put("name", "cast_today_fortune");
        ObjectNode arguments = params.putObject("arguments");
        arguments.put("castTime", "2026-05-28T10:30:00");
        arguments.put("question", "今天适合推进吗？");
        arguments.put("background", "准备推进一项合作。");

        JsonNode response = controller.handleRpc(null, request).getBody();

        assertFalse(response.path("result").path("isError").asBoolean());
        assertEquals("2026-05-28T10:30:00", response.path("result").path("structuredContent").path("castTime").asText());
        assertEquals("今天适合推进吗？", response.path("result").path("structuredContent").path("question").asText());
        assertEquals("稳中有进", response.path("result").path("structuredContent").path("keyOutcome").asText());
        assertTrue(response.path("result").path("content").get(0).path("text").asText().contains("今日运势已生成"));
        assertEquals(McpProtocolSupport.DEFAULT_PROTOCOL_VERSION, controller.listTools(null).getHeaders().getFirst(McpProtocolSupport.PROTOCOL_VERSION_HEADER));
    }
}
