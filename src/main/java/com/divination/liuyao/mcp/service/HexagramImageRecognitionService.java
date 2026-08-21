package com.divination.liuyao.mcp.service;

import com.divination.liuyao.mcp.model.RecognizedHexagramResult;
import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.service.HexagramService;
import com.divination.liuyao.service.factory.LLMServiceFactory;
import com.divination.liuyao.util.AIDocJsonBuilder;
import com.divination.liuyao.util.ConstantUtil;
import com.divination.liuyao.util.FreemarkerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HexagramImageRecognitionService {

    private static final Logger log = LoggerFactory.getLogger(HexagramImageRecognitionService.class);

    private final LLMServiceFactory llmServiceFactory;
    private final HexagramTextFormatter hexagramTextFormatter;
    private final HexagramPosterRenderService hexagramPosterRenderService;
    private final PosterBackgroundService posterBackgroundService;
    private final HexagramService hexagramService;
    private final ObjectMapper objectMapper;

    public HexagramImageRecognitionService(
        LLMServiceFactory llmServiceFactory,
        HexagramTextFormatter hexagramTextFormatter,
        HexagramPosterRenderService hexagramPosterRenderService,
        PosterBackgroundService posterBackgroundService,
        HexagramService hexagramService,
        ObjectMapper objectMapper
    ) {
        this.llmServiceFactory = llmServiceFactory;
        this.hexagramTextFormatter = hexagramTextFormatter;
        this.hexagramPosterRenderService = hexagramPosterRenderService;
        this.posterBackgroundService = posterBackgroundService;
        this.hexagramService = hexagramService;
        this.objectMapper = objectMapper;
    }

    public RecognizedHexagramResult recognize(String imageUrl) throws IOException, InterruptedException {
        return recognize(imageUrl, true);
    }

    public RecognizedHexagramResult recognize(String imageUrl, boolean renderImage) throws IOException, InterruptedException {
        String normalizedImageUrl = normalizeImageUrl(imageUrl);

        String prompt = buildPredictionPrompt();
        AiResult aiResult = llmServiceFactory.generateTextByImage(ConstantUtil.IMAGE_SYSTEM_PROMPT, prompt, normalizedImageUrl);
        String cleaned = LLMServiceFactory.cleanJson(aiResult.getText());
        log.info("MCP image recognition cleaned response. imageUrl={}, cleaned={}", normalizedImageUrl, cleaned);
        Prediction prediction = objectMapper.readValue(cleaned, Prediction.class);
        Hexagram hexagram = hexagramService.calculateLiuYaoByImage(prediction);

        String renderedImageUrl = null;
        boolean imageRenderingImplemented = false;
        String imageRenderingStatus = "已跳过图片渲染";
        if (renderImage) {
            Path backgroundImagePath = posterBackgroundService.prepareDefaultBackgroundPath();
            try {
                renderedImageUrl = hexagramPosterRenderService.renderAndUpload(
                    hexagram,
                    prediction,
                    deriveSourceFileName(normalizedImageUrl),
                    backgroundImagePath
                );
            } finally {
                Files.deleteIfExists(backgroundImagePath);
            }
            imageRenderingImplemented = true;
            imageRenderingStatus = "卦象图片渲染完成";
        }

        return new RecognizedHexagramResult(
            normalizedImageUrl,
            normalizedImageUrl,
            renderedImageUrl,
            prediction,
            hexagram,
            hexagramTextFormatter.format(hexagram),
            imageRenderingImplemented,
            imageRenderingStatus
        );
    }

    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new IllegalArgumentException("imageUrl is required");
        }
        try {
            URI uri = new URI(imageUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("imageUrl must be an http/https URL");
            }
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("imageUrl must include a valid host");
            }
            return uri.toString();
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("imageUrl is invalid: " + imageUrl, ex);
        }
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

    private String buildPredictionPrompt() {
        String jsonTemplate = AIDocJsonBuilder.generateJsonWithNotes(Prediction.class);
        Map<String, Object> data = new HashMap<>();
        data.put("guaList", ConstantUtil.GUA_LIST);
        data.put("jsonTemplate", jsonTemplate);
        return FreemarkerUtil.render("prediction_prompt.ftl", data);
    }
}
