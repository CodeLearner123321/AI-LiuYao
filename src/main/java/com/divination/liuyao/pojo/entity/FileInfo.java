package com.divination.liuyao.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("file_info")
public class FileInfo {
    
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long uploaderUserId;    // 上传者userId

    private String fileName;        // 文件名称

    private Long fileSize;          // 文件大小（字节）

    private String fileFormat;      // 文件格式（如PDF、DOCX）

    private String imageName;       // 图片名称（如果是图片文件）

    private String author;          // 作者

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;        // 上传时间

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;        // 修改时间

    @TableLogic
    private Integer deleted;        // 是否删除

    @TableField(exist = false)
    private String type;            // 书籍类型

    public Boolean isBook() {
        return "book".equalsIgnoreCase(type);
    }
    public Boolean isImage() {
        return "image".equalsIgnoreCase(type);
    }
}
