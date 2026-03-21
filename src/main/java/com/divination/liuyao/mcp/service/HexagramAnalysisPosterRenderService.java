package com.divination.liuyao.mcp.service;

import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.model.AiResult;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.util.OSSUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

@Service
public class HexagramAnalysisPosterRenderService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final HexagramAnalysisPosterHtmlService htmlService;
    private final BrowserScreenshotService browserScreenshotService;

    public HexagramAnalysisPosterRenderService(
        HexagramAnalysisPosterHtmlService htmlService,
        BrowserScreenshotService browserScreenshotService
    ) {
        this.htmlService = htmlService;
        this.browserScreenshotService = browserScreenshotService;
    }

    /**
     * 生成带断卦结果的海报图片并上传 OSS，返回外部访问地址。
     */
    public String renderAndUpload(
        Hexagram hexagram,
        Prediction prediction,
        AiResult analysisResult,
        String sourceFileName,
        Path backgroundImagePath
    ) throws IOException, InterruptedException {
        Path tempOutput = Files.createTempFile("hexagram-analysis-poster-", ".png");
        try {
            renderToFile(hexagram, prediction, analysisResult, backgroundImagePath, tempOutput);
            try (InputStream inputStream = Files.newInputStream(tempOutput)) {
                return OSSUtil.uploadFile("mcp/renderedHexagramAnalysis", buildRenderedFileName(sourceFileName), inputStream);
            }
        } finally {
            Files.deleteIfExists(tempOutput);
        }
    }

    /**
     * 将结果海报 HTML 渲染为本地 PNG 文件，便于测试和样式调试。
     */
    public Path renderToFile(
        Hexagram hexagram,
        Prediction prediction,
        AiResult analysisResult,
        Path backgroundImagePath,
        Path outputFilePath
    ) throws IOException, InterruptedException {
        HexagramAnalysisPosterHtmlService.RenderedPoster renderedPoster =
            htmlService.render(hexagram, prediction, analysisResult, backgroundImagePath);
        Path htmlFile = Files.createTempFile("hexagram-analysis-poster-", ".html");
        try {
            Files.writeString(htmlFile, renderedPoster.getHtml(), StandardCharsets.UTF_8);
            return browserScreenshotService.captureHtml(
                htmlFile,
                outputFilePath,
                HexagramAnalysisPosterHtmlService.POSTER_WIDTH,
                renderedPoster.getHeight()
            );
        } finally {
            Files.deleteIfExists(htmlFile);
        }
    }

    private String buildRenderedFileName(String sourceFileName) {
        String baseName = sourceFileName;
        int dotIndex = sourceFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            baseName = sourceFileName.substring(0, dotIndex);
        }
        return baseName + "_analysis_" + FILE_TS.format(LocalDateTime.now()) + ".png";
    }
}
