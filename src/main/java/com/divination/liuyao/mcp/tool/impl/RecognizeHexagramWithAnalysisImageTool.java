package com.divination.liuyao.mcp.tool.impl;

import com.divination.liuyao.assemblies.enums.CastType;
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
            throw new IllegalArgumentException("imageUrl is required");
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
                return buildResponse(imageUrl);
            } finally {
                Files.deleteIfExists(backgroundImagePath);
            }
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
    private ObjectNode buildResponse(String imageUrl) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("isError", false);

        ObjectNode structuredContent = response.putObject("structuredContent");
        structuredContent.put("imageUrl", blankToEmpty(imageUrl));

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
    }

    /**
     * MCP 结构化返回内容，仅包含结果图地址。
     */
    public static class StructuredContent {
        @ToolField(description = "渲染生成的断卦结果图 URL。", required = true)
        private String imageUrl;
    }
}
