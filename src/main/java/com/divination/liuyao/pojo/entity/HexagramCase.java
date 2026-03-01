package com.divination.liuyao.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 六爻卦例记录实体，对应 hexagram_case 表。
 * <p>
 * 每条记录对应一个从原始文件中检测到的完整六爻占例单元，
 * 包含在原文中的精确字符位置、AI 置信度及卦例原始 JSON。
 */
@Data
@TableName("hexagram_case")
public class HexagramCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属原始文件 ID，关联 source_file 表 */
    private Long sourceFileId;

    /** 该卦例在文件中的序号（从 1 开始，按原文顺序） */
    private Integer caseIndex;

    /** 卦例在原始全文中的起始字符位置（绝对偏移） */
    private Integer startOffset;

    /** 卦例在原始全文中的结束字符位置（绝对偏移） */
    private Integer endOffset;

    /** 完整卦例文本（含背景、卦象、断语，直接从全文截取） */
    private String caseText;

    /** AI 识别置信度，范围 0~1，精度 DECIMAL(4,3) */
    private BigDecimal aiConfidence;

    /**
     * 该占例单元中包含的六爻卦例数量。
     * 通常为 1；一段叙述包含多次起卦时大于 1。
     */
    private Integer hexagramNumber;

    /** AI 返回的原始 JSON 字符串（存入 MySQL JSON 类型列，便于后续结构化解析） */
    private String rawAiJson;

    /** 使用的 AI 模型名称，如 qwenPlus */
    private String aiModel;

    /** 结构版本号，预留用于后续 schema 升级 */
    private String structureVersion;

    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;

    /** 软删除标记（0=正常 1=已删除） */
    @TableLogic
    private Integer isDeleted;
}
