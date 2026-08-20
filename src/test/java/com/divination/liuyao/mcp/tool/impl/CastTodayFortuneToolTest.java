package com.divination.liuyao.mcp.tool.impl;

import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.mcp.exception.McpProtocolException;
import com.divination.liuyao.mcp.service.HexagramTextFormatter;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.service.AiAnalysisService;
import com.divination.liuyao.service.HexagramService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CastTodayFortuneToolTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldUseCurrentTimeAndDefaultQuestionWhenArgumentsAreEmpty() throws Exception {
        HexagramService hexagramService = mock(HexagramService.class);
        AiAnalysisService aiAnalysisService = mock(AiAnalysisService.class);
        HexagramTextFormatter hexagramTextFormatter = mock(HexagramTextFormatter.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-28T02:30:00Z"), ZONE);

        Hexagram hexagram = new Hexagram();
        when(hexagramService.castHexagram(any(CastDto.class))).thenReturn(hexagram);
        when(hexagramTextFormatter.format(hexagram)).thenReturn("卦象文本");

        AiResult aiResult = new AiResult();
        aiResult.setText("今日运势分析");
        aiResult.setKeyOutcome("小雨无妨事可为");
        aiResult.setIsTrue(true);
        when(aiAnalysisService.analyzeTodayFortune(eq(hexagram), any(CastDto.class))).thenReturn(aiResult);

        CastTodayFortuneTool tool = new CastTodayFortuneTool(
            objectMapper,
            hexagramService,
            aiAnalysisService,
            hexagramTextFormatter,
            clock
        );

        ObjectNode response = tool.execute(objectMapper.createObjectNode());

        assertFalse(response.get("isError").asBoolean());
        assertEquals("2026-05-28T10:30:00", response.path("structuredContent").path("castTime").asText());
        assertEquals("请测我今日运势", response.path("structuredContent").path("question").asText());
        assertEquals("卦象文本", response.path("structuredContent").path("hexagramText").asText());
        assertEquals("今日运势分析", response.path("structuredContent").path("analysisText").asText());
        assertEquals("小雨无妨事可为", response.path("structuredContent").path("keyOutcome").asText());
        assertTrue(response.path("content").get(0).path("text").asText().contains("小雨无妨事可为"));
        assertTrue(response.path("content").get(1).path("text").asText().contains("\"keyOutcome\":\"小雨无妨事可为\""));

        ArgumentCaptor<CastDto> castDtoCaptor = ArgumentCaptor.forClass(CastDto.class);
        verify(hexagramService).castHexagram(castDtoCaptor.capture());
        CastDto castDto = castDtoCaptor.getValue();
        LocalDateTime expectedTime = LocalDateTime.of(2026, 5, 28, 10, 30);
        assertEquals(CastType.TIME, castDto.getCastType());
        assertEquals(expectedTime, castDto.getCastTime());
        assertEquals(expectedTime.atZone(ZONE).toInstant().toEpochMilli(), castDto.getTimestamp());
        assertEquals("请测我今日运势", castDto.getQuestion());
        assertEquals("请结合今日卦象，综合判断今天在事业、财运、感情、健康与宜忌上的走势。", castDto.getBackground());
        assertEquals("请测我今日运势", hexagram.getQuestionDescription());
        assertEquals("请结合今日卦象，综合判断今天在事业、财运、感情、健康与宜忌上的走势。", hexagram.getQuestionBackground());
    }

    @Test
    void shouldUseExplicitCastTimeQuestionAndBackground() throws Exception {
        HexagramService hexagramService = mock(HexagramService.class);
        AiAnalysisService aiAnalysisService = mock(AiAnalysisService.class);
        HexagramTextFormatter hexagramTextFormatter = mock(HexagramTextFormatter.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-28T00:00:00Z"), ZONE);

        Hexagram hexagram = new Hexagram();
        when(hexagramService.castHexagram(any(CastDto.class))).thenReturn(hexagram);
        when(hexagramTextFormatter.format(hexagram)).thenReturn("自定义卦象文本");

        AiResult aiResult = new AiResult();
        aiResult.setText("自定义日运分析");
        aiResult.setKeyOutcome("稳中求进");
        aiResult.setIsTrue(true);
        when(aiAnalysisService.analyzeTodayFortune(eq(hexagram), any(CastDto.class))).thenReturn(aiResult);

        CastTodayFortuneTool tool = new CastTodayFortuneTool(
            objectMapper,
            hexagramService,
            aiAnalysisService,
            hexagramTextFormatter,
            clock
        );

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("castTime", "2026-05-28 09:15:00");
        arguments.put("question", "今天出门办事顺不顺？");
        arguments.put("background", "上午要去谈一件重要事情。");

        ObjectNode response = tool.execute(arguments);

        assertEquals("2026-05-28T09:15:00", response.path("structuredContent").path("castTime").asText());
        assertEquals("今天出门办事顺不顺？", response.path("structuredContent").path("question").asText());
        assertEquals("上午要去谈一件重要事情。", response.path("structuredContent").path("background").asText());
        assertEquals("自定义日运分析", response.path("structuredContent").path("analysisText").asText());

        ArgumentCaptor<CastDto> castDtoCaptor = ArgumentCaptor.forClass(CastDto.class);
        verify(aiAnalysisService).analyzeTodayFortune(eq(hexagram), castDtoCaptor.capture());
        assertEquals(LocalDateTime.of(2026, 5, 28, 9, 15), castDtoCaptor.getValue().getCastTime());
    }

    @Test
    void shouldRejectInvalidCastTimeAsProtocolError() {
        CastTodayFortuneTool tool = new CastTodayFortuneTool(
            objectMapper,
            mock(HexagramService.class),
            mock(AiAnalysisService.class),
            mock(HexagramTextFormatter.class),
            Clock.fixed(Instant.parse("2026-05-28T00:00:00Z"), ZONE)
        );

        ObjectNode arguments = objectMapper.createObjectNode();
        arguments.put("castTime", "not-a-date");

        McpProtocolException exception = assertThrows(McpProtocolException.class, () -> tool.execute(arguments));
        assertEquals(McpProtocolException.INVALID_PARAMS, exception.getCode());
    }

    @Test
    void shouldWrapAiFailureAsToolExecutionFailure() throws Exception {
        HexagramService hexagramService = mock(HexagramService.class);
        AiAnalysisService aiAnalysisService = mock(AiAnalysisService.class);
        HexagramTextFormatter hexagramTextFormatter = mock(HexagramTextFormatter.class);

        Hexagram hexagram = new Hexagram();
        when(hexagramService.castHexagram(any(CastDto.class))).thenReturn(hexagram);
        when(aiAnalysisService.analyzeTodayFortune(eq(hexagram), any(CastDto.class)))
            .thenThrow(new IllegalStateException("AI service unavailable"));

        CastTodayFortuneTool tool = new CastTodayFortuneTool(
            objectMapper,
            hexagramService,
            aiAnalysisService,
            hexagramTextFormatter,
            Clock.fixed(Instant.parse("2026-05-28T00:00:00Z"), ZONE)
        );

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> tool.execute(objectMapper.createObjectNode())
        );
        assertTrue(exception.getMessage().contains("测今日运势失败"));
        assertTrue(exception.getMessage().contains("AI service unavailable"));
    }
}
