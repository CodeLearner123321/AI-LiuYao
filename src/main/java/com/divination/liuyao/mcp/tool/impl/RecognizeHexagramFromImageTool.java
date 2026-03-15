package com.divination.liuyao.mcp.tool.impl;

import com.divination.liuyao.mcp.model.RecognizedHexagramResult;
import com.divination.liuyao.mcp.service.HexagramImageRecognitionService;
import com.divination.liuyao.mcp.tool.ToolField;
import com.divination.liuyao.mcp.tool.ToolHandler;
import com.divination.liuyao.service.AiAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class RecognizeHexagramFromImageTool implements ToolHandler<RecognizeHexagramFromImageTool.Input, RecognizeHexagramFromImageTool.Output> {

    private final ObjectMapper objectMapper;
    private final HexagramImageRecognitionService recognitionService;
    private final AiAnalysisService aiAnalysisService;

    public RecognizeHexagramFromImageTool(
        ObjectMapper objectMapper,
        HexagramImageRecognitionService recognitionService,
        AiAnalysisService aiAnalysisService
    ) {
        this.objectMapper = objectMapper;
        this.recognitionService = recognitionService;
        this.aiAnalysisService = aiAnalysisService;
    }

    @Override
    public String getName() {
        return "recognize_hexagram_from_image";
    }

    @Override
    public String getDescription() {
        return "识别网络图片中的六爻卦例，可选生成海报图片，并基于识别卦象生成分析提示词。";
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

        boolean renderImage = input.getRenderImage() == null || input.getRenderImage();
        try {
            RecognizedHexagramResult result = recognitionService.recognize(input.getImageUrl(), renderImage);
            String question = firstNonBlank(
                input.getQuestion(),
                result.getHexagram() == null ? null : result.getHexagram().getQuestionDescription()
            );
            String background = firstNonBlank(
                input.getBackground(),
                result.getHexagram() == null ? null : result.getHexagram().getQuestionBackground()
            );
            String constructionPrompt = aiAnalysisService.buildConstructionPrompt(
                result.getHexagram(),
                blankToEmpty(question),
                blankToEmpty(background)
            );
            return buildResponse(result.getImageUrl(), constructionPrompt);
        } catch (Exception ex) {
            throw new IllegalArgumentException("识别卦例失败: " + ex.getMessage(), ex);
        }
    }

    private ObjectNode buildResponse(String imageUrl, String constructionPrompt) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("isError", false);

        ObjectNode structuredContent = response.putObject("structuredContent");
        structuredContent.put("imageUrl", blankToEmpty(imageUrl));
        structuredContent.put("constructionPrompt", blankToEmpty(constructionPrompt));

        return response;
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
        @ToolField(description = "待识别六爻图片的网络 URL，必须是外部可访问的 http/https 地址。", required = true)
        private String imageUrl;

        @ToolField(description = "是否生成并上传卦象海报图片。默认 true。")
        private Boolean renderImage;

        @ToolField(description = "用于构造提示词的问题描述。若不传则尝试使用图片识别结果中的问题。")
        private String question;

        @ToolField(description = "用于构造提示词的背景信息。若不传则尝试使用图片识别结果中的背景。")
        private String background;

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        public Boolean getRenderImage() {
            return renderImage;
        }

        public void setRenderImage(Boolean renderImage) {
            this.renderImage = renderImage;
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
    }

    public static class StructuredContent {
        @ToolField(description = "渲染生成的海报 URL。renderImage=false 时可能为空。", required = true)
        private String imageUrl;

        @ToolField(description = "根据识别卦象构建的分析提示词。", required = true)
        private String constructionPrompt;
    }
}
