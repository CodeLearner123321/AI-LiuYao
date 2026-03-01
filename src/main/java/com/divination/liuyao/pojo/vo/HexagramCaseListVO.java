package com.divination.liuyao.pojo.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 卦例列表查询 VO，仅包含前端展示所需字段。
 * <p>
 * 不包含 raw_ai_json、ai_model、structure_version 等非展示字段。
 */
@Data
public class HexagramCaseListVO {

    private Long id;
    private Integer caseIndex;
    private Integer startOffset;
    private Integer endOffset;
    private String caseText;
    private BigDecimal aiConfidence;
    private Integer hexagramNumber;
}
