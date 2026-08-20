package com.divination.liuyao.mcp.tool.impl;

import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.mcp.exception.McpProtocolException;
import com.divination.liuyao.mcp.service.HexagramTextFormatter;
import com.divination.liuyao.mcp.tool.ToolField;
import com.divination.liuyao.mcp.tool.ToolHandler;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.service.AiAnalysisService;
import com.divination.liuyao.service.HexagramService;
import com.divination.liuyao.util.BaZiUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CastTodayFortuneTool implements ToolHandler<CastTodayFortuneTool.Input, CastTodayFortuneTool.Output> {

    private static final Logger log = LoggerFactory.getLogger(CastTodayFortuneTool.class);
    private static final DateTimeFormatter OUTPUT_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String DEFAULT_QUESTION = "请测我今日运势";
    private static final String DEFAULT_BACKGROUND = "请结合今日卦象，综合判断今天在事业、财运、感情、健康与宜忌上的走势。";

    private final ObjectMapper objectMapper;
    private final HexagramService hexagramService;
    private final AiAnalysisService aiAnalysisService;
    private final HexagramTextFormatter hexagramTextFormatter;
    private final Clock clock;

    @Autowired
    public CastTodayFortuneTool(
        ObjectMapper objectMapper,
        HexagramService hexagramService,
        AiAnalysisService aiAnalysisService,
        HexagramTextFormatter hexagramTextFormatter
    ) {
        this(objectMapper, hexagramService, aiAnalysisService, hexagramTextFormatter, Clock.systemDefaultZone());
    }

    public CastTodayFortuneTool(
        ObjectMapper objectMapper,
        HexagramService hexagramService,
        AiAnalysisService aiAnalysisService,
        HexagramTextFormatter hexagramTextFormatter,
        Clock clock
    ) {
        this.objectMapper = objectMapper;
        this.hexagramService = hexagramService;
        this.aiAnalysisService = aiAnalysisService;
        this.hexagramTextFormatter = hexagramTextFormatter;
        this.clock = clock;
    }

    @Override
    public String getName() {
        return "cast_today_fortune";
    }

    @Override
    public String getDescription() {
        return "基于六爻时间起卦，生成今日运势、事业、财运、感情、健康与宜忌分析。";
    }

    @Override
    public Class<Input> getInputType() {
        return Input.class;
    }

    @Override
    public Class<Output> getOutputType() {
        return Output.class;
    }

    @Override
    public ObjectNode execute(JsonNode arguments) {
        Input input = arguments == null || arguments.isMissingNode()
            ? new Input()
            : objectMapper.convertValue(arguments, Input.class);
        LocalDateTime castTime = resolveCastTime(input.getCastTime());
        String question = firstNonBlank(input.getQuestion(), DEFAULT_QUESTION);
        String background = firstNonBlank(input.getBackground(), DEFAULT_BACKGROUND);

        try {
            CastDto castDto = new CastDto();
            castDto.setCastType(CastType.TIME);
            castDto.setTimestamp(castTime.atZone(clock.getZone()).toInstant().toEpochMilli());
            castDto.setCastTime(castTime);
            castDto.setQuestion(question);
            castDto.setBackground(background);

            Hexagram hexagram = hexagramService.castHexagram(castDto);
            if (hexagram == null) {
                throw new IllegalStateException("起卦结果为空");
            }
            hexagram.setQuestionDescription(question);
            hexagram.setQuestionBackground(background);
            hexagram.setCustomTime(BaZiUtil.getAllByLocalDateTime(castTime));

            AiResult analysisResult = aiAnalysisService.analyzeTodayFortune(hexagram, castDto);
            return buildResponse(castTime, question, background, hexagram, analysisResult);
        } catch (McpProtocolException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to generate today fortune. castTime={}, question={}", castTime, question, ex);
            throw new IllegalArgumentException("测今日运势失败: " + buildErrorMessage(ex), ex);
        }
    }

    private ObjectNode buildResponse(
        LocalDateTime castTime,
        String question,
        String background,
        Hexagram hexagram,
        AiResult analysisResult
    ) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("isError", false);

        ObjectNode structuredContent = response.putObject("structuredContent");
        structuredContent.put("castTime", OUTPUT_TIME_FORMATTER.format(castTime));
        structuredContent.put("question", question);
        structuredContent.put("background", background);
        structuredContent.put("hexagramText", blankToEmpty(hexagramTextFormatter.format(hexagram)));
        structuredContent.put("analysisText", blankToEmpty(analysisResult == null ? null : analysisResult.getText()));
        structuredContent.put("keyOutcome", blankToEmpty(analysisResult == null ? null : analysisResult.getKeyOutcome()));

        ArrayNode content = response.putArray("content");
        ObjectNode summaryText = content.addObject();
        summaryText.put("type", "text");
        summaryText.put("text", buildSummaryText(castTime, analysisResult));

        ObjectNode jsonText = content.addObject();
        jsonText.put("type", "text");
        jsonText.put("text", structuredContent.toString());

        return response;
    }

    private LocalDateTime resolveCastTime(String castTime) {
        if (castTime == null || castTime.isBlank()) {
            return LocalDateTime.now(clock);
        }

        String normalized = castTime.trim();
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (DateTimeParseException ignored) {
        }

        throw McpProtocolException.invalidParams("castTime must be a valid local date-time, such as 2026-05-28T12:30:00");
    }

    private String buildSummaryText(LocalDateTime castTime, AiResult analysisResult) {
        StringBuilder builder = new StringBuilder("今日运势已生成");
        builder.append("\n起卦时间: ").append(OUTPUT_TIME_FORMATTER.format(castTime));
        String keyOutcome = analysisResult == null ? null : analysisResult.getKeyOutcome();
        if (keyOutcome != null && !keyOutcome.isBlank()) {
            builder.append("\n判辞: ").append(keyOutcome);
        }
        return builder.toString();
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

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static class Input {
        @ToolField(description = "起卦时间，支持 ISO 本地时间或 yyyy-MM-dd HH:mm[:ss] 格式；不传则默认当前时间。")
        private String castTime;

        @ToolField(description = "今日运势问题描述，未传时默认使用“请测我今日运势”。")
        private String question;

        @ToolField(description = "今日运势背景说明，未传时使用默认日运背景。")
        private String background;

        public String getCastTime() {
            return castTime;
        }

        public void setCastTime(String castTime) {
            this.castTime = castTime;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }
    }

    public static class Output {
        @ToolField(description = "调用是否失败，成功时固定为 false。", required = true)
        private Boolean isError;

        @ToolField(description = "结构化返回内容。", required = true)
        private StructuredContent structuredContent;

        @ToolField(description = "兼容 MCP 客户端的人类可读内容。", required = true)
        private ContentItem[] content;
    }

    public static class StructuredContent {
        @ToolField(description = "实际用于起卦的时间。", required = true)
        private String castTime;

        @ToolField(description = "今日运势问题。", required = true)
        private String question;

        @ToolField(description = "今日运势背景。", required = true)
        private String background;

        @ToolField(description = "六爻卦象文本。", required = true)
        private String hexagramText;

        @ToolField(description = "AI 断卦分析文本。", required = true)
        private String analysisText;

        @ToolField(description = "AI 生成的判辞。", required = true)
        private String keyOutcome;
    }

    public static class ContentItem {
        @ToolField(description = "内容类型，当前固定为 text。", required = true)
        private String type;

        @ToolField(description = "供模型或用户直接读取的文本内容。", required = true)
        private String text;
    }
}
