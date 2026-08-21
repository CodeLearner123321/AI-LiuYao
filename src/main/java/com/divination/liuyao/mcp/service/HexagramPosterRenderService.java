package com.divination.liuyao.mcp.service;

import com.divination.liuyao.pojo.entity.Prediction;
import com.divination.liuyao.pojo.model.Hexagram;
import com.divination.liuyao.util.OSSUtil;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
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
        Path workDir = Files.createTempDirectory("hexagram-poster-");
        Path htmlFile = workDir.resolve("poster.html");
        Path stagedBackground = workDir.resolve("background" + backgroundFileExtension(backgroundImagePath));
        try {
            Files.copy(backgroundImagePath, stagedBackground, StandardCopyOption.REPLACE_EXISTING);
            String html = htmlService.render(hexagram, prediction, stagedBackground.getFileName().toString());
            Files.writeString(htmlFile, html, StandardCharsets.UTF_8);
            return browserScreenshotService.captureHtml(
                    htmlFile,
                    outputFilePath,
                    HexagramPosterHtmlService.POSTER_WIDTH,
                    HexagramPosterHtmlService.POSTER_HEIGHT
            );
        } finally {
            deleteRecursively(workDir);
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

    private String backgroundFileExtension(Path backgroundImagePath) {
        String fileName = backgroundImagePath.getFileName() == null ? "" : backgroundImagePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex) : ".png";
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            stream.sorted((left, right) -> right.getNameCount() - left.getNameCount())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // Temp render workspace cleanup is best effort.
                    }
                });
        }
    }
}
