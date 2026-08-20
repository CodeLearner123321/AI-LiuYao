package com.divination.liuyao.mcp.tool.impl;

import com.divination.liuyao.assemblies.enums.CastType;
import com.divination.liuyao.mcp.exception.McpProtocolException;
import com.divination.liuyao.mcp.model.RecognizedHexagramResult;
import com.divination.liuyao.mcp.service.HexagramAnalysisPosterRenderService;
import com.divination.liuyao.mcp.service.HexagramImageRecognitionService;
import com.divination.liuyao.mcp.service.PosterBackgroundService;
import com.divination.liuyao.mcp.tool.ToolField;
import com.divination.liuyao.mcp.tool.ToolHandler;
import com.divination.liuyao.pojo.dto.CastDto;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.service.AiAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RecognizeHexagramWithAnalysisImageTool implements ToolHandler<RecognizeHexagramWithAnalysisImageTool.Input, RecognizeHexagramWithAnalysisImageTool.Output> {

    private static final Logger log = LoggerFactory.getLogger(RecognizeHexagramWithAnalysisImageTool.class);

    private final ObjectMapper objectMapper;
    private final HexagramImageRecognitionService recognitionService;
    private final AiAnalysisService aiAnalysisService;
    private final HexagramAnalysisPosterRenderService analysisPosterRenderService;
    private final PosterBackgroundService posterBackgroundService;

    public RecognizeHexagramWithAnalysisImageTool(
        ObjectMapper objectMapper,
        HexagramImageRecognitionService recognitionService,
        AiAnalysisService aiAnalysisService,
        HexagramAnalysisPosterRenderService analysisPosterRenderService,
        PosterBackgroundService posterBackgroundService
    ) {
        this.objectMapper = objectMapper;
        this.recognitionService = recognitionService;
        this.aiAnalysisService = aiAnalysisService;
        this.analysisPosterRenderService = analysisPosterRenderService;
        this.posterBackgroundService = posterBackgroundService;
    }

    @Override
    public String getName() {
        return "recognize_hexagram_with_analysis_image";
    }

    @Override
    public String getDescription() {
        return "识别网络图片中的六爻卦例，直接生成算卦结论，并渲染带结论的结果图片。";
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
        Input input = objectMapper.convertValue(arguments, Input.class);
        if (input.getImageUrl() == null || input.getImageUrl().isBlank()) {
            throw McpProtocolException.invalidParams("imageUrl is required");
        }

        try {
            RecognizedHexagramResult recognized = recognitionService.recognize(input.getImageUrl(), false);
            String question = firstNonBlank(
                input.getQuestion(),
                recognized.getHexagram() == null ? null : recognized.getHexagram().getQuestionDescription()
            );
            String background = firstNonBlank(
                input.getBackground(),
                recognized.getHexagram() == null ? null : recognized.getHexagram().getQuestionBackground()
            );
            if (recognized.getHexagram() != null) {
                recognized.getHexagram().setQuestionDescription(blankToEmpty(question));
                recognized.getHexagram().setQuestionBackground(blankToEmpty(background));
            }

            CastDto castDto = buildCastDto(recognized, question, background);
            AiResult analysisResult = aiAnalysisService.analyzeHexagram(recognized.getHexagram(), castDto);

            Path backgroundImagePath = posterBackgroundService.prepareDefaultBackgroundPath();
            try {
                String imageUrl = analysisPosterRenderService.renderAndUpload(
                    recognized.getHexagram(),
                    recognized.getPrediction(),
                    analysisResult,
                    deriveSourceFileName(input.getImageUrl()),
                    backgroundImagePath
                );
                return buildResponse(recognized, analysisResult, imageUrl);
            } finally {
                Files.deleteIfExists(backgroundImagePath);
            }
        } catch (McpProtocolException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to generate analysis poster. imageUrl={}", input.getImageUrl(), ex);
            throw new IllegalArgumentException("识别并生成断卦结果图失败: " + buildErrorMessage(ex), ex);
        }
    }

    /**
     * 为分析链路补齐最小 CastDto，上游识别出的卦象直接复用，不再重新起卦。
     */
    private CastDto buildCastDto(RecognizedHexagramResult recognized, String question, String background) {
        CastDto castDto = new CastDto();
        castDto.setCastType(CastType.IMAGE);
        castDto.setQuestion(blankToEmpty(question));
        castDto.setBackground(blankToEmpty(background));
        if (recognized.getHexagram() != null) {
            castDto.setCustomTime(recognized.getHexagram().getCustomTime());
            castDto.setNumber(recognized.getHexagram().getNumber());
        }
        return castDto;
    }

    /**
     * 按 MCP 约定拼装结构化返回结果，只返回最终结果图地址。
     */
    private ObjectNode buildResponse(RecognizedHexagramResult recognized, AiResult analysisResult, String imageUrl) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("isError", false);

        ObjectNode structuredContent = response.putObject("structuredContent");
        String sourceImageUrl = firstNonBlank(recognized.getSourceImageUrl(), recognized.getInputImageUrl());
        structuredContent.put("sourceImageUrl", blankToEmpty(sourceImageUrl));
        structuredContent.put("imageUrl", blankToEmpty(imageUrl));
        structuredContent.put("hexagramText", blankToEmpty(recognized.getHexagramText()));
        structuredContent.put("analysisText", blankToEmpty(analysisResult == null ? null : analysisResult.getText()));

        ArrayNode content = response.putArray("content");
        ObjectNode summaryText = content.addObject();
        summaryText.put("type", "text");
        summaryText.put("text", buildSummaryText(sourceImageUrl, imageUrl, recognized.getHexagramText()));

        ObjectNode jsonText = content.addObject();
        jsonText.put("type", "text");
        jsonText.put("text", structuredContent.toString());

        return response;
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

    private String buildSummaryText(String sourceImageUrl, String imageUrl, String hexagramText) {
        StringBuilder builder = new StringBuilder("断卦结果图已生成");
        if (imageUrl != null && !imageUrl.isBlank()) {
            builder.append("，图片地址: ").append(imageUrl);
        }
        if (sourceImageUrl != null && !sourceImageUrl.isBlank()) {
            builder.append("\n来源图片: ").append(sourceImageUrl);
        }
        if (hexagramText != null && !hexagramText.isBlank()) {
            builder.append("\n卦象文本: ").append(hexagramText);
        }
        return builder.toString();
    }

    private String deriveSourceFileName(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank() || path.endsWith("/")) {
                return "remote-image.png";
            }
            String fileName = Paths.get(path).getFileName().toString();
            return fileName.isBlank() ? "remote-image.png" : fileName;
        } catch (Exception ex) {
            return "remote-image.png";
        }
    }

    public static class Input {
        @ToolField(description = "待识别六爻图片的网络 URL，必须是外部可访问的 http/https 地址。", required = true)
        private String imageUrl;

        @ToolField(description = "用于算卦分析的问题描述。若不传则尝试使用图片识别结果中的问题。")
        private String question;

        @ToolField(description = "用于算卦分析的背景信息。若不传则尝试使用图片识别结果中的背景。")
        private String background;

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
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

    /**
     * MCP 输出结构定义，仅用于生成 outputSchema。
     */
    public static class Output {
        @ToolField(description = "调用是否失败，成功时固定为 false。", required = true)
        private Boolean isError;

        @ToolField(description = "结构化返回内容。", required = true)
        private StructuredContent structuredContent;

        @ToolField(description = "兼容 MCP 客户端的人类可读内容。", required = true)
        private ContentItem[] content;
    }

    /**
     * MCP 结构化返回内容，仅包含结果图地址。
     */
    public static class StructuredContent {
        @ToolField(description = "识别输入的原始图片 URL。", required = true)
        private String sourceImageUrl;

        @ToolField(description = "渲染生成的断卦结果图 URL。", required = true)
        private String imageUrl;

        @ToolField(description = "识别得到的卦象文本。", required = true)
        private String hexagramText;

        @ToolField(description = "AI 断卦分析文本。", required = true)
        private String analysisText;
    }

    public static class ContentItem {
        @ToolField(description = "内容类型，当前固定为 text。", required = true)
        private String type;

        @ToolField(description = "供模型或用户直接读取的文本内容。", required = true)
        private String text;
    }
}
