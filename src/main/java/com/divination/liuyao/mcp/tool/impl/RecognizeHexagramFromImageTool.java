package com.divination.liuyao.mcp.tool.impl;

import com.divination.liuyao.mcp.model.RecognizedHexagramResult;
import com.divination.liuyao.mcp.service.HexagramImageRecognitionService;
import com.divination.liuyao.mcp.tool.ToolField;
import com.divination.liuyao.mcp.tool.ToolHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

@Component
public class RecognizeHexagramFromImageTool implements ToolHandler<RecognizeHexagramFromImageTool.Input, RecognizedHexagramResult> {

    private final ObjectMapper objectMapper;
    private final HexagramImageRecognitionService recognitionService;

    public RecognizeHexagramFromImageTool(ObjectMapper objectMapper, HexagramImageRecognitionService recognitionService) {
        this.objectMapper = objectMapper;
        this.recognitionService = recognitionService;
    }

    @Override
    public String getName() {
        return "recognize_hexagram_from_image";
    }

    @Override
    public String getDescription() {
        return "识别网络图片中的六爻卦例，可选生成 HTML 海报图片，返回结构化卦象、文本卦象和成图结果。";
    }

    @Override
    public Class<Input> getInputType() {
        return Input.class;
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
            return buildResponse(result);
        } catch (Exception ex) {
            throw new IllegalArgumentException("识别卦例失败: " + ex.getMessage(), ex);
        }
    }

    private ObjectNode buildResponse(RecognizedHexagramResult result) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("isError", false);
        response.set("structuredContent", objectMapper.valueToTree(result));

        ArrayNode content = response.putArray("content");
        content.addObject()
            .put("type", "text")
            .put("text", result.getHexagramText());

        content.addObject()
            .put("type", "text")
            .put("text", buildSummaryText(result));

        if (result.getImageUrl() != null && !result.getImageUrl().isBlank()) {
            content.addObject()
                .put("type", "resource_link")
                .put("uri", result.getImageUrl())
                .put("name", "六爻卦例海报")
                .put("mimeType", "image/png");
        }
        return response;
    }

    private String buildSummaryText(RecognizedHexagramResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("输入图片URL: ").append(nullSafe(result.getInputImageUrl())).append('\n');
        builder.append("图片渲染状态: ").append(nullSafe(result.getImageRenderingStatus()));
        if (result.getImageUrl() != null && !result.getImageUrl().isBlank()) {
            builder.append('\n').append("海报URL: ").append(result.getImageUrl());
        }
        return builder.toString();
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    public static class Input {
        @ToolField(description = "待识别六爻图片的网络 URL，必须是外部可访问的 http/https 地址。", required = true)
        private String imageUrl;

        @ToolField(description = "是否生成并上传卦象海报图片。默认 true。")
        private Boolean renderImage;

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
    }
}
