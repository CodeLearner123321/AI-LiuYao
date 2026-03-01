package com.divination.liuyao.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 原始文件记录实体，对应 source_file 表。
 * <p>
 * 每次用户上传文件时生成一条记录，存储 OSS 地址、解析后全文以及卦例识别状态。
 * parse_status 由异步服务更新：0=未解析 1=已解析 2=失败。
 */
@Data
@TableName("source_file")
public class SourceFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名（含扩展名） */
    private String fileName;

    /** OSS 存储地址（上传后写入，预签名 URL 有时效，持久化需存 object key 或重新生成） */
    private String ossUrl;

    /** 文件 MD5，用于去重（UNIQUE KEY） */
    private String fileMd5;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文件类型：pdf / docx / doc / txt */
    private String fileType;

    /** 解析后的完整文本内容 */
    private String fullText;

    /** 文本字符长度 */
    private Integer textLength;

    /** 识别出的卦例总数（异步检测完成后写入） */
    private Integer totalCases;

    /** 上传用户 ID */
    private Long uploadedBy;

    /** 上传者角色：admin / user */
    private String uploaderRole;

    /**
     * 解析状态：0=未解析 1=已解析 2=失败
     */
    private Integer parseStatus;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 软删除标记（0=正常 1=已删除） */
    @TableLogic
    private Integer isDeleted;
}
