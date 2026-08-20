package com.divination.liuyao.util;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class OSSUtilTest {
    @Test
    public void testUploadAndGetFileUrl() {
        assumeTrue(Boolean.parseBoolean(System.getProperty("minio.integration.enabled", "false")),
                "需要本地 MinIO 时使用 -Dminio.integration.enabled=true 开启");

        String path = "test/junit";
        String fileName = "ossutil_test.txt";
        String content = "MinIO JUnit Test!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        String url = OSSUtil.uploadFile(path, fileName, inputStream);
        System.out.println("上传文件URL: " + url);
        assertNotNull(url);
        assertTrue(url.contains(fileName));

        String downloadUrl = OSSUtil.getFileUrl(path, fileName);
        System.out.println("下载文件URL: " + downloadUrl);
        assertNotNull(downloadUrl);
        assertTrue(downloadUrl.contains(fileName));
    }

    @Test
    public void testGetFileUrlWithDefaultLocalMinioConfig() {
        String downloadUrl = OSSUtil.getFileUrl("/test/junit/", "/ossutil_test.txt/");

        assertNotNull(downloadUrl);
        assertTrue(downloadUrl.startsWith("http://localhost:9000/ai-liuyao/test/junit/ossutil_test.txt"));
        assertEquals(-1, downloadUrl.indexOf("//test"));
    }
}

