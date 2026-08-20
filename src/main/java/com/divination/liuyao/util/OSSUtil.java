package com.divination.liuyao.util;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

public class OSSUtil {
    private static final String ENDPOINT = getConfig("MINIO_ENDPOINT", "OBJECT_STORAGE_ENDPOINT", "http://localhost:9000");
    private static final String ACCESS_KEY = getConfig("MINIO_ACCESS_KEY", "OBJECT_STORAGE_ACCESS_KEY", "minioadmin");
    private static final String SECRET_KEY = getConfig("MINIO_SECRET_KEY", "OBJECT_STORAGE_SECRET_KEY", "minioadmin");
    private static final String BUCKET_NAME = getConfig("MINIO_BUCKET", "OBJECT_STORAGE_BUCKET", "ai-liuyao");
    private static final String REGION = getConfig("MINIO_REGION", "OBJECT_STORAGE_REGION", "us-east-1");
    private static final int PRESIGNED_URL_EXPIRY_SECONDS = getIntConfig(
            "MINIO_PRESIGNED_URL_EXPIRY_SECONDS",
            "OBJECT_STORAGE_PRESIGNED_URL_EXPIRY_SECONDS",
            3600
    );

    private OSSUtil() {
    }

    private static MinioClient getOSSClient() {
        return MinioClient.builder()
                .endpoint(ENDPOINT)
                .credentials(ACCESS_KEY, SECRET_KEY)
                .region(REGION)
                .build();
    }

    /**
     * 上传文件到对象存储，支持自定义路径。
     *
     * @param path 对象目录路径（可为空）
     * @param fileName 文件名
     * @param inputStream 文件流
     * @return 文件访问URL
     */
    public static String uploadFile(String path, String fileName, InputStream inputStream) {
        String objectName = buildObjectName(path, fileName);
        try {
            MinioClient minioClient = getOSSClient();
            ensureBucketExists(minioClient);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .stream(inputStream, -1, 10 * 1024 * 1024)
                            .build()
            );
            return getFileUrl(path, fileName);
        } catch (Exception e) {
            throw new IllegalStateException("上传文件到对象存储失败: " + objectName, e);
        }
    }

    /**
     * 获取对象存储文件访问URL，支持自定义路径。
     *
     * @param path 对象目录路径（可为空）
     * @param fileName 文件名
     * @return 文件访问URL
     */
    public static String getFileUrl(String path, String fileName) {
        String objectName = buildObjectName(path, fileName);
        try {
            return getOSSClient().getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(BUCKET_NAME)
                            .object(objectName)
                            .expiry(PRESIGNED_URL_EXPIRY_SECONDS, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("生成对象存储预签名URL失败: " + objectName, e);
        }
    }

    private static void ensureBucketExists(MinioClient minioClient) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(BUCKET_NAME)
                        .build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(BUCKET_NAME)
                            .build()
            );
        }
    }

    private static String buildObjectName(String path, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        if (path == null || path.isBlank()) {
            return fileName;
        }
        return trimSlashes(path) + "/" + trimSlashes(fileName);
    }

    private static String trimSlashes(String value) {
        return value.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private static String getConfig(String primaryKey, String secondaryKey, String defaultValue) {
        String value = System.getenv(primaryKey);
        if (value == null || value.isBlank()) {
            value = System.getenv(secondaryKey);
        }
        if (value == null || value.isBlank()) {
            value = System.getProperty(toPropertyName(primaryKey));
        }
        if (value == null || value.isBlank()) {
            value = System.getProperty(toPropertyName(secondaryKey));
        }
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static int getIntConfig(String primaryKey, String secondaryKey, int defaultValue) {
        String value = getConfig(primaryKey, secondaryKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String toPropertyName(String envKey) {
        return envKey.toLowerCase().replace('_', '.');
    }
}
