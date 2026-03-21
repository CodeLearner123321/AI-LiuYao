package com.divination.liuyao.mcp.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PosterBackgroundService {

    private static final String DEFAULT_BACKGROUND_RESOURCE = "images/default-poster-background.png";

    /**
     * 复制默认海报背景图到临时文件，供浏览器渲染时直接读取本地路径。
     */
    public Path prepareDefaultBackgroundPath() throws IOException {
        ClassPathResource resource = new ClassPathResource(DEFAULT_BACKGROUND_RESOURCE);
        if (!resource.exists()) {
            throw new IllegalStateException("默认海报背景图片不存在: " + DEFAULT_BACKGROUND_RESOURCE);
        }

        Path tempBackgroundPath = Files.createTempFile("hexagram-poster-bg-", ".png");
        try (InputStream inputStream = resource.getInputStream()) {
            Files.copy(inputStream, tempBackgroundPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return tempBackgroundPath;
    }
}


