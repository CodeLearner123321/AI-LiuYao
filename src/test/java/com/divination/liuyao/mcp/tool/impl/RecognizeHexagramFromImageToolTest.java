package com.divination.liuyao.mcp.tool.impl;

import com.divination.liuyao.mcp.model.RecognizedHexagramResult;
import com.divination.liuyao.mcp.service.HexagramImageRecognitionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecognizeHexagramFromImageToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HexagramImageRecognitionService recognitionService = mock(HexagramImageRecognitionService.class);
    private final RecognizeHexagramFromImageTool tool = new RecognizeHexagramFromImageTool(objectMapper, recognitionService);

    @Test
    void shouldReturnImageResourceContentWhenRenderEnabled() throws Exception {
        RecognizedHexagramResult result = new RecognizedHexagramResult();
        result.setInputImageUrl("https://example.com/input.png");
        result.setHexagramText("卦象文本");
        result.setSourceImageUrl("https://example.com/input.png");
        result.setImageUrl("https://oss.example.com/poster.png");
        result.setImageRenderingImplemented(true);
        result.setImageRenderingStatus("卦象图片渲染完成");
        when(recognitionService.recognize(anyString(), eq(true))).thenReturn(result);

        ObjectNode args = objectMapper.createObjectNode();
        args.put("imageUrl", "https://example.com/input.png");
        ObjectNode response = tool.execute(args);

        assertFalse(response.get("isError").asBoolean());
        assertEquals("卦象文本", response.get("content").get(0).get("text").asText());
        assertEquals("resource_link", response.get("content").get(2).get("type").asText());
        assertEquals("https://oss.example.com/poster.png", response.get("content").get(2).get("uri").asText());
        verify(recognitionService).recognize("https://example.com/input.png", true);
    }

    @Test
    void shouldSkipImageResourceContentWhenRenderDisabled() throws Exception {
        RecognizedHexagramResult result = new RecognizedHexagramResult();
        result.setInputImageUrl("https://example.com/input.png");
        result.setHexagramText("卦象文本");
        result.setSourceImageUrl("https://example.com/input.png");
        result.setImageRenderingImplemented(false);
        result.setImageRenderingStatus("已跳过图片渲染");
        when(recognitionService.recognize(anyString(), eq(false))).thenReturn(result);

        ObjectNode args = objectMapper.createObjectNode();
        args.put("imageUrl", "https://example.com/input.png");
        args.put("renderImage", false);
        ObjectNode response = tool.execute(args);

        assertEquals(2, response.get("content").size());
        assertTrue(response.get("content").get(1).get("text").asText().contains("已跳过图片渲染"));
        verify(recognitionService).recognize("https://example.com/input.png", false);
    }
}
