package com.divination.liuyao.pojo.vo;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.NoArgsConstructor;

/**
 * AI六爻历史记录VO，用于向前端展示
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiLiuyaoHistoryVO extends BaGuaVo{
    
    /**
     * 记录ID
     */
    private Long id;

    /**
     * 历史记录ID，一个历史记录下可以会有多次对话
     */
    private Long historyId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 关联的任务ID
     */
    private Long taskId;

    /**
     * 用户提问的问题
     */
    private String question;

    /**
     * 问题背景
     */
    private String background;

    /**
     * 创建的类型
     */
    private String castType;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 数字
     */
    private String number;

    /**
     * 用户选定的时间
     */
    private LocalDateTime castTime;

    /**
     * 判词
     */
    private String keyOutcome;

    /**
     * 卦象结果数据（JSON格式）
     */
    private JsonNode resultData;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 消费金额
     */
    private String amount;

    /**
     * 解卦耗时
     */
    private Integer durationSeconds;


    /**
     * 是否准确 0：false 1 true
     */
    private Integer isAccurate;


    public static void mergeFromBaGua(AiLiuyaoHistoryVO vo, BaGuaVo baGuaVo) {
        vo.setOriginalBaGua(baGuaVo.getOriginalBaGua());
        vo.setChangedBaGua(baGuaVo.getChangedBaGua());
        vo.setExistChanged(baGuaVo.getExistChanged());
        vo.setShenSha(baGuaVo.getShenSha());
        vo.setLocalDateTime(baGuaVo.getLocalDateTime());
        vo.setBaZi(baGuaVo.getBaZi());
    }
}