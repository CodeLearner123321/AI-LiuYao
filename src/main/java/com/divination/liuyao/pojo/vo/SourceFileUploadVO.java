package com.divination.liuyao.pojo.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文件上传接口的响应 VO。
 * <p>
 * 上传成功后立即返回；AI 检测在后台异步执行，
 * 前端可凭 sourceFileId 轮询 parse_status。
 */
@Data
@Builder
public class SourceFileUploadVO {

    /** source_file 表主键，供后续查询使用 */
    private Long sourceFileId;

    /** 原始文件名 */
    private String fileName;

    /** 文件类型（pdf / docx / doc / txt） */
    private String fileType;

    /** 文本字符长度 */
    private Integer textLength;

    /**
     * 是否为重复文件（MD5 命中已有记录）。
     * true 时 sourceFileId 为已有记录的 ID，不会再触发 AI 检测。
     */
    private Boolean duplicate;

    /** 提示信息（重复时说明已有记录；首次上传时说明检测任务已提交） */
    private String message;
}
