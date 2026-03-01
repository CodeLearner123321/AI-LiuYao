package com.divination.liuyao.pojo.vo;

import lombok.Data;

import java.util.Date;

/**
 * 文件解析状态查询 VO，仅包含前端轮询所需字段。
 * <p>
 * 不包含 full_text、oss_url、file_md5 等敏感或大字段。
 */
@Data
public class SourceFileStatusVO {

    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer textLength;
    private Integer totalCases;
    /** 0=检测中 1=已完成 2=失败 */
    private Integer parseStatus;
    private Date createdAt;
    private Date updatedAt;
}
