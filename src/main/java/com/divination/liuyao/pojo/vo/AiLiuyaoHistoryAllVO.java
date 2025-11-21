package com.divination.liuyao.pojo.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI六爻历史记录VO，用于向前端展示
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiLiuyaoHistoryAllVO {

    private List<AiLiuyaoHistoryVO>  aiLiuyaoHistoryVOS;

    private BigDecimal accuracyRate;
}