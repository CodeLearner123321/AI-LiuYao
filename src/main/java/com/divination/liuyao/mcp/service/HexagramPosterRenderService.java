package com.divination.liuyao.mcp.service;

import com.divination.liuyao.pojo.entity.Prediction;
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
public class HexagramPosterRenderService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final HexagramPosterHtmlService htmlService;
    private final BrowserScreenshotService browserScreenshotService;

    public HexagramPosterRenderService(
        HexagramPosterHtmlService htmlService,
        BrowserScreenshotService browserScreenshotService
    ) {
        this.htmlService = htmlService;
        this.browserScreenshotService = browserScreenshotService;
    }

    public String renderAndUpload(
            Hexagram hexagram,
            Prediction prediction,
            String sourceFileName,
            Path backgroundImagePath
    ) throws IOException, InterruptedException {
        Path tempOutput = Files.createTempFile("hexagram-poster-", ".png");
        try {
            renderToFile(hexagram, prediction, backgroundImagePath, tempOutput);
            try (InputStream inputStream = Files.newInputStream(tempOutput)) {
                return OSSUtil.uploadFile("mcp/renderedHexagram", buildRenderedFileName(sourceFileName), inputStream);
            }
        } finally {
            Files.deleteIfExists(tempOutput);
        }
    }

    public Path renderToFile(
            Hexagram hexagram,
            Prediction prediction,
            Path backgroundImagePath,
            Path outputFilePath
    ) throws IOException, InterruptedException {
        String html = htmlService.render(hexagram, prediction, backgroundImagePath);
        Path htmlFile = Files.createTempFile("hexagram-poster-", ".html");
        try {
            Files.writeString(htmlFile, html, StandardCharsets.UTF_8);
            return browserScreenshotService.captureHtml(
                    htmlFile,
                    outputFilePath,
                    HexagramPosterHtmlService.POSTER_WIDTH,
                    HexagramPosterHtmlService.POSTER_HEIGHT
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
        return baseName + "_poster_" + FILE_TS.format(LocalDateTime.now()) + ".png";
    }
}
