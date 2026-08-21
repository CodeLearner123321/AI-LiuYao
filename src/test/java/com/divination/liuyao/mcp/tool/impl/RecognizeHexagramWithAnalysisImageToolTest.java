package com.divination.liuyao.mcp.tool.impl;

import com.divination.liuyao.mcp.exception.McpProtocolException;
import com.divination.liuyao.mcp.model.RecognizedHexagramResult;
import com.divination.liuyao.mcp.service.HexagramAnalysisPosterRenderService;
import com.divination.liuyao.mcp.service.HexagramImageRecognitionService;
import com.divination.liuyao.mcp.service.PosterBackgroundService;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.Hexagram;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RecognizeHexagramWithAnalysisImageToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldBuildStructuredOutputWithImageAndAnalysis(@TempDir Path tempDir) throws Exception {
        HexagramImageRecognitionService recognitionService = mock(HexagramImageRecognitionService.class);
        com.divination.liuyao.service.AiAnalysisService aiAnalysisService = mock(com.divination.liuyao.service.AiAnalysisService.class);
        HexagramAnalysisPosterRenderService renderService = mock(HexagramAnalysisPosterRenderService.class);
        PosterBackgroundService backgroundService = mock(PosterBackgroundService.class);

        Path backgroundPath = tempDir.resolve("background.png");
        Files.writeString(backgroundPath, "background");
        when(backgroundService.prepareDefaultBackgroundPath()).thenReturn(backgroundPath);

        RecognizeHexagramWithAnalysisImageTool tool = new RecognizeHexagramWithAnalysisImageTool(
            objectMapper,
            recognitionService,
            aiAnalysisService,
            renderService,
            backgroundService
        );

        Hexagram hexagram = new Hexagram();
        hexagram.setCustomTime("2026年5月28日");
        hexagram.setNumber("123123");

        RecognizedHexagramResult recognized = new RecognizedHexagramResult(
            "https://example.com/source.png",
            "https://example.com/source.png",
            null,
            null,
            hexagram,
            "乾为天，利见大人。",
            false,
            "已跳过图片渲染"
        );

        when(recognitionService.recognize("https://example.com/source.png", false)).thenReturn(recognized);

        AiResult aiResult = new AiResult();
        aiResult.setText("分析内容");
        aiResult.setKeyOutcome("可成");
        aiResult.setIsTrue(true);
        aiResult.setInputToken(0L);
        aiResult.setOutputToken(0L);
        when(aiAnalysisService.analyzeHexagram(any(Hexagram.class), any())).thenReturn(aiResult);

        when(renderService.renderAndUpload(
            any(Hexagram.class),
            any(),
            any(),
            anyString(),
            eq(backgroundPath)
        )).thenReturn("https://cdn.example.com/result.png");

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("imageUrl", "https://example.com/source.png");
        arguments.put("question", "这件事能成吗？");

        ObjectNode response = tool.execute(arguments);

        assertFalse(response.get("isError").asBoolean());
        assertEquals("https://example.com/source.png", response.path("structuredContent").path("sourceImageUrl").asText());
        assertEquals("https://cdn.example.com/result.png", response.path("structuredContent").path("imageUrl").asText());
        assertEquals("乾为天，利见大人。", response.path("structuredContent").path("hexagramText").asText());
        assertEquals("分析内容", response.path("structuredContent").path("analysisText").asText());
        assertTrue(response.path("content").isArray());
        assertTrue(response.path("content").get(0).path("text").asText().contains("https://cdn.example.com/result.png"));
        assertTrue(response.path("content").get(1).path("text").asText().contains("\"analysisText\":\"分析内容\""));

        verify(recognitionService).recognize("https://example.com/source.png", false);
        verify(renderService).renderAndUpload(any(Hexagram.class), any(), any(), anyString(), eq(backgroundPath));
    }

    @Test
    void shouldRejectMissingImageUrlAsProtocolError() {
        RecognizeHexagramWithAnalysisImageTool tool = new RecognizeHexagramWithAnalysisImageTool(
            objectMapper,
            mock(HexagramImageRecognitionService.class),
            mock(com.divination.liuyao.service.AiAnalysisService.class),
            mock(HexagramAnalysisPosterRenderService.class),
            mock(PosterBackgroundService.class)
        );

        ObjectNode arguments = objectMapper.createObjectNode();

        McpProtocolException exception = assertThrows(McpProtocolException.class, () -> tool.execute(arguments));
        assertEquals(McpProtocolException.INVALID_PARAMS, exception.getCode());
    }
}
