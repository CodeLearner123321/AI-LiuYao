package com.divination.liuyao.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class OSSUtilTest {
    @Test
    public void testUploadAndGetFileUrl() {
        String path = "test/junit";
        String fileName = "ossutil_test.txt";
        String content = "OSS JUnit Test!";
        InputStream inputStream = new ByteArrayInputStream(content.getBytes());
        String url = OSSUtil.uploadFile(path, fileName, inputStream);
        System.out.println("上传文件URL: " + url);
        Assertions.assertNotNull(url);
        Assertions.assertTrue(url.contains(fileName));

        String downloadUrl = OSSUtil.getFileUrl(path, fileName);
        System.out.println("下载文件URL: " + downloadUrl);
        Assertions.assertNotNull(downloadUrl);
        Assertions.assertTrue(downloadUrl.contains(fileName));
    }
}

