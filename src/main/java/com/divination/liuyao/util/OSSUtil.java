package com.divination.liuyao.util;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.common.comm.SignVersion;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

public class OSSUtil {
    private static final String ENDPOINT = "https://oss-cn-beijing.aliyuncs.com";
    private static final String REGION = "cn-beijing";
    private static final String BUCKET_NAME = "ysyj-cloud";

    private static OSS getOSSClient() {
        try {
            EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
            return OSSClientBuilder.create()
                    .endpoint(ENDPOINT)
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(REGION)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 上传文件到OSS，支持自定义路径
     * @param path OSS目录路径（可为空）
     * @param fileName 文件名
     * @param inputStream 文件流
     * @return 文件访问URL
     */
    public static String uploadFile(String path, String fileName, InputStream inputStream) {
        OSS ossClient = getOSSClient();
        String objectName = (path == null || path.isEmpty()) ? fileName : path + "/" + fileName;
        try {
            ossClient.putObject(BUCKET_NAME, objectName, inputStream);
            return getFileUrl(path, fileName);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 获取OSS文件的访问URL，支持自定义路径
     * @param path OSS目录路径（可为空）
     * @param fileName 文件名
     * @return 文件访问URL
     */
    public static String getFileUrl(String path, String fileName) {
        OSS ossClient = getOSSClient();
        String objectName = (path == null || path.isEmpty()) ? fileName : path + "/" + fileName;
        Date expiration = new Date(new Date().getTime() + 60 * 1000L);
        // 生成以GET方法访问的预签名URL。本示例没有额外请求头，其他人可以直接通过浏览器访问相关内容。
        URL url = ossClient.generatePresignedUrl(BUCKET_NAME, objectName, expiration);
        return url.toString();
    }
}
