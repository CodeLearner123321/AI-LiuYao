package com.divination.liuyao.service.impl;

import com.divination.liuyao.hexagram.service.HexagramParseAsyncService;
import com.divination.liuyao.hexagram.util.FileTextExtractor;
import com.divination.liuyao.mapper.SourceFileMapper;
import com.divination.liuyao.pojo.entity.SourceFile;
import com.divination.liuyao.pojo.vo.SourceFileUploadVO;
import com.divination.liuyao.service.SourceFileService;
import com.divination.liuyao.util.OSSUtil;
import com.divination.liuyao.util.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 原始文件上传服务实现。
 * <p>
 * 同步流程：
 * <ol>
 *   <li>计算文件 MD5，检查是否已上传过（去重）</li>
 *   <li>将文件上传至阿里云 OSS</li>
 *   <li>将 MultipartFile 写入临时文件，调用 FileTextExtractor 提取纯文本</li>
 *   <li>将 SourceFile 记录写入数据库（parse_status=0）</li>
 *   <li>触发异步 AI 检测任务</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SourceFileServiceImpl implements SourceFileService {

    /** OSS 存储目录前缀 */
    private static final String OSS_PREFIX = "hexagram";

    /** 默认 AI 结构版本 */
    private static final String STRUCTURE_VERSION = "v1";

    private final SourceFileMapper         sourceFileMapper;
    private final HexagramParseAsyncService hexagramParseAsyncService;

    @Override
    public SourceFileUploadVO upload(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String fileType = resolveFileType(originalName);

        // ① MD5 去重
        String md5;
        try {
            md5 = DigestUtils.md5DigestAsHex(file.getInputStream());
        } catch (Exception e) {
            throw new RuntimeException("MD5 计算失败", e);
        }

        SourceFile existing = sourceFileMapper.findByMd5(md5);
        if (existing != null) {
            log.info("[SourceFileService] 文件已存在，MD5={}, sourceFileId={}", md5, existing.getId());
            return SourceFileUploadVO.builder()
                    .sourceFileId(existing.getId())
                    .fileName(existing.getFileName())
                    .fileType(existing.getFileType())
                    .textLength(existing.getTextLength())
                    .duplicate(true)
                    .message("该文件已上传过（MD5 匹配），返回已有记录，sourceFileId=" + existing.getId())
                    .build();
        }

        // ② 上传 OSS
        String ossPath = OSS_PREFIX + "/" + UserContextHolder.getUsername();
        String ossUrl;
        try {
            ossUrl = OSSUtil.uploadFile(ossPath, originalName, file.getInputStream());
            log.info("[SourceFileService] 文件已上传 OSS，path={}/{}", ossPath, originalName);
        } catch (Exception e) {
            throw new RuntimeException("OSS 上传失败: " + e.getMessage(), e);
        }

        // ③ 提取全文（MultipartFile → 临时文件 → FileTextExtractor）
        String fullText = extractTextFromMultipart(file, originalName);

        // ④ 入库 source_file（parse_status=0，等待异步检测）
        SourceFile sourceFile = new SourceFile();
        sourceFile.setFileName(originalName);
        sourceFile.setOssUrl(ossUrl);
        sourceFile.setFileMd5(md5);
        sourceFile.setFileSize(file.getSize());
        sourceFile.setFileType(fileType);
        sourceFile.setFullText(fullText);
        sourceFile.setTextLength(fullText.length());
        sourceFile.setTotalCases(0);
        sourceFile.setUploadedBy(UserContextHolder.getUserId());
        sourceFile.setUploaderRole(UserContextHolder.isRoot() ? "admin" : "user");
        sourceFile.setParseStatus(0);
        sourceFileMapper.insert(sourceFile);
        log.info("[SourceFileService] source_file 入库完成，id={}, textLength={}",
                sourceFile.getId(), fullText.length());

        // ⑤ 触发异步 AI 检测
        hexagramParseAsyncService.asyncDetectAndSave(sourceFile.getId(), fullText, STRUCTURE_VERSION);
        log.info("[SourceFileService] 已提交异步检测任务，sourceFileId={}", sourceFile.getId());

        return SourceFileUploadVO.builder()
                .sourceFileId(sourceFile.getId())
                .fileName(originalName)
                .fileType(fileType)
                .textLength(fullText.length())
                .duplicate(false)
                .message("文件上传成功，AI 检测任务已在后台启动，请稍后查询 parse_status")
                .build();
    }

    // ------------------------------------------------------------------ //
    //  私有辅助方法
    // ------------------------------------------------------------------ //

    /**
     * 将 MultipartFile 写入系统临时文件，用 FileTextExtractor 提取纯文本，提取完成后删除临时文件。
     */
    private String extractTextFromMultipart(MultipartFile file, String originalName) {
        Path tempPath = null;
        try {
            String suffix = originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf('.'))
                    : ".tmp";
            tempPath = Files.createTempFile("hexagram-upload-", suffix);
            file.transferTo(tempPath);

            String text = FileTextExtractor.extract(tempPath.toFile());
            log.info("[SourceFileService] 文本提取完成，字符数={}", text.length());
            return text;
        } catch (Exception e) {
            throw new RuntimeException("文本提取失败: " + e.getMessage(), e);
        } finally {
            if (tempPath != null) {
                try {
                    Files.deleteIfExists(tempPath);
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** 从文件名中提取扩展名作为文件类型（小写） */
    private String resolveFileType(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx >= 0 ? fileName.substring(dotIdx + 1).toLowerCase() : "unknown";
    }
}
